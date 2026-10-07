package com.kaan.watchlist.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Register : Screen("register_screen")
    object Home : Screen("home_screen") // This will be the main host
    object TelegramWeb : Screen("telegram_web")
    object Detail : Screen("detail_screen/{mediaId}") {
        fun createRoute(mediaId: Int) = "detail_screen/$mediaId"
    }
    object Stats : Screen("stats_screen")
    object Trailer : Screen("trailer_screen/{videoId}") {
        fun createRoute(videoId: String) = "trailer_screen/$videoId"
    }
    object SharedList : Screen("shared_list_screen/{code}") {
        fun createRoute(code: String) = "shared_list_screen/$code"
    }
}

sealed class BottomNavScreen(val route: String, val titleResId: Int) {
    object Discover : BottomNavScreen("discover", com.kaan.watchlist.R.string.home_tab_discover)
    object Search : BottomNavScreen("search", com.kaan.watchlist.R.string.home_tab_search)
    object MyList : BottomNavScreen("my_list", com.kaan.watchlist.R.string.home_tab_my_list)
    object Favorites : BottomNavScreen("favorites", com.kaan.watchlist.R.string.home_tab_favorites)
    object Settings : BottomNavScreen("settings", com.kaan.watchlist.R.string.home_tab_settings)
}
