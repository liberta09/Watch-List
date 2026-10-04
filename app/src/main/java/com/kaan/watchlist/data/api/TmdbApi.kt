package com.kaan.watchlist.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {
    @GET("3/movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "tr-TR",
        @Query("page") page: Int = 1
    ): TmdbResponse

    @GET("3/tv/popular")
    suspend fun getPopularTvShows(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "tr-TR",
        @Query("page") page: Int = 1
    ): TmdbResponse

    @GET("3/search/multi")
    suspend fun searchMulti(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "tr-TR",
        @Query("page") page: Int = 1
    ): TmdbResponse

    @GET("3/movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "tr-TR",
        @Query("append_to_response") append: String = "credits,videos"
    ): MediaDetailsDto

    @GET("3/tv/{tv_id}")
    suspend fun getTvDetails(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "tr-TR",
        @Query("append_to_response") append: String = "credits,videos"
    ): MediaDetailsDto

    @GET("3/movie/{movie_id}/videos")
    suspend fun getMovieVideos(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String? = null
    ): VideoResponseDto

    @GET("3/tv/{tv_id}/videos")
    suspend fun getTvVideos(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String? = null
    ): VideoResponseDto
}

data class TmdbResponse(
    @SerializedName("results") val results: List<MediaDto>
)

data class MediaDto(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("media_type") val mediaType: String?
)

data class MediaDetailsDto(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("original_title") val originalTitle: String?,
    @SerializedName("original_name") val originalName: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("runtime") val runtime: Int?,
    @SerializedName("episode_run_time") val episodeRunTime: List<Int>?,
    @SerializedName("genres") val genres: List<GenreDto>?,
    @SerializedName("production_countries") val productionCountries: List<CountryDto>?,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int?,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int?,
    @SerializedName("credits") val credits: CreditsDto?,
    @SerializedName("videos") val videos: VideoResponseDto?
)

data class GenreDto(@SerializedName("name") val name: String?)
data class CountryDto(@SerializedName("name") val name: String?)
data class CreditsDto(
    @SerializedName("cast") val cast: List<CastDto>?,
    @SerializedName("crew") val crew: List<CrewDto>?
)
data class CastDto(@SerializedName("name") val name: String?)
data class CrewDto(@SerializedName("name") val name: String?, @SerializedName("job") val job: String?)

data class VideoResponseDto(
    @SerializedName("results") val results: List<VideoDto>?
)

data class VideoDto(
    @SerializedName("type") val type: String?,
    @SerializedName("site") val site: String?,
    @SerializedName("key") val key: String?,
    @SerializedName("official") val official: Boolean?
)
