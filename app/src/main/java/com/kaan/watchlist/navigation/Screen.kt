package com.kaan.watchlist.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Register : Screen("register_screen")
    object Home : Screen("home_screen") // This will be the main host
    object TelegramWeb : Screen("telegram_web")
    object Detail : Screen("detail_screen/{mediaId}") {
        fun createRoute(mediaId: Int) = "detail_screen/$mediaId"
    }
}

sealed class BottomNavScreen(val route: String, val title: String) {
    object Discover : BottomNavScreen("discover", "Ana Sayfa")
    object Search : BottomNavScreen("search", "Ara")
    object MyList : BottomNavScreen("my_list", "Listem")
    object Favorites : BottomNavScreen("favorites", "Favoriler")
    object Settings : BottomNavScreen("settings", "Ayarlar")
}
