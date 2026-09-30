package com.kaan.watchlist.domain.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.PropertyName
import com.google.gson.annotations.SerializedName

data class MediaItem(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val year: String,
    val overview: String,
    val type: MediaType,
    @get:PropertyName("isFavorite")
    @SerializedName(value = "isFavorite", alternate = ["favorite"])
    val isFavorite: Boolean = false,
    @get:PropertyName("isInList")
    @SerializedName(value = "isInList", alternate = ["inList"])
    val isInList: Boolean = false,
    @get:PropertyName("isWatched")
    @SerializedName(value = "isWatched", alternate = ["watched"])
    val isWatched: Boolean = false
) {
    @get:Exclude
    val posterUrl: String get() = if (posterPath != null) "https://image.tmdb.org/t/p/w500$posterPath" else ""
    @get:Exclude
    val backdropUrl: String get() = if (backdropPath != null) "https://image.tmdb.org/t/p/w1280$backdropPath" else ""
}

enum class MediaType {
    MOVIE, TV
}
