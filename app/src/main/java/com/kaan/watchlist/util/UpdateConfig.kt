package com.kaan.watchlist.util

/**
 * Centralized configuration for App Update System via GitHub Releases.
 * 
 * Replace placeholders with your actual GitHub Owner and Repository name.
 * Example:
 * GITHUB_OWNER = "kaan"
 * GITHUB_REPO = "WatchList"
 */
object UpdateConfig {
    const val GITHUB_OWNER = "liberta09"
    const val GITHUB_REPO = "Watch-List"

    val isConfigured: Boolean
        get() = GITHUB_OWNER != "REPLACE_WITH_OWNER" && 
                GITHUB_REPO != "REPLACE_WITH_REPOSITORY" && 
                GITHUB_OWNER.isNotBlank() && 
                GITHUB_REPO.isNotBlank()
}
