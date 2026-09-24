package com.kaan.watchlist.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
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
