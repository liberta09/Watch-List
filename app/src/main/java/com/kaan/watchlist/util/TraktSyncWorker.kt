package com.kaan.watchlist.util

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kaan.watchlist.data.api.SyncEpisodeHistory
import com.kaan.watchlist.data.api.SyncHistoryRequest
import com.kaan.watchlist.data.api.SyncMovieHistory
import com.kaan.watchlist.data.api.SyncMovieRating
import com.kaan.watchlist.data.api.SyncMovieWatchlist
import com.kaan.watchlist.data.api.SyncRatingsRequest
import com.kaan.watchlist.data.api.SyncSeasonHistory
import com.kaan.watchlist.data.api.SyncShowHistory
import com.kaan.watchlist.data.api.SyncShowRating
import com.kaan.watchlist.data.api.SyncShowWatchlist
import com.kaan.watchlist.data.api.SyncWatchlistRequest
import com.kaan.watchlist.data.api.TraktSyncIds
import com.kaan.watchlist.data.repository.MediaRepository
import com.kaan.watchlist.data.repository.SyncActionType
import com.kaan.watchlist.data.repository.TraktPushedStore
import com.kaan.watchlist.data.repository.TraktRepository
import com.kaan.watchlist.data.repository.TraktSyncQueue
import com.kaan.watchlist.domain.model.MediaType
import kotlinx.coroutines.delay
import retrofit2.Response
import java.util.concurrent.TimeUnit

class TraktSyncWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val mediaRepository = MediaRepository(context)
        val traktRepository = TraktRepository(context, mediaRepository)

        if (!traktRepository.isConnected()) {
            traktRepository.setLastSyncStatus("NOT_CONNECTED")
            return Result.success()
        }

        if (!traktRepository.isAutoPushEnabled()) {
            traktRepository.setLastSyncStatus("AUTO_PUSH_OFF")
            return Result.success()
        }

        while (true) {
            val batch = TraktSyncQueue.peekBatch(context, 100)
            if (batch.isEmpty()) break

            val successIds = mutableListOf<String>()
            val pushedKeysToMark = mutableListOf<String>()

            // 1. Process History items (Movies & Episodes)
            val historyItems = batch.filter {
                it.actionType == SyncActionType.WATCHED_MOVIE || it.actionType == SyncActionType.WATCHED_EPISODES
            }
            if (historyItems.isNotEmpty()) {
                val movieHistories = historyItems
                    .filter { it.actionType == SyncActionType.WATCHED_MOVIE }
                    .map {
                        SyncMovieHistory(
                            ids = TraktSyncIds(tmdb = it.tmdbId),
                            watched_at = TraktSyncQueue.formatIso8601(it.watchedAt)
                        )
                    }

                val showHistories = historyItems
                    .filter { it.actionType == SyncActionType.WATCHED_EPISODES }
                    .groupBy { it.tmdbId }
                    .map { (showTmdbId, epItems) ->
                        val seasons = epItems.groupBy { it.season ?: 1 }.map { (seasonNum, epList) ->
                            SyncSeasonHistory(
                                number = seasonNum,
                                episodes = epList.map {
                                    SyncEpisodeHistory(
                                        number = it.episode ?: 1,
                                        watched_at = TraktSyncQueue.formatIso8601(it.watchedAt)
                                    )
                                }
                            )
                        }
                        SyncShowHistory(
                            ids = TraktSyncIds(tmdb = showTmdbId),
                            seasons = seasons
                        )
                    }

                val request = SyncHistoryRequest(
                    movies = movieHistories.ifEmpty { null },
                    shows = showHistories.ifEmpty { null }
                )

                val result = executeSyncCall(traktRepository) {
                    traktRepository.traktApi.addHistory(request)
                }

                if (result is SyncCallResult.Success) {
                    successIds.addAll(historyItems.map { it.id })
                    historyItems.forEach {
                        if (it.actionType == SyncActionType.WATCHED_MOVIE) {
                            pushedKeysToMark.add(TraktPushedStore.movieKey(it.tmdbId))
                        } else if (it.actionType == SyncActionType.WATCHED_EPISODES && it.season != null && it.episode != null) {
                            pushedKeysToMark.add(TraktPushedStore.episodeKey(it.tmdbId, it.season, it.episode))
                        }
                    }
                } else {
                    cleanUpPartialSuccess(successIds, pushedKeysToMark)
                    return handleFailedResult(result, traktRepository)
                }
            }

            // 2. Process Rating items
            val ratingItems = batch.filter { it.actionType == SyncActionType.RATING }
            if (ratingItems.isNotEmpty()) {
                if (historyItems.isNotEmpty()) delay(1000)

                val movieRatings = ratingItems
                    .filter { it.mediaType == MediaType.MOVIE }
                    .map {
                        SyncMovieRating(
                            ids = TraktSyncIds(tmdb = it.tmdbId),
                            rating = it.rating ?: 10,
                            rated_at = TraktSyncQueue.formatIso8601(it.ratedAt)
                        )
                    }

                val showRatings = ratingItems
                    .filter { it.mediaType == MediaType.TV }
                    .map {
                        SyncShowRating(
                            ids = TraktSyncIds(tmdb = it.tmdbId),
                            rating = it.rating ?: 10,
                            rated_at = TraktSyncQueue.formatIso8601(it.ratedAt)
                        )
                    }

                val request = SyncRatingsRequest(
                    movies = movieRatings.ifEmpty { null },
                    shows = showRatings.ifEmpty { null }
                )

                val result = executeSyncCall(traktRepository) {
                    traktRepository.traktApi.addRatings(request)
                }

                if (result is SyncCallResult.Success) {
                    successIds.addAll(ratingItems.map { it.id })
                } else {
                    cleanUpPartialSuccess(successIds, pushedKeysToMark)
                    return handleFailedResult(result, traktRepository)
                }
            }

            // 3. Process Watchlist items
            val watchlistItems = batch.filter { it.actionType == SyncActionType.WATCHLIST }
            if (watchlistItems.isNotEmpty()) {
                if (historyItems.isNotEmpty() || ratingItems.isNotEmpty()) delay(1000)

                val movieWatchlist = watchlistItems
                    .filter { it.mediaType == MediaType.MOVIE }
                    .map { SyncMovieWatchlist(ids = TraktSyncIds(tmdb = it.tmdbId)) }

                val showWatchlist = watchlistItems
                    .filter { it.mediaType == MediaType.TV }
                    .map { SyncShowWatchlist(ids = TraktSyncIds(tmdb = it.tmdbId)) }

                val request = SyncWatchlistRequest(
                    movies = movieWatchlist.ifEmpty { null },
                    shows = showWatchlist.ifEmpty { null }
                )

                val result = executeSyncCall(traktRepository) {
                    traktRepository.traktApi.addWatchlist(request)
                }

                if (result is SyncCallResult.Success) {
                    successIds.addAll(watchlistItems.map { it.id })
                } else {
                    cleanUpPartialSuccess(successIds, pushedKeysToMark)
                    return handleFailedResult(result, traktRepository)
                }
            }

            // Clean up successful items in this batch iteration
            if (successIds.isNotEmpty()) {
                TraktSyncQueue.removeBatch(context, successIds)
                if (pushedKeysToMark.isNotEmpty()) {
                    TraktPushedStore.markPushed(context, pushedKeysToMark)
                }
                traktRepository.setLastSyncStatus("OK")
                traktRepository.setLastSyncSuccessAt(System.currentTimeMillis())
            } else {
                break
            }

            delay(1000)
        }

        traktRepository.setLastSyncStatus("OK")
        traktRepository.setLastSyncSuccessAt(System.currentTimeMillis())
        return Result.success()
    }

    private suspend fun cleanUpPartialSuccess(
        successIds: List<String>,
        pushedKeysToMark: List<String>
    ) {
        if (successIds.isNotEmpty()) {
            TraktSyncQueue.removeBatch(context, successIds)
            if (pushedKeysToMark.isNotEmpty()) {
                TraktPushedStore.markPushed(context, pushedKeysToMark)
            }
        }
    }

    private sealed class SyncCallResult {
        object Success : SyncCallResult()
        data class Error(val code: Int, val message: String?) : SyncCallResult()
    }

    private suspend fun <T> executeSyncCall(
        traktRepository: TraktRepository,
        call: suspend () -> Response<T>
    ): SyncCallResult {
        return try {
            var response = call()
            if (response.code() == 401) {
                val refreshed = traktRepository.refreshIfNeeded(force = true)
                if (refreshed) {
                    response = call()
                }
            }
            if (response.isSuccessful) {
                SyncCallResult.Success
            } else {
                SyncCallResult.Error(response.code(), response.message())
            }
        } catch (e: Exception) {
            SyncCallResult.Error(-1, "NETWORK_${e.javaClass.simpleName}")
        }
    }

    private fun handleFailedResult(
        result: SyncCallResult,
        traktRepository: TraktRepository
    ): Result {
        if (result is SyncCallResult.Error) {
            when {
                result.code == 401 -> traktRepository.setLastSyncStatus("AUTH_EXPIRED")
                result.code == 403 -> traktRepository.setLastSyncStatus("NO_PERMISSION")
                result.code == 420 || result.code == 429 -> traktRepository.setLastSyncStatus("RATE_LIMIT")
                result.code > 0 -> traktRepository.setLastSyncStatus("HTTP_${result.code}")
                else -> traktRepository.setLastSyncStatus(result.message ?: "NETWORK_Exception")
            }
        }
        return Result.retry()
    }

    companion object {
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<TraktSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "trakt_sync",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
