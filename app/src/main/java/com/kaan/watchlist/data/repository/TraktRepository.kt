package com.kaan.watchlist.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.kaan.watchlist.BuildConfig
import com.kaan.watchlist.data.api.DeviceCodeRequest
import com.kaan.watchlist.data.api.DeviceTokenRequest
import com.kaan.watchlist.data.api.RefreshTokenRequest
import com.kaan.watchlist.data.api.TraktApi
import com.kaan.watchlist.domain.model.MediaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.kaan.watchlist.data.api.DeviceCodeResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

data class ImportResult(
    val moviesAdded: Int,
    val showsAdded: Int,
    val episodesAdded: Int,
    val itemsUpdated: Int,
    val unmatched: Int
)

class TraktRepository(private val context: Context, private val mediaRepository: MediaRepository) {

    private val prefs: SharedPreferences = context.getSharedPreferences("trakt_prefs", Context.MODE_PRIVATE)
    private val clientId = BuildConfig.TRAKT_CLIENT_ID
    val isConfigured = clientId.isNotBlank()

    private val tokenMutex = Mutex()

    private val traktApi: TraktApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        val authInterceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            requestBuilder.addHeader("Content-Type", "application/json")
            requestBuilder.addHeader("trakt-api-version", "2")
            requestBuilder.addHeader("trakt-api-key", clientId)

