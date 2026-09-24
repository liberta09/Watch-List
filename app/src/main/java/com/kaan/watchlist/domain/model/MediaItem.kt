package com.kaan.watchlist.domain.model

data class MediaItem(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val year: String,
    val overview: String,
    val type: MediaType,
    val isFavorite: Boolean = false,
    val isInList: Boolean = false,
    val isWatched: Boolean = false
) {
    val posterUrl: String get() = if (posterPath != null) "https://image.tmdb.org/t/p/w500$posterPath" else ""
    val backdropUrl: String get() = if (backdropPath != null) "https://image.tmdb.org/t/p/w1280$backdropPath" else ""
}

enum class MediaType {
    MOVIE, TV
}
