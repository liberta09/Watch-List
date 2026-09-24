package com.kaan.watchlist.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

interface UpdateApi {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GithubReleaseDto
}

data class GithubReleaseDto(
    @SerializedName("tag_name") val tagName: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("html_url") val htmlUrl: String?,
    @SerializedName("body") val body: String?,
    @SerializedName("assets") val assets: List<GithubAssetDto>?
)

data class GithubAssetDto(
    @SerializedName("name") val name: String?,
    @SerializedName("browser_download_url") val downloadUrl: String?
)
