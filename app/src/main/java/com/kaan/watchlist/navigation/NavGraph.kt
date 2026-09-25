package com.kaan.watchlist.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kaan.watchlist.data.repository.MediaRepository
import com.kaan.watchlist.screens.DetailScreen
import com.kaan.watchlist.screens.HomeScreen
import com.kaan.watchlist.screens.LoginScreen
import com.kaan.watchlist.screens.RegisterScreen
import com.kaan.watchlist.screens.TelegramWebScreen
import com.kaan.watchlist.viewmodel.MediaViewModel
import com.kaan.watchlist.viewmodel.MediaViewModelFactory

@Composable
fun SetupNavGraph(
    navController: NavHostController
) {
    val context = LocalContext.current
    val repository = remember { MediaRepository(context) }
    val factory = remember { MediaViewModelFactory(repository) }
    val sharedViewModel: MediaViewModel = viewModel(factory = factory)

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(route = Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        composable(route = Screen.Register.route) {
            RegisterScreen(navController = navController)
        }
        composable(route = Screen.Home.route) {
            HomeScreen(rootNavController = navController, viewModel = sharedViewModel)
        }
        composable(route = Screen.TelegramWeb.route) {
            TelegramWebScreen(navController = navController)
        }
        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("mediaId") { type = NavType.IntType })
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getInt("mediaId") ?: 0
            DetailScreen(navController = navController, viewModel = sharedViewModel, mediaId = mediaId)
        }
    }
}
