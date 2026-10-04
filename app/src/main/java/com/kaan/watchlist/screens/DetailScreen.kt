package com.kaan.watchlist.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.CheckCircle
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import com.kaan.watchlist.navigation.Screen
import com.kaan.watchlist.domain.model.MediaItem
import com.kaan.watchlist.domain.model.MediaType
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.kaan.watchlist.ui.components.MediaCard
import com.kaan.watchlist.ui.components.tvFocusable
import com.kaan.watchlist.ui.theme.BlueAccent
import com.kaan.watchlist.ui.theme.DarkNavy
import com.kaan.watchlist.ui.theme.DarkSurface
import com.kaan.watchlist.ui.theme.LightText
import com.kaan.watchlist.viewmodel.MediaViewModel
import java.util.Locale

import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.LinearProgressIndicator
import com.kaan.watchlist.data.api.WatchProviderCountryDto
import com.kaan.watchlist.data.api.WatchProviderItemDto
import com.kaan.watchlist.data.api.TvSeasonResponseDto
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(navController: NavController, viewModel: MediaViewModel, mediaId: Int) {
    val selectedMedia by viewModel.selectedMedia.collectAsState()
    val isLoading by viewModel.isLoadingDetails.collectAsState()
    val detailNotFound by viewModel.detailNotFound.collectAsState()
    val myList by viewModel.myList.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val watchProviders by viewModel.watchProviders.collectAsState()
    val seasonDetails by viewModel.currentSeasonDetails.collectAsState()
    val currentSeasonKey by viewModel.currentSeasonKey.collectAsState()

    LaunchedEffect(mediaId) {
        viewModel.loadMediaDetails(mediaId)
    }

    if (detailNotFound) {
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
        return
    }

    val rawMedia = selectedMedia
    if (rawMedia == null || rawMedia.id != mediaId) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = BlueAccent)
        }
        return
    }

    val media = viewModel.withUserState(rawMedia, myList, favorites)

    val currentNote = notes[media.id]
    var isEditingNote by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf(currentNote ?: "") }
    var showSuccessOverlay by remember { mutableStateOf(false) }
    var showAddToListDialog by remember { mutableStateOf(false) }
    
    val initialFocusRequester = remember { FocusRequester() }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(showSuccessOverlay) {
        if (showSuccessOverlay) {
            delay(1500)
            showSuccessOverlay = false
        }
    }

    LaunchedEffect(currentNote) {
        if (!isEditingNote) {
            noteText = currentNote ?: ""
        }
    }

    LaunchedEffect(Unit) {
        try {
            initialFocusRequester.requestFocus()
        } catch (e: Exception) {
            // Ignore if requestFocus fails
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .tvFocusable(shape = CircleShape, onClick = { navController.popBackStack() })
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = LightText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = LightText
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
                    .verticalScroll(rememberScrollState())
            ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 150.dp)
            ) {
                AsyncImage(
                    model = media.backdropUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                                startY = 20f
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 800.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    AsyncImage(
                        model = media.posterUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(90.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = media.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = media.year,
                                fontSize = 15.sp,
                                color = LightText.copy(alpha = 0.7f)
                            )
                            if (media.runtime != null && media.runtime > 0) {
                                Text(
                                    text = " • ${media.runtime} dk",
                                    fontSize = 15.sp,
                                    color = LightText.copy(alpha = 0.7f)
                                )
                            }
                            if (media.voteAverage != null && media.voteAverage > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format(Locale.US, "%.1f", media.voteAverage),
                                    fontSize = 15.sp,
                                    color = LightText.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        if (media.genres.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = media.genres.take(3).joinToString(", "),
                                fontSize = 13.sp,
                                color = LightText.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val onToggleList = {
                        val wasInList = media.isInList
                        if (!wasInList) {
                            showAddToListDialog = true
                        } else {
                            viewModel.toggleList(media)
                        }
                    }

                    Button(
                        onClick = onToggleList,
                        modifier = Modifier
                            .weight(1f)
                            .tvFocusable(
                                shape = RoundedCornerShape(8.dp),
                                focusRequester = initialFocusRequester,
                                onClick = onToggleList
                            ),
                        colors = ButtonDefaults.buttonColors(containerColor = if (media.isInList) DarkNavy else BlueAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (media.isInList) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (media.isInList) "✓ LİSTEYE EKLENDİ" else "LİSTEYE EKLE")
                    }

                    Button(
                        onClick = { viewModel.toggleFavorite(media) },
                        modifier = Modifier
                            .weight(1f)
                            .tvFocusable(shape = RoundedCornerShape(8.dp), onClick = { viewModel.toggleFavorite(media) }),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (media.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (media.isFavorite) Color.Red else LightText
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (media.isFavorite) "Favorilerde" else "Favoriye Ekle")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                
                if (media.videoKey != null) {
                    val onWatchTrailer = {
                        navController.navigate(Screen.Trailer.createRoute(media.videoKey))
                    }

                    Button(
                        onClick = onWatchTrailer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvFocusable(shape = RoundedCornerShape(8.dp), onClick = onWatchTrailer),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Fragmanı İzle", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // User Rating Section
                UserRatingSection(
                    currentRating = media.userRating,
                    onRatingSelected = { rating ->
                        viewModel.setUserRating(media, rating)
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Personal Note Section
                Text(
                    text = "Kişisel Notunuz",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightText
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (isEditingNote) {
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { if (it.length <= 500) noteText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .tvFocusable(shape = RoundedCornerShape(8.dp)),
                        placeholder = { Text("Notunuzu buraya yazın...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BlueAccent,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = LightText,
                            unfocusedTextColor = LightText
                        ),
                        supportingText = {
                            Text(
                                text = "${noteText.length}/500",
                                color = if (noteText.length == 500) Color.Red else Color.Gray,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            onClick = {
                                isEditingNote = false
                                noteText = currentNote ?: ""
                            },
                            modifier = Modifier.tvFocusable(shape = RoundedCornerShape(8.dp), onClick = {
                                isEditingNote = false
                                noteText = currentNote ?: ""
                            })
                        ) {
                            Text("İptal", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (noteText.isNotBlank()) {
                                    if (!media.isInList && !media.isFavorite) {
                                        viewModel.addToList(media, watched = false)
                                    }
                                    viewModel.saveNote(media.id, noteText)
                                    isEditingNote = false
                                }
                            },
                            enabled = noteText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                            modifier = Modifier.tvFocusable(shape = RoundedCornerShape(8.dp), onClick = {
                                if (noteText.isNotBlank()) {
                                    if (!media.isInList && !media.isFavorite) {
                                        viewModel.addToList(media, watched = false)
                                    }
                                    viewModel.saveNote(media.id, noteText)
                                    isEditingNote = false
                                }
                            })
                        ) {
                            Text("Kaydet")
                        }
                    }
                } else {
                    if (currentNote != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(text = currentNote, color = LightText, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = { viewModel.removeNote(media.id) },
                                modifier = Modifier.tvFocusable(shape = RoundedCornerShape(8.dp), onClick = { viewModel.removeNote(media.id) })
                            ) {
                                Text("Notu Sil", color = Color.Red.copy(alpha = 0.8f))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    noteText = currentNote
                                    isEditingNote = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                                modifier = Modifier.tvFocusable(shape = RoundedCornerShape(8.dp), onClick = {
                                    noteText = currentNote
                                    isEditingNote = true
                                })
                            ) {
                                Text("Notu Düzenle", color = LightText)
                            }
                        }
                    } else {
                        Button(
                            onClick = { isEditingNote = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvFocusable(shape = RoundedCornerShape(8.dp), onClick = { isEditingNote = true })
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Not Ekle", color = LightText)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // TV Show Episodes
                if (media.type == MediaType.TV) {
                    TvEpisodesSection(
                        media = media,
                        seasonDetails = seasonDetails,
                        currentSeasonKey = currentSeasonKey,
                        onSelectSeason = { season ->
                            viewModel.loadTvSeasonDetails(media.id, season)
                        },
                        onToggleEpisode = { s, ep, watched ->
                            viewModel.setEpisodeWatched(media, s, ep, watched)
                        },
                        onToggleSeason = { s, count, watched ->
                            viewModel.setSeasonWatched(media, s, count, watched)
                        }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Watch Providers
                WatchProvidersSection(providers = watchProviders)
                Spacer(modifier = Modifier.height(14.dp))

                // Description Section
                Text(
                    text = "Açıklama",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightText
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (media.overview.isNotBlank()) media.overview else "Bu içerik için bir açıklama bulunmuyor.",
                    fontSize = 14.sp,
                    color = LightText.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                )
                
                if (!media.director.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Yönetmen",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LightText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = media.director,
                        fontSize = 14.sp,
                        color = LightText.copy(alpha = 0.8f)
                    )
                }

                if (media.cast.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Oyuncular",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LightText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = media.cast.joinToString(", "),
                        fontSize = 14.sp,
                        color = LightText.copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )
                }

                // Recommendations (AŞAMA 7)
                val recommendations by viewModel.recommendations.collectAsState()
                if (recommendations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Benzer İçerikler",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LightText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recommendations) { rec ->
                            val userMedia = viewModel.withUserState(rec, myList, favorites)
                            MediaCard(
                                media = userMedia,
                                onClick = { navController.navigate(Screen.Detail.createRoute(userMedia.id)) },
                                onToggleList = {
                                    if (!it.isInList) {
                                        viewModel.addToList(it, watched = false)
                                    } else {
                                        viewModel.toggleList(it)
                                    }
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
            }
            
            AnimatedVisibility(
                visible = showSuccessOverlay,
                enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.8f, animationSpec = tween(300)),
                exit = fadeOut(tween(300)) + scaleOut(targetScale = 0.8f, animationSpec = tween(300))
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(min = 260.dp, max = 340.dp)
                        .background(DarkSurface, RoundedCornerShape(16.dp))
                        .border(2.dp, BlueAccent.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = BlueAccent,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "LİSTEYE EKLENDİ",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (media.type == MediaType.MOVIE) "Film listenize eklendi." else "Dizi listenize eklendi.",
                            fontSize = 16.sp,
                            color = LightText.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        if (showAddToListDialog) {
            val watchLaterFocusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                try { watchLaterFocusRequester.requestFocus() } catch (e: Exception) {}
            }
            AlertDialog(
                onDismissRequest = { showAddToListDialog = false },
                title = { Text("Listeye Ekle", color = LightText, fontWeight = FontWeight.Bold) },
                text = { Text("Bu içeriği nasıl eklemek istersin?", color = LightText.copy(alpha = 0.85f)) },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.addToList(media, watched = false)
                                showAddToListDialog = false
                                showSuccessOverlay = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                            modifier = Modifier
                                .weight(1f)
                                .tvFocusable(shape = RoundedCornerShape(8.dp), focusRequester = watchLaterFocusRequester, onClick = {
                                    viewModel.addToList(media, watched = false)
                                    showAddToListDialog = false
                                    showSuccessOverlay = true
                                })
                        ) {
                            Text("🔖 İzlenecek", color = Color.White)
                        }
                        Button(
                            onClick = {
                                viewModel.addToList(media, watched = true)
                                showAddToListDialog = false
                                showSuccessOverlay = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                            modifier = Modifier
                                .weight(1f)
                                .tvFocusable(shape = RoundedCornerShape(8.dp), onClick = {
                                    viewModel.addToList(media, watched = true)
                                    showAddToListDialog = false
                                    showSuccessOverlay = true
                                })
                        ) {
                            Text("✓ İzlendi", color = Color.White)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAddToListDialog = false },
                        modifier = Modifier.tvFocusable(shape = RoundedCornerShape(8.dp), onClick = { showAddToListDialog = false })
                    ) {
                        Text("İptal", color = Color.Gray)
                    }
                },
                containerColor = DarkSurface
            )
        }
    }
}
}

@Composable
fun UserRatingSection(
    currentRating: Int?,
    onRatingSelected: (Int?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Puanın",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = LightText
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..10) {
                val isSelected = currentRating != null && i <= currentRating
                IconButton(
                    onClick = {
                        if (currentRating == i) {
                            onRatingSelected(null)
                        } else {
                            onRatingSelected(i)
                        }
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .tvFocusable(shape = CircleShape, onClick = {
                            if (currentRating == i) {
                                onRatingSelected(null)
                            } else {
                                onRatingSelected(i)
                            }
                        })
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "$i Puan",
                        tint = if (isSelected) Color(0xFFFFD700) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        if (currentRating != null) {
            Text(
                text = "Puanın: $currentRating / 10",
                fontSize = 13.sp,
                color = BlueAccent,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun WatchProvidersSection(
    providers: WatchProviderCountryDto?
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Nerede İzlenir",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = LightText
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (providers == null || (providers.flatrate.isNullOrEmpty() && providers.rent.isNullOrEmpty() && providers.buy.isNullOrEmpty())) {
            Text(
                text = "Türkiye'de bir platformda bulunamadı",
                fontSize = 14.sp,
                color = LightText.copy(alpha = 0.6f)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!providers.flatrate.isNullOrEmpty()) {
                    ProviderCategoryRow("Abonelikle", providers.flatrate, providers.link, context)
                }
                if (!providers.rent.isNullOrEmpty()) {
                    ProviderCategoryRow("Kirala", providers.rent, providers.link, context)
                }
                if (!providers.buy.isNullOrEmpty()) {
                    ProviderCategoryRow("Satın Al", providers.buy, providers.link, context)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Kaynak: JustWatch",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ProviderCategoryRow(
    title: String,
    items: List<WatchProviderItemDto>,
    link: String?,
    context: android.content.Context
) {
    Column {
        Text(text = title, fontSize = 13.sp, color = LightText.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                if (!item.logoPath.isNullOrBlank()) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w92${item.logoPath}",
                        contentDescription = item.providerName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .tvFocusable(shape = RoundedCornerShape(8.dp), onClick = {
                                if (!link.isNullOrBlank()) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {}
                                }
                            })
                    )
                }
            }
        }
    }
}

@Composable
fun TvEpisodesSection(
    media: MediaItem,
    seasonDetails: TvSeasonResponseDto?,
    currentSeasonKey: String?,
    onSelectSeason: (Int) -> Unit,
    onToggleEpisode: (season: Int, episode: Int, watched: Boolean) -> Unit,
    onToggleSeason: (season: Int, episodeCount: Int, watched: Boolean) -> Unit
) {
    var selectedSeason by remember(media.id) { mutableStateOf(1) }
    val totalSeasons = media.totalSeasons ?: 1

    LaunchedEffect(media.id, selectedSeason) {
        onSelectSeason(selectedSeason)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Bölümler",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = LightText
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Season Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (s in 1..totalSeasons) {
                val isSelected = s == selectedSeason
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) BlueAccent else DarkSurface)
                        .tvFocusable(shape = RoundedCornerShape(16.dp), onClick = { selectedSeason = s })
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "S$s",
                        color = LightText,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val expectedKey = "${media.id}_$selectedSeason"
        val isSeasonLoaded = currentSeasonKey == expectedKey && seasonDetails != null

        val episodes = if (isSeasonLoaded) seasonDetails?.episodes ?: emptyList() else emptyList()
        val totalEps = media.totalEpisodes
        val watchedCount = media.watchedEpisodes.size

        // Progress bar
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (totalEps != null) {
                    Text(
                        text = "$watchedCount / $totalEps bölüm izlendi",
                        fontSize = 13.sp,
                        color = LightText.copy(alpha = 0.8f)
                    )
                } else {
                    Text(
                        text = "$watchedCount bölüm izlendi",
                        fontSize = 13.sp,
                        color = LightText.copy(alpha = 0.8f)
                    )
                }
                if (isSeasonLoaded && episodes.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            val allWatchedInSeason = episodes.all { ep ->
                                media.watchedEpisodes.containsKey("S${selectedSeason}_E${ep.episodeNumber}")
                            }
                            onToggleSeason(selectedSeason, episodes.size, !allWatchedInSeason)
                        }
                    ) {
                        Text(
                            text = if (episodes.all { media.watchedEpisodes.containsKey("S${selectedSeason}_E${it.episodeNumber}") }) "Sezonu geri al" else "Sezonu tamamla",
                            fontSize = 12.sp,
                            color = BlueAccent
                        )
                    }
                }
            }

            if (totalEps != null) {
                val progress = if (totalEps > 0) (watchedCount.toFloat() / totalEps).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = BlueAccent,
                    trackColor = DarkSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Next Episode
            val nextEpText = if (totalEps != null && watchedCount >= totalEps) {
                "Tüm bölümler izlendi ✓"
            } else if (isSeasonLoaded && episodes.isNotEmpty()) {
                val firstUnwatched = episodes.firstOrNull { !media.watchedEpisodes.containsKey("S${selectedSeason}_E${it.episodeNumber}") }
                if (firstUnwatched != null) {
                    "Sıradaki: S${selectedSeason} B${firstUnwatched.episodeNumber}"
                } else if (selectedSeason < totalSeasons) {
                    "Sıradaki: S${selectedSeason + 1} B1"
                } else {
                    "Tüm bölümler izlendi ✓"
                }
            } else {
                "Sıradaki: S${media.lastWatchedSeason ?: 1} B${(media.lastWatchedEpisode ?: 0) + 1}"
            }
            Text(text = nextEpText, fontSize = 12.sp, color = BlueAccent, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Episode List
        if (!isSeasonLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BlueAccent, modifier = Modifier.size(32.dp))
            }
        } else {
            episodes.forEach { ep ->
            val epNum = ep.episodeNumber ?: 1
            val epKey = "S${selectedSeason}_E$epNum"
            val isEpWatched = media.watchedEpisodes.containsKey(epKey)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface)
                    .tvFocusable(shape = RoundedCornerShape(8.dp), onClick = {
                        onToggleEpisode(selectedSeason, epNum, !isEpWatched)
                    })
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "B$epNum - ${ep.name ?: "Bölüm $epNum"}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LightText
                    )
                    if (!ep.airDate.isNullOrBlank()) {
                        Text(
                            text = ep.airDate,
                            fontSize = 11.sp,
                            color = LightText.copy(alpha = 0.6f)
                        )
                    }
                }

                IconButton(
                    onClick = { onToggleEpisode(selectedSeason, epNum, !isEpWatched) }
                ) {
                    Icon(
                        imageVector = if (isEpWatched) Icons.Default.CheckCircle else Icons.Default.Add,
                        contentDescription = "Watched",
                        tint = if (isEpWatched) BlueAccent else Color.Gray
                    )
                }
            }
        }
        }
    }
}
