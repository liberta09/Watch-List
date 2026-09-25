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
import coil.compose.AsyncImage
import com.kaan.watchlist.ui.components.tvFocusable
import com.kaan.watchlist.ui.theme.BlueAccent
import com.kaan.watchlist.ui.theme.DarkNavy
import com.kaan.watchlist.ui.theme.DarkSurface
import com.kaan.watchlist.ui.theme.LightText
import com.kaan.watchlist.viewmodel.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(navController: NavController, viewModel: MediaViewModel, mediaId: Int) {
    val movies by viewModel.popularMovies.collectAsState()
    val shows by viewModel.popularTvShows.collectAsState()
    val search by viewModel.searchResults.collectAsState()
    val list by viewModel.myList.collectAsState()
    val favs by viewModel.favorites.collectAsState()
    val notes by viewModel.notes.collectAsState()

    val media = movies.find { it.id == mediaId }
        ?: shows.find { it.id == mediaId }
        ?: search.find { it.id == mediaId }
        ?: list.find { it.id == mediaId }
        ?: favs.find { it.id == mediaId }

    if (media == null) {
        navController.popBackStack()
        return
    }

    val currentNote = notes[media.id]
    var isEditingNote by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf(currentNote ?: "") }
    
    val initialFocusRequester = remember { FocusRequester() }

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
                        modifier = Modifier.tvFocusable(shape = CircleShape, onClick = { navController.popBackStack() })
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(bottom = innerPadding.calculateBottomPadding())
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
                        Text(
                            text = media.year,
                            fontSize = 15.sp,
                            color = LightText.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { viewModel.toggleList(media) },
                        modifier = Modifier
                            .weight(1f)
                            .tvFocusable(
                                shape = RoundedCornerShape(8.dp),
                                focusRequester = initialFocusRequester,
                                onClick = { viewModel.toggleList(media) }
                            ),
                        colors = ButtonDefaults.buttonColors(containerColor = if (media.isInList) DarkNavy else BlueAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (media.isInList) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (media.isInList) "Listemde" else "Listeme Ekle")
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
                                        viewModel.toggleList(media)
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
                                        viewModel.toggleList(media)
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
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