            if (chain.request().url.encodedPath.contains("sync/")) {
                getAccessToken()?.let { token ->
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
            }

            chain.proceed(requestBuilder.build())
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.trakt.tv/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TraktApi::class.java)
    }

    fun isConnected(): Boolean {
        return getAccessToken() != null
    }

    private fun getAccessToken(): String? = prefs.getString("access_token", null)
    private fun getRefreshToken(): String? = prefs.getString("refresh_token", null)
    private fun getExpiresAt(): Long = prefs.getLong("expires_at", 0)

    private fun saveTokens(accessToken: String, refreshToken: String, expiresInSec: Long) {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .putLong("expires_at", System.currentTimeMillis() + (expiresInSec * 1000))
            .apply()
    }

    fun disconnect() {
        prefs.edit().clear().apply()
    }

    suspend fun startDeviceAuth(): DeviceCodeResponse? {
        if (!isConfigured) return null
        return try {
            val response = traktApi.getDeviceCode(DeviceCodeRequest(clientId))
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    // Returns true if authenticated, false if failed/expired
    suspend fun pollForToken(deviceCode: String, initialInterval: Long, expiresInSec: Long): Boolean {
        var intervalMs = initialInterval * 1000
        val endTime = System.currentTimeMillis() + (expiresInSec * 1000)

        while (System.currentTimeMillis() < endTime) {
            delay(intervalMs)
            try {
                val response = traktApi.getDeviceToken(DeviceTokenRequest(deviceCode, clientId))
                if (response.isSuccessful) {
                    response.body()?.let { tokenResp ->
                        saveTokens(tokenResp.access_token, tokenResp.refresh_token, tokenResp.expires_in)
                        return true
                    }
                } else {
                    when (response.code()) {
                        400 -> { /* Pending, continue polling */ }
                        404, 409, 410, 418 -> return false // Invalid/used/expired/denied
                        429 -> intervalMs += 1000 // Slow down
                    }
                }
            } catch (e: Exception) {
                // Network error, maybe try again
            }
        }
        return false
    }

    suspend fun refreshIfNeeded(): Boolean {
        if (!isConnected()) return false
        val expiresAt = getExpiresAt()
        val oneDayMs = 24 * 60 * 60 * 1000L
        if (System.currentTimeMillis() + oneDayMs >= expiresAt) {
            return performTokenRefresh()
        }
        return true
    }

    private suspend fun performTokenRefresh(): Boolean {
        tokenMutex.withLock {
            val expiresAt = getExpiresAt()
            val oneDayMs = 24 * 60 * 60 * 1000L
            if (System.currentTimeMillis() + oneDayMs < expiresAt) {
                return true
            }

            val refreshToken = getRefreshToken() ?: return false
            try {
                val response = traktApi.refreshToken(RefreshTokenRequest(refreshToken, clientId))
                if (response.isSuccessful) {
                    val tokenResp = response.body()
                    if (tokenResp != null) {
                        saveTokens(tokenResp.access_token, tokenResp.refresh_token, tokenResp.expires_in)
                        return true
                    }
                } else {
                    disconnect()
                    return false
                }
            } catch (e: Exception) {
                return false
            }
            return false
        }
    }

    suspend fun importAll(onProgress: (done: Int, total: Int) -> Unit): ImportResult = coroutineScope {
        if (!isConnected()) return@coroutineScope ImportResult(0, 0, 0, 0, 0)
        
        // 1. Paralel istekler
        var unmatchedCount = 0
        var totalEpisodesAdded = 0
        
        val deferredWatchedMovies = async { traktApi.getWatchedMovies().let { if (it.isSuccessful) it.body() ?: emptyList() else emptyList() } }
        val deferredWatchedShows = async { traktApi.getWatchedShows().let { if (it.isSuccessful) it.body() ?: emptyList() else emptyList() } }
        val deferredWatchlistMovies = async { traktApi.getWatchlistMovies().let { if (it.isSuccessful) it.body() ?: emptyList() else emptyList() } }
        val deferredWatchlistShows = async { traktApi.getWatchlistShows().let { if (it.isSuccessful) it.body() ?: emptyList() else emptyList() } }
        val deferredRatedMovies = async { traktApi.getRatedMovies().let { if (it.isSuccessful) it.body() ?: emptyList() else emptyList() } }
        val deferredRatedShows = async { traktApi.getRatedShows().let { if (it.isSuccessful) it.body() ?: emptyList() else emptyList() } }

        val watchedMovies = deferredWatchedMovies.await()
        val watchedShows = deferredWatchedShows.await()
        val watchlistMovies = deferredWatchlistMovies.await()
        val watchlistShows = deferredWatchlistShows.await()
        val ratedMovies = deferredRatedMovies.await()
        val ratedShows = deferredRatedShows.await()

        val itemMap = mutableMapOf<Pair<com.kaan.watchlist.domain.model.MediaType, Int>, MediaItemBuilder>()

        fun getOrPut(type: com.kaan.watchlist.domain.model.MediaType, tmdbId: Int, title: String, year: Int?): MediaItemBuilder {
            return itemMap.getOrPut(Pair(type, tmdbId)) {
                MediaItemBuilder(tmdbId, title, year, type)
            }
        }

        // Parse Time String to epoch
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        fun parseDate(dateStr: String?): Long? = try { dateStr?.let { sdf.parse(it)?.time } } catch (e: Exception) { null }

        watchedMovies.forEach { item: com.kaan.watchlist.data.api.TraktWatchedMovie ->
            val tmdbId = item.movie.ids.tmdb?.toInt()
            if (tmdbId == null) { unmatchedCount++; return@forEach }
            val builder = getOrPut(com.kaan.watchlist.domain.model.MediaType.MOVIE, tmdbId, item.movie.title, item.movie.year)
            builder.isWatched = true
            builder.watchedAt = parseDate(item.last_watched_at)
        }

        watchedShows.forEach { item: com.kaan.watchlist.data.api.TraktWatchedShow ->
            val tmdbId = item.show.ids.tmdb?.toInt()
            if (tmdbId == null) { unmatchedCount++; return@forEach }
            val builder = getOrPut(com.kaan.watchlist.domain.model.MediaType.TV, tmdbId, item.show.title, item.show.year)
            
            var latestEpisodeTime = 0L
            var lastS = 0
            var lastE = 0
            
            item.seasons?.forEach { season: com.kaan.watchlist.data.api.TraktWatchedSeason ->
                season.episodes?.forEach { episode: com.kaan.watchlist.data.api.TraktWatchedEpisode ->
                    val epTime = parseDate(episode.last_watched_at) ?: System.currentTimeMillis()
                    builder.watchedEpisodes["S${season.number}_E${episode.number}"] = epTime
                    totalEpisodesAdded++
                    if (epTime > latestEpisodeTime) {
                        latestEpisodeTime = epTime
                        lastS = season.number
                        lastE = episode.number
                    }
                }
            }
            if (latestEpisodeTime > 0) {
                builder.watchedAt = latestEpisodeTime
                builder.lastWatchedSeason = lastS
                builder.lastWatchedEpisode = lastE
            }
        }

        watchlistMovies.forEach { item: com.kaan.watchlist.data.api.TraktWatchlistMovie ->
            val tmdbId = item.movie.ids.tmdb?.toInt()
            if (tmdbId == null) { unmatchedCount++; return@forEach }
            val builder = getOrPut(com.kaan.watchlist.domain.model.MediaType.MOVIE, tmdbId, item.movie.title, item.movie.year)
            builder.addedAt = parseDate(item.listed_at)
        }

        watchlistShows.forEach { item: com.kaan.watchlist.data.api.TraktWatchlistShow ->
            val tmdbId = item.show.ids.tmdb?.toInt()
            if (tmdbId == null) { unmatchedCount++; return@forEach }
            val builder = getOrPut(com.kaan.watchlist.domain.model.MediaType.TV, tmdbId, item.show.title, item.show.year)
            builder.addedAt = parseDate(item.listed_at)
        }

        ratedMovies.forEach { item: com.kaan.watchlist.data.api.TraktRatedMovie ->
            val tmdbId = item.movie.ids.tmdb?.toInt()
            if (tmdbId == null) { unmatchedCount++; return@forEach }
            val builder = getOrPut(com.kaan.watchlist.domain.model.MediaType.MOVIE, tmdbId, item.movie.title, item.movie.year)
            builder.userRating = item.rating
        }

        ratedShows.forEach { item: com.kaan.watchlist.data.api.TraktRatedShow ->
            val tmdbId = item.show.ids.tmdb?.toInt()
            if (tmdbId == null) { unmatchedCount++; return@forEach }
            val builder = getOrPut(com.kaan.watchlist.domain.model.MediaType.TV, tmdbId, item.show.title, item.show.year)
            builder.userRating = item.rating
        }

        val allItems = itemMap.values.toList()
        val totalItems = allItems.size
        var doneItems = 0
        val finalMediaItems = mutableListOf<MediaItem>()

        // Process in chunks of 5
        allItems.chunked(5).forEach { chunk ->
            val deferredDetails = chunk.map { builder ->
                async {
                    val basicItem = MediaItem(
                        id = builder.tmdbId,
                        title = builder.title,
                        posterPath = null,
                        backdropPath = null,
                        year = builder.year?.toString() ?: "",
                        overview = "",
                        type = builder.type
                    )
                    val tmdbItem = mediaRepository.fetchMediaDetails(basicItem, persist = false)
                    
                    val now = System.currentTimeMillis()
                    val addedTime = builder.addedAt ?: builder.watchedAt ?: now
                    
                    tmdbItem.copy(
                        isWatched = builder.isWatched ?: false,
                        watchedAt = builder.watchedAt,
                        userRating = builder.userRating,
                        addedAt = addedTime,
                        watchedEpisodes = builder.watchedEpisodes,
                        lastWatchedSeason = builder.lastWatchedSeason,
                        lastWatchedEpisode = builder.lastWatchedEpisode,
                        isInList = true
                    )
                }
            }
            finalMediaItems.addAll(deferredDetails.awaitAll())
            doneItems += chunk.size
            onProgress(doneItems, totalItems)
        }

        val stats = mediaRepository.importItems(finalMediaItems)
        
        ImportResult(
            moviesAdded = finalMediaItems.count { it.type == com.kaan.watchlist.domain.model.MediaType.MOVIE },
            showsAdded = finalMediaItems.count { it.type == com.kaan.watchlist.domain.model.MediaType.TV }, 
            episodesAdded = totalEpisodesAdded,
            itemsUpdated = stats.itemsUpdated,
            unmatched = unmatchedCount
        )
    }

    private class MediaItemBuilder(
        val tmdbId: Int,
        val title: String,
        val year: Int?,
        val type: com.kaan.watchlist.domain.model.MediaType
    ) {
        var isWatched: Boolean? = null
        var watchedAt: Long? = null
        var addedAt: Long? = null
        var userRating: Int? = null
        val watchedEpisodes = mutableMapOf<String, Long>()
        var lastWatchedSeason: Int? = null
        var lastWatchedEpisode: Int? = null
    }
}
