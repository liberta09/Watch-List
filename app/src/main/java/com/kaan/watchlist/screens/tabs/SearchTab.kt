package com.kaan.watchlist.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaan.watchlist.ui.components.tvFocusable
import com.kaan.watchlist.domain.model.MediaItem
import com.kaan.watchlist.ui.components.MediaCard
import com.kaan.watchlist.ui.theme.BlueAccent
import com.kaan.watchlist.ui.theme.LightText
import com.kaan.watchlist.viewmodel.MediaViewModel

@Composable
fun SearchTab(viewModel: MediaViewModel, onMediaClick: (MediaItem) -> Unit) {
    var query by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { 
                query = it 
                viewModel.search(it)
            },
            modifier = Modifier.fillMaxWidth().tvFocusable(shape = RoundedCornerShape(12.dp)),
            placeholder = { Text("Film veya dizi ara...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LightText) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { 
                        query = "" 
                        viewModel.clearSearch()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Temizle", tint = LightText)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboardController?.hide()
            }),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BlueAccent,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = LightText,
                unfocusedTextColor = LightText
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isSearching) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = BlueAccent)
            }
        } else if (searchError != null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = searchError ?: "", color = LightText, fontSize = 16.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(searchResults) { media ->
                    MediaCard(media = media, onClick = { onMediaClick(media) })
                }
            }
        }
    }
}
