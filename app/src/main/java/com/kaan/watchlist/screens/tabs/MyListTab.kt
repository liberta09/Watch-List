package com.kaan.watchlist.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kaan.watchlist.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaan.watchlist.domain.model.MediaItem
import com.kaan.watchlist.ui.components.MediaCard
import com.kaan.watchlist.ui.theme.LightText
import com.kaan.watchlist.viewmodel.MediaViewModel

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.kaan.watchlist.domain.model.MediaType
import com.kaan.watchlist.ui.components.tvFocusable
import com.kaan.watchlist.ui.theme.BlueAccent
import com.kaan.watchlist.ui.theme.DarkNavy
import com.kaan.watchlist.ui.theme.DarkSurface

@Composable
fun MyListTab(
    viewModel: MediaViewModel,
    onMediaClick: (MediaItem) -> Unit,
    onNavigateToStats: (() -> Unit)? = null
) {
    val myList by viewModel.myList.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    var filterType by rememberSaveable { mutableStateOf("ALL") } // ALL, MOVIE, TV
    var filterGenre by rememberSaveable { mutableStateOf("ALL") }
    var sortBy by rememberSaveable { mutableStateOf("ADDED_DESC") }
    var showSortMenu by remember { mutableStateOf(false) }

    val allGenres = remember(myList) {
        listOf("ALL") + myList.flatMap { it.genres }.distinct().sorted()
    }

    val processList = { list: List<MediaItem> ->
        list.filter { item ->
            val typeMatch = when (filterType) {
                "MOVIE" -> item.type == MediaType.MOVIE
                "TV" -> item.type == MediaType.TV
                else -> true
            }
            val genreMatch = filterGenre == "ALL" || item.genres.contains(filterGenre)
            typeMatch && genreMatch
        }.sortedWith(Comparator { a, b ->
            when (sortBy) {
                "VOTE_DESC" -> (b.voteAverage ?: 0.0).compareTo(a.voteAverage ?: 0.0)
                "RATING_DESC" -> (b.userRating ?: 0).compareTo(a.userRating ?: 0)
                "NAME_ASC" -> a.title.compareTo(b.title, ignoreCase = true)
                "WATCHED_DESC" -> (b.watchedAt ?: 0L).compareTo(a.watchedAt ?: 0L)
                else -> (b.addedAt ?: 0L).compareTo(a.addedAt ?: 0L)
            }
        })
    }

    val toWatch = processList(myList.filter { !it.isWatched })
    val watched = processList(myList.filter { it.isWatched })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Listem",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightText
                )
                Text(
                    text = "Toplam: ${myList.size}  |  İzlenen: ${myList.count { it.isWatched }}  |  İzlenecek: ${myList.count { !it.isWatched }}",
                    fontSize = 13.sp,
                    color = LightText.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (onNavigateToStats != null) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BlueAccent)
                        .tvFocusable(shape = RoundedCornerShape(8.dp), onClick = onNavigateToStats)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(stringResource(R.string.tab_stats), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Filter and Sort Bar
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Filter Chips
                val types = listOf("ALL" to stringResource(R.string.my_list_all), "MOVIE" to stringResource(R.string.my_list_movie), "TV" to stringResource(R.string.my_list_tv))
                types.forEach { (key, label) ->
                    val isSelected = filterType == key
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) BlueAccent else DarkNavy)
                            .tvFocusable(shape = RoundedCornerShape(12.dp), onClick = { filterType = key })
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = label, color = LightText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Sort Dropdown
                androidx.compose.foundation.layout.Box {
                    TextButton(onClick = { showSortMenu = true }) {
                        val sortLabel = when (sortBy) {
                            "VOTE_DESC" -> stringResource(R.string.sort_tmdb_score_short)
                            "RATING_DESC" -> stringResource(R.string.sort_my_score_short)
                            "NAME_ASC" -> "A-Z"
                            "WATCHED_DESC" -> stringResource(R.string.sort_date_watched_short)
                            else -> stringResource(R.string.sort_date_added_short)
                        }
                        Text(stringResource(R.string.sort_by, sortLabel), fontSize = 12.sp, color = BlueAccent)
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_date_added), color = LightText, fontSize = 13.sp) },
                            onClick = { sortBy = "ADDED_DESC"; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_tmdb_score), color = LightText, fontSize = 13.sp) },
                            onClick = { sortBy = "VOTE_DESC"; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_my_score), color = LightText, fontSize = 13.sp) },
                            onClick = { sortBy = "RATING_DESC"; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_name), color = LightText, fontSize = 13.sp) },
                            onClick = { sortBy = "NAME_ASC"; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_date_watched), color = LightText, fontSize = 13.sp) },
                            onClick = { sortBy = "WATCHED_DESC"; showSortMenu = false }
                        )
                    }
                }
            }

            // Genre Chips
            if (allGenres.size > 2) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    items(allGenres) { genre ->
                        val isSelected = filterGenre == genre
                        val label = if (genre == "ALL") stringResource(R.string.my_list_all_genres) else genre
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) BlueAccent else DarkSurface)
                                .tvFocusable(shape = RoundedCornerShape(12.dp), onClick = { filterGenre = genre })
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(text = label, color = LightText, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        if (myList.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = stringResource(R.string.empty_list), color = LightText.copy(alpha = 0.7f))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (toWatch.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(R.string.my_list_to_watch),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LightText,
                            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                        )
                    }
                    items(toWatch) { media ->
                        MediaCard(
                            media = media, 
                            onClick = { onMediaClick(media) },
                            modifier = Modifier.fillMaxWidth(),
                            fillWidth = true,
                            onToggleWatched = { viewModel.toggleWatchedStatus(it) },
                            onToggleList = { 
                                if (!it.isInList) viewModel.addToList(it, watched = false) 
                                else viewModel.toggleList(it) 
                            }
                        )
                    }
                }

                if (watched.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(R.string.my_list_watched),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LightText,
                            modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                        )
                    }
                    items(watched) { media ->
                        MediaCard(
                            media = media, 
                            onClick = { onMediaClick(media) },
                            modifier = Modifier.fillMaxWidth(),
                            fillWidth = true,
                            onToggleWatched = { viewModel.toggleWatchedStatus(it) },
                            onToggleList = { 
                                if (!it.isInList) viewModel.addToList(it, watched = false) 
                                else viewModel.toggleList(it) 
                            }
                        )
                    }
                }
            }
        }
    }
}
