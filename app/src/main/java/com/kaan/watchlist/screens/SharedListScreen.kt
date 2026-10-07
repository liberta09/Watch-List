package com.kaan.watchlist.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.kaan.watchlist.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kaan.watchlist.domain.model.SharedList
import com.kaan.watchlist.navigation.Screen

import com.kaan.watchlist.ui.theme.BlueAccent
import com.kaan.watchlist.ui.theme.LightText
import com.kaan.watchlist.viewmodel.MediaViewModel
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@Composable
fun SharedListScreen(
    navController: NavController,
    viewModel: MediaViewModel,
    code: String
) {
    var sharedList by remember { mutableStateOf<SharedList?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(code) {
        viewModel.fetchSharedList(code) { list ->
            loading = false
            if (list != null) {
                sharedList = list
            } else {
                error = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.general_cancel),
                    tint = LightText
                )
            }
            Column(modifier = Modifier.padding(start = 8.dp)) {
                if (sharedList != null) {
                    Text(
                        text = sharedList!!.title.ifBlank { stringResource(R.string.settings_unnamed_list) },
                        color = LightText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.shared_list_owner, sharedList!!.ownerName),
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.settings_unnamed_list),
                        color = LightText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (sharedList != null && sharedList!!.items.isNotEmpty()) {
                Button(
                    onClick = {
                        val currentMyList = viewModel.myList.value
                        val newItems = sharedList!!.items.filter { sharedItem ->
                            currentMyList.none { it.id == sharedItem.tmdbId && it.type.name == sharedItem.type }
                        }
                        
                        if (newItems.isEmpty()) {
                            Toast.makeText(context, context.getString(R.string.toast_already_in_list), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        Toast.makeText(context, context.getString(R.string.toast_adding_items, newItems.size), Toast.LENGTH_SHORT).show()
                        
                        val dummyMediaItems = newItems.map { sharedItem ->
                            com.kaan.watchlist.domain.model.MediaItem(
                                id = sharedItem.tmdbId,
                                title = sharedItem.title,
                                posterPath = sharedItem.posterPath,
                                backdropPath = null,
                                year = sharedItem.releaseDate ?: "",
                                overview = "",
                                type = if (sharedItem.type == "MOVIE") com.kaan.watchlist.domain.model.MediaType.MOVIE else com.kaan.watchlist.domain.model.MediaType.TV,
                                isWatched = sharedItem.isWatched,
                                userRating = sharedItem.userRating,
                                releaseDate = sharedItem.releaseDate,
                                nextEpisodeAirDate = sharedItem.releaseDate
                            )
                        }
                        
                        // We need a way to add multiple items, but MediaViewModel.addToMyList takes one.
                        // Or we can add them to list. Wait, there's no addAll.
                        dummyMediaItems.forEach { item ->
                            viewModel.addToList(item, watched = item.isWatched)
                        }
                        
                        Toast.makeText(context, context.getString(R.string.toast_added), Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueAccent)
                ) {
                    Text(stringResource(R.string.shared_list_add_all), color = Color.White)
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = BlueAccent
                )
            } else if (error) {
                Text(
                    text = stringResource(R.string.shared_list_not_found),
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sharedList!!.items) { item ->
                        val domainMediaItem = com.kaan.watchlist.domain.model.MediaItem(
                            id = item.tmdbId,
                            title = item.title,
                            posterPath = item.posterPath,
                            backdropPath = null,
                            year = item.releaseDate ?: "",
                            overview = "",
                            type = if (item.type == "MOVIE") com.kaan.watchlist.domain.model.MediaType.MOVIE else com.kaan.watchlist.domain.model.MediaType.TV,
                            userRating = item.userRating,
                            isWatched = item.isWatched
                        )
                        com.kaan.watchlist.ui.components.MediaCard(
                            media = domainMediaItem,
                            onClick = {
                                navController.navigate(Screen.Detail.createRoute(item.tmdbId))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            fillWidth = true,
                            onToggleList = { }
                        )
                    }
                }
            }
        }
    }
}