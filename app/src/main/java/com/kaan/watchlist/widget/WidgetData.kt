package com.kaan.watchlist.widget

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kaan.watchlist.domain.model.MediaItem
import com.kaan.watchlist.domain.model.MediaType
import com.kaan.watchlist.util.DateUtils

data class WidgetState(
    val thisWeek: List<WidgetRow>,
    val continueWatching: List<WidgetRow>
)

data class WidgetRow(
    val id: Int,
    val title: String,
    val subtitle: String
)

object WidgetData {
    fun loadWidgetData(context: Context): WidgetState {
        val prefs = context.getSharedPreferences("watchlist_prefs", Context.MODE_PRIVATE)
        val myListJson = prefs.getString("my_list", "[]") ?: "[]"
        val favJson = prefs.getString("favorites", "[]") ?: "[]"
        val gson = Gson()
        val type = object : TypeToken<List<MediaItem>>() {}.type

        val myList = try { gson.fromJson<List<MediaItem>>(myListJson, type) } catch (e: Exception) { emptyList() }
        val favList = try { gson.fromJson<List<MediaItem>>(favJson, type) } catch (e: Exception) { emptyList() }

        val allItems = (myList + favList).distinctBy { it.id }.map {
            // Ensure lists/maps are not null from Gson parsing
            it.copy(
                genres = it.genres ?: emptyList(),
                productionCountries = it.productionCountries ?: emptyList(),
                cast = it.cast ?: emptyList(),
                watchedEpisodes = it.watchedEpisodes ?: emptyMap()
            )
        }

        // This Week
        val thisWeekItems = allItems.filter {
            val dateStr = if (it.type == MediaType.TV) it.nextEpisodeAirDate else it.releaseDate
            val days = DateUtils.getDaysUntil(dateStr)
            days != null && days in 0..7
        }.sortedBy {
            DateUtils.getDaysUntil(if (it.type == MediaType.TV) it.nextEpisodeAirDate else it.releaseDate) ?: 999
        }.take(4)

        val thisWeekRows = thisWeekItems.map { media ->
            val dateStr = if (media.type == MediaType.TV) media.nextEpisodeAirDate else media.releaseDate
            val days = DateUtils.getDaysUntil(dateStr)
            val timeStr = when (days) {
                0 -> "Bugün"
                1 -> "Yarın"
                else -> "$days gün sonra"
            }
            
            val subtitle = if (media.type == MediaType.TV) {
                val s = media.nextEpisodeSeason ?: 1
                val e = media.nextEpisodeNumber ?: 1
                val prefix = if (e == 1) "Yeni sezon · " else ""
                "${prefix}S$s B$e · $timeStr"
            } else {
                "Vizyon · $timeStr"
            }
            WidgetRow(media.id, media.title, subtitle)
        }

        // Continue Watching
        val cwItems = allItems.filter { it.type == MediaType.TV && it.watchedEpisodes.isNotEmpty() }
            .filter {
                val total = it.totalEpisodes
                total == null || it.watchedEpisodes.size < total
            }
            .sortedByDescending { it.watchedEpisodes.values.maxOrNull() ?: 0L }
            .take(3)

        val continueWatchingRows = cwItems.map { media ->
            val lastS = media.lastWatchedSeason ?: 1
            val lastE = media.lastWatchedEpisode ?: 1
            WidgetRow(media.id, media.title, "Son: S$lastS B$lastE")
        }

        return WidgetState(thisWeekRows, continueWatchingRows)
    }
}
