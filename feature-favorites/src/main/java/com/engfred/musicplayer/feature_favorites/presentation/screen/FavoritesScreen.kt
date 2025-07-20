package com.engfred.musicplayer.feature_favorites.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.feature_favorites.presentation.viewmodel.FavoritesEvent
import com.engfred.musicplayer.feature_favorites.presentation.viewmodel.FavoritesViewModel

/**
 * Composable screen for displaying the list of favorite audio files.
 */
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState

    Scaffold{ paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                uiState.favoriteAudioFiles.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No favorite songs yet. Add some!",
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            count = uiState.favoriteAudioFiles.size,
                            key = { index -> uiState.favoriteAudioFiles[index].id }
                        ) {
                            val favoriteAudioFile = uiState.favoriteAudioFiles[it]
                            FavoriteAudioFileItem(
                                favoriteAudioFile = favoriteAudioFile,
                                onClick = { clickedAudioFile ->
                                    viewModel.onEvent(FavoritesEvent.OnAudioFileClick(clickedAudioFile))
                                },
                                onRemoveClick = { audioFileId ->
                                    viewModel.onEvent(FavoritesEvent.RemoveFavorite(audioFileId))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteAudioFileItem(
    favoriteAudioFile: AudioFile,
    onClick: (AudioFile) -> Unit,
    onRemoveClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onClick(favoriteAudioFile)
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art or Placeholder Icon
            if (favoriteAudioFile.albumArtUri != null) {
                AsyncImage(
                    model = favoriteAudioFile.albumArtUri,
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "No Album Art",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = favoriteAudioFile.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = favoriteAudioFile.artist ?: "Unknown Artist",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Remove from Favorites Button
            IconButton(onClick = { onRemoveClick(favoriteAudioFile.id) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove from Favorites",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}