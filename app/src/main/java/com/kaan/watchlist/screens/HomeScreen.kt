package com.kaan.watchlist.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.kaan.watchlist.screens.tabs.DiscoverTab
import com.kaan.watchlist.screens.tabs.FavoritesTab
import com.kaan.watchlist.screens.tabs.MyListTab
import com.kaan.watchlist.screens.tabs.SearchTab
import com.kaan.watchlist.screens.tabs.SettingsTab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import com.kaan.watchlist.BuildConfig
import com.kaan.watchlist.util.UpdateHelper
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kaan.watchlist.ui.components.tvFocusable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kaan.watchlist.data.repository.UpdateStatus
import com.kaan.watchlist.navigation.BottomNavScreen
import com.kaan.watchlist.navigation.Screen
import com.kaan.watchlist.ui.theme.BlueAccent
import com.kaan.watchlist.ui.theme.DarkNavy
import com.kaan.watchlist.ui.theme.DarkSurface
import com.kaan.watchlist.ui.theme.LightText
import com.kaan.watchlist.viewmodel.MediaViewModel

@Composable
fun HomeScreen(rootNavController: NavController, viewModel: MediaViewModel) {
    val bottomNavController = rememberNavController()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val currentAnnouncement by viewModel.currentAnnouncement.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(true) }

    if (currentAnnouncement != null) {
        val announcement = currentAnnouncement!!

        AlertDialog(
            onDismissRequest = {
                viewModel.dismissAnnouncement(announcement.id)
            },
            title = {
                Text(
                    text = announcement.title,
                    color = LightText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    text = announcement.message,
                    color = LightText.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissAnnouncement(announcement.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.tvFocusable(shape = RoundedCornerShape(8.dp), onClick = {
                        viewModel.dismissAnnouncement(announcement.id)
                    })
                ) {
                    Text(text = announcement.buttonText, color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showUpdateDialog && updateStatus is UpdateStatus.UpdateAvailable) {
        val update = updateStatus as UpdateStatus.UpdateAvailable
        val context = LocalContext.current
        val currentVer = BuildConfig.VERSION_NAME

        val onDoUpdate = {
            showUpdateDialog = false
            UpdateHelper.downloadAndInstallApk(context, update.downloadUrl)
        }

        val onDismissUpdate = {
            showUpdateDialog = false
        }

        AlertDialog(
            onDismissRequest = onDismissUpdate,
            title = {
                Text(
                    text = "🔄 Güncelleme Mevcut",
                    color = LightText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    text = "Watch List için yeni bir güncelleme mevcut.\n\nMevcut sürüm: $currentVer\nYeni sürüm: ${update.version}",
                    color = LightText.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = onDoUpdate,
                    colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.tvFocusable(shape = RoundedCornerShape(8.dp), onClick = onDoUpdate)
                ) {
                    Text("Güncellemeyi Yap", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissUpdate,
                    modifier = Modifier.tvFocusable(shape = RoundedCornerShape(8.dp), onClick = onDismissUpdate)
                ) {
                    Text("Daha Sonra", color = Color.Gray)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    val items = listOf(
        BottomNavItem(BottomNavScreen.Discover, Icons.Default.Home),
        BottomNavItem(BottomNavScreen.Search, Icons.Default.Search),
        BottomNavItem(BottomNavScreen.MyList, Icons.AutoMirrored.Filled.List),
        BottomNavItem(BottomNavScreen.Favorites, Icons.Default.Favorite),
        BottomNavItem(BottomNavScreen.Settings, Icons.Default.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DarkNavy,
                contentColor = Color.White
            ) {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { item ->
                    val onTabClick = {
                        bottomNavController.navigate(item.screen.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }

                    NavigationBarItem(
                        modifier = Modifier.tvFocusable(
                            shape = RoundedCornerShape(16.dp),
                            scaleOnFocus = 1.08f,
                            onClick = onTabClick
                        ),
                        icon = { Icon(item.icon, contentDescription = item.screen.title) },
                        label = { Text(item.screen.title) },
                        selected = currentRoute == item.screen.route,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BlueAccent,
                            selectedTextColor = BlueAccent,
                            indicatorColor = DarkSurface,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        onClick = onTabClick
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavScreen.Discover.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavScreen.Discover.route) {
                DiscoverTab(viewModel = viewModel, onMediaClick = { rootNavController.navigate(Screen.Detail.createRoute(it.id)) })
            }
            composable(BottomNavScreen.Search.route) {
                SearchTab(viewModel = viewModel, onMediaClick = { rootNavController.navigate(Screen.Detail.createRoute(it.id)) })
            }
            composable(BottomNavScreen.MyList.route) {
                MyListTab(viewModel = viewModel, onMediaClick = { rootNavController.navigate(Screen.Detail.createRoute(it.id)) })
            }
            composable(BottomNavScreen.Favorites.route) {
                FavoritesTab(viewModel = viewModel, onMediaClick = { rootNavController.navigate(Screen.Detail.createRoute(it.id)) })
            }
            composable(BottomNavScreen.Settings.route) {
                SettingsTab(
                    viewModel = viewModel,
                    onLogout = {
                        rootNavController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onOpenTelegramWeb = {
                        rootNavController.navigate(Screen.TelegramWeb.route)
                    }
                )
            }
        }
    }
}

data class BottomNavItem(
    val screen: BottomNavScreen,
    val icon: ImageVector
)
