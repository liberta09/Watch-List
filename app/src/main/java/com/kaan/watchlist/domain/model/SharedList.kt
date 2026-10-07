package com.kaan.watchlist.domain.model

data class SharedList(
    val ownerUid: String = "",
    val ownerName: String = "",
    val title: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val items: List<SharedMediaItem> = emptyList()
)

data class SharedMediaItem(
    val tmdbId: Int = 0,
    val type: String = "", // "MOVIE" or "TV"
    val title: String = "",
    val posterPath: String? = null,
    val releaseDate: String? = null, // Can be nextEpisodeAirDate or releaseDate depending on what we want to display
    val userRating: Int? = null,
    val isWatched: Boolean = false
)

data class SharedListInfo(
    val code: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)

enum class ShareFilter {
    ALL, WATCHLIST, WATCHED, MOVIES, SHOWS
}