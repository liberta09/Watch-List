package com.kaan.watchlist.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class DeviceCodeRequest(val client_id: String)

data class DeviceCodeResponse(
    val device_code: String,
    val user_code: String,
    val verification_url: String,
    val expires_in: Long,
    val interval: Long
)

data class DeviceTokenRequest(val code: String, val client_id: String)

data class RefreshTokenRequest(
    val refresh_token: String,
    val client_id: String,
    val grant_type: String = "refresh_token"
)

data class TokenResponse(
    val access_token: String,
    val refresh_token: String,
    val expires_in: Long,
    val created_at: Long
)

data class TraktIds(val trakt: Long, val tmdb: Long?, val imdb: String?)
data class TraktMovie(val title: String, val year: Int?, val ids: TraktIds)
data class TraktShow(val title: String, val year: Int?, val ids: TraktIds)

data class TraktWatchedMovie(val last_watched_at: String, val movie: TraktMovie)
data class TraktWatchedEpisode(val number: Int, val last_watched_at: String?)
data class TraktWatchedSeason(val number: Int, val episodes: List<TraktWatchedEpisode>?)
data class TraktWatchedShow(val last_watched_at: String, val show: TraktShow, val seasons: List<TraktWatchedSeason>?)

data class TraktWatchlistMovie(val listed_at: String, val movie: TraktMovie)
data class TraktWatchlistShow(val listed_at: String, val show: TraktShow)

data class TraktRatedMovie(val rated_at: String, val rating: Int, val movie: TraktMovie)
data class TraktRatedShow(val rated_at: String, val rating: Int, val show: TraktShow)

// --- SYNC POST REQUEST & RESPONSE DTOs ---

data class TraktSyncIds(val tmdb: Int)

data class SyncMovieHistory(
    val ids: TraktSyncIds,
    val watched_at: String
)

data class SyncEpisodeHistory(
    val number: Int,
    val watched_at: String
)

data class SyncSeasonHistory(
    val number: Int,
    val episodes: List<SyncEpisodeHistory>
)

data class SyncShowHistory(
    val ids: TraktSyncIds,
    val seasons: List<SyncSeasonHistory>? = null
)

data class SyncHistoryRequest(
    val movies: List<SyncMovieHistory>? = null,
    val shows: List<SyncShowHistory>? = null
)

data class SyncMovieRating(
    val ids: TraktSyncIds,
    val rating: Int,
    val rated_at: String
)

data class SyncShowRating(
    val ids: TraktSyncIds,
    val rating: Int,
    val rated_at: String
)

data class SyncRatingsRequest(
    val movies: List<SyncMovieRating>? = null,
    val shows: List<SyncShowRating>? = null
)

data class SyncMovieWatchlist(
    val ids: TraktSyncIds
)

data class SyncShowWatchlist(
    val ids: TraktSyncIds
)

data class SyncWatchlistRequest(
    val movies: List<SyncMovieWatchlist>? = null,
    val shows: List<SyncShowWatchlist>? = null
)

data class SyncItemCount(
    val movies: Int? = 0,
    val shows: Int? = 0,
    val episodes: Int? = 0
)

data class SyncResponse(
    val added: SyncItemCount?,
    val not_found: Any? = null
)

interface TraktApi {
    @POST("oauth/device/code")
    suspend fun getDeviceCode(@Body request: DeviceCodeRequest): Response<DeviceCodeResponse>

    @POST("oauth/device/token")
    suspend fun getDeviceToken(@Body request: DeviceTokenRequest): Response<TokenResponse>

    @POST("oauth/token")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<TokenResponse>

    @GET("sync/watched/movies")
    suspend fun getWatchedMovies(): Response<List<TraktWatchedMovie>>

    @GET("sync/watched/shows")
    suspend fun getWatchedShows(): Response<List<TraktWatchedShow>>

    @GET("sync/watchlist/movies")
    suspend fun getWatchlistMovies(): Response<List<TraktWatchlistMovie>>

    @GET("sync/watchlist/shows")
    suspend fun getWatchlistShows(): Response<List<TraktWatchlistShow>>

    @GET("sync/ratings/movies")
    suspend fun getRatedMovies(): Response<List<TraktRatedMovie>>

    @GET("sync/ratings/shows")
    suspend fun getRatedShows(): Response<List<TraktRatedShow>>

    @POST("sync/history")
    suspend fun addHistory(@Body request: SyncHistoryRequest): Response<SyncResponse>

    @POST("sync/ratings")
    suspend fun addRatings(@Body request: SyncRatingsRequest): Response<SyncResponse>

    @POST("sync/watchlist")
    suspend fun addWatchlist(@Body request: SyncWatchlistRequest): Response<SyncResponse>
}
