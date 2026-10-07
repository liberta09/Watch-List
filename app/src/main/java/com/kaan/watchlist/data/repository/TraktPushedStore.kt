package com.kaan.watchlist.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object TraktPushedStore {
    private const val PREFS_NAME = "trakt_pushed"
    private const val KEY_PUSHED_SET = "pushed_keys"
    private val mutex = Mutex()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun movieKey(tmdbId: Int): String = "M_$tmdbId"

    fun episodeKey(showTmdbId: Int, season: Int, episode: Int): String = "E_${showTmdbId}_${season}_$episode"

    suspend fun isPushed(context: Context, key: String): Boolean = mutex.withLock {
        val prefs = getPrefs(context)
        val set = prefs.getStringSet(KEY_PUSHED_SET, emptySet()) ?: emptySet()
        return set.contains(key)
    }

    suspend fun markPushed(context: Context, keys: Collection<String>) = mutex.withLock {
        if (keys.isEmpty()) return@withLock
        val prefs = getPrefs(context)
        val set = (prefs.getStringSet(KEY_PUSHED_SET, emptySet()) ?: emptySet()).toMutableSet()
        set.addAll(keys)
        prefs.edit().putStringSet(KEY_PUSHED_SET, set).apply()
    }

    suspend fun clear(context: Context) = mutex.withLock {
        getPrefs(context).edit().clear().apply()
    }
}
