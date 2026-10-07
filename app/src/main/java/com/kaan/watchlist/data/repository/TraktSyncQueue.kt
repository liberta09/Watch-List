package com.kaan.watchlist.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kaan.watchlist.domain.model.MediaType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

enum class SyncActionType {
    WATCHED_MOVIE,
    WATCHED_EPISODES,
    RATING,
    WATCHLIST
}

data class SyncQueueItem(
    val id: String = UUID.randomUUID().toString(),
    val actionType: SyncActionType,
    val mediaType: MediaType? = null,
    val tmdbId: Int,
    val watchedAt: Long? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val rating: Int? = null,
    val ratedAt: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)

object TraktSyncQueue {
    private const val PREFS_NAME = "trakt_sync_queue"
    private const val KEY_PENDING_ITEMS = "pending_items"
    private val gson = Gson()
    private val mutex = Mutex()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    suspend fun enqueue(context: Context, item: SyncQueueItem) = mutex.withLock {
        if (item.tmdbId <= 0) return@withLock
        val prefs = getPrefs(context)
        val items = loadItems(prefs).toMutableList()

        // Merging & Deduplication logic
        val existingIndex = items.indexOfFirst { existing ->
            when (item.actionType) {
                SyncActionType.WATCHED_MOVIE -> {
                    existing.actionType == SyncActionType.WATCHED_MOVIE && existing.tmdbId == item.tmdbId
                }
                SyncActionType.WATCHED_EPISODES -> {
                    existing.actionType == SyncActionType.WATCHED_EPISODES &&
                            existing.tmdbId == item.tmdbId &&
                            existing.season == item.season &&
                            existing.episode == item.episode
                }
                SyncActionType.RATING -> {
                    existing.actionType == SyncActionType.RATING &&
                            existing.mediaType == item.mediaType &&
                            existing.tmdbId == item.tmdbId
                }
                SyncActionType.WATCHLIST -> {
                    existing.actionType == SyncActionType.WATCHLIST &&
                            existing.mediaType == item.mediaType &&
                            existing.tmdbId == item.tmdbId
                }
            }
        }

        if (existingIndex != -1) {
            // Replace existing item with the updated one
            items[existingIndex] = item
        } else {
            items.add(item)
        }

        saveItems(prefs, items)
    }

    suspend fun peekBatch(context: Context, limit: Int = 100): List<SyncQueueItem> = mutex.withLock {
        val prefs = getPrefs(context)
        return loadItems(prefs).take(limit)
    }

    suspend fun removeBatch(context: Context, ids: List<String>) = mutex.withLock {
        if (ids.isEmpty()) return@withLock
        val prefs = getPrefs(context)
        val items = loadItems(prefs).filterNot { ids.contains(it.id) }
        saveItems(prefs, items)
    }

    suspend fun pendingCount(context: Context): Int = mutex.withLock {
        val prefs = getPrefs(context)
        return loadItems(prefs).size
    }

    suspend fun clear(context: Context) = mutex.withLock {
        getPrefs(context).edit().remove(KEY_PENDING_ITEMS).apply()
    }

    private fun loadItems(prefs: SharedPreferences): List<SyncQueueItem> {
        val json = prefs.getString(KEY_PENDING_ITEMS, "[]") ?: "[]"
        val type = object : TypeToken<List<SyncQueueItem>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveItems(prefs: SharedPreferences, items: List<SyncQueueItem>) {
        val json = gson.toJson(items)
        prefs.edit().putString(KEY_PENDING_ITEMS, json).apply()
    }

    fun formatIso8601(epochMs: Long?): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(epochMs ?: System.currentTimeMillis()))
    }
}
