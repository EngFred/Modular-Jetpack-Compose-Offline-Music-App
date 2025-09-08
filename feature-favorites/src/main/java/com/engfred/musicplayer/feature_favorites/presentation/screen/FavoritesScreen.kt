package com.engfred.musicplayer.feature_favorites.presentation.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.engfred.musicplayer.core.ui.AudioFileItem
import com.engfred.musicplayer.core.ui.AddSongToPlaylistDialog
import com.engfred.musicplayer.core.ui.ConfirmationDialog
import com.engfred.musicplayer.feature_favorites.presentation.viewmodel.FavoritesEvent
import com.engfred.musicplayer.feature_favorites.presentation.viewmodel.FavoritesViewModel

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Replace Scaffold with a Box to act as the root container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            )

    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                uiState.isLoading || uiState.isCleaningMissingFavorites -> {
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
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(
                            count = uiState.favoriteAudioFiles.size,
                            key = { uiState.favoriteAudioFiles[it].id }
                        ) { index ->
                            val favoriteAudioFile = uiState.favoriteAudioFiles[index]

                            Column {
                                AudioFileItem(
                                    audioFile = favoriteAudioFile,
                                    onClick = { clickedAudioFile ->
                                        viewModel.onEvent(FavoritesEvent.PlayAudio(clickedAudioFile))
                                    },
                                    isAudioPlaying = uiState.isPlaying,
                                    isCurrentPlayingAudio = uiState.currentPlayingId == favoriteAudioFile.id,
                                    onAddToPlaylist = { audioFile ->
                                        viewModel.onEvent(FavoritesEvent.ShowPlaylistsDialog(audioFile))
                                    },
                                    onPlayNext = {
                                        viewModel.onEvent(FavoritesEvent.PlayNext(it))
                                    },
                                    onRemoveOrDelete = {
                                        viewModel.onEvent(FavoritesEvent.ShowRemoveFavoriteConfirmation(it))
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (index < uiState.favoriteAudioFiles.size - 1) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showAddToPlaylistDialog) {
        AddSongToPlaylistDialog(
            onDismiss = { viewModel.onEvent(FavoritesEvent.DismissAddToPlaylistDialog) },
            playlists = uiState.playlists,
            onAddSongToPlaylist = { playlist ->
                viewModel.onEvent(FavoritesEvent.AddedSongToPlaylist(playlist))
            }
        )
    }

    if (uiState.showRemoveFavoriteConfirmationDialog) {
        uiState.audioFileToRemove?.let { audioFile ->
            ConfirmationDialog(
                title = "Remove from Favorites?",
                message = "Are you sure you want to remove '${audioFile.title}' from your favorites?",
                confirmButtonText = "Remove",
                dismissButtonText = "Cancel",
                onConfirm = {
                    viewModel.onEvent(FavoritesEvent.ConfirmRemoveFavorite)
                },
                onDismiss = {
                    viewModel.onEvent(FavoritesEvent.DismissRemoveFavoriteConfirmation)
                }
            )
        } ?: run {
            viewModel.onEvent(FavoritesEvent.DismissRemoveFavoriteConfirmation)
        }
    }
}
