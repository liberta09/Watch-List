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
    val isWatched: Boolean = false,

    // Extended Details
    val originalTitle: String? = null,
    val voteAverage: Double? = null,
    val runtime: Int? = null,
    val episodeRuntime: Int? = null,
    val genres: List<String> = emptyList(),
    val productionCountries: List<String> = emptyList(),
    val director: String? = null,
    val cast: List<String> = emptyList(),
    val videoKey: String? = null,

    // User State & Tracking
    val addedAt: Long? = null,
    val watchedAt: Long? = null,
    val userRating: Int? = null,
    val isTracked: Boolean = false,
    val totalSeasons: Int? = null,
    val totalEpisodes: Int? = null,
    val watchedEpisodes: Map<String, Long> = emptyMap(), // Key: "S1_E1", Value: epoch ms
    val lastWatchedSeason: Int? = null,
    val lastWatchedEpisode: Int? = null,

    // Upcoming Info
    val showStatus: String? = null,
    val nextEpisodeAirDate: String? = null,
    val nextEpisodeSeason: Int? = null,
    val nextEpisodeNumber: Int? = null,
    val nextEpisodeName: String? = null,
    val lastAiredSeason: Int? = null,
    val lastAiredEpisode: Int? = null,
    val releaseDate: String? = null
) {
    @get:Exclude
    val posterUrl: String get() = if (posterPath != null) "https://image.tmdb.org/t/p/w500$posterPath" else ""
    @get:Exclude
    val backdropUrl: String get() = if (backdropPath != null) "https://image.tmdb.org/t/p/w1280$backdropPath" else ""
    
    @get:Exclude
    val displayTitle: String get() = title
    
    @get:Exclude
    val displayYear: String get() = year
}

enum class MediaType {
    MOVIE, TV
}
