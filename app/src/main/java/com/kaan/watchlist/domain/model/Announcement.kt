package com.kaan.watchlist.domain.model

data class Announcement(
    val id: String,
    val title: String,
    val message: String,
    val buttonText: String,
    val titleEn: String? = null,
    val messageEn: String? = null,
    val buttonTextEn: String? = null
) {
    fun getLocalizedTitle(isEnglish: Boolean): String = if (isEnglish) titleEn ?: title else title
    fun getLocalizedMessage(isEnglish: Boolean): String = if (isEnglish) messageEn ?: message else message
    fun getLocalizedButtonText(isEnglish: Boolean): String = if (isEnglish) buttonTextEn ?: buttonText else buttonText
}
