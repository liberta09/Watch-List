package com.kaan.watchlist.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Url

interface AnnouncementApi {
    @GET
    suspend fun getAnnouncement(
        @Url url: String
    ): AnnouncementDto
}

data class AnnouncementDto(
    @SerializedName("enabled") val enabled: Boolean?,
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("buttonText") val buttonText: String?
)
