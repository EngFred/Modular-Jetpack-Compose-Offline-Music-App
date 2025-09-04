package com.engfred.musicplayer.feature_favorites.presentation.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.engfred.musicplayer.feature_favorites.presentation.viewmodel.FavoritesEvent
import com.engfred.musicplayer.feature_favorites.presentation.viewmodel.FavoritesViewModel
import androidx.compose.foundation.background
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.engfred.musicplayer.core.ui.AudioFileItem
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.engfred.musicplayer.core.ui.AddSongToPlaylistDialog
import com.engfred.musicplayer.core.ui.ConfirmationDialog

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

    Scaffold(
        containerColor = Color.Transparent
    ){ paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                )
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
                            key = { uiState.favoriteAudioFiles[it].id }
                        ) { index->
                            val favoriteAudioFile = uiState.favoriteAudioFiles[index]

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
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Show the dialog if showAddToPlaylistDialog is true
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
            // If dialog is somehow shown without an audioFileToRemove, dismiss it.
            viewModel.onEvent(FavoritesEvent.DismissRemoveFavoriteConfirmation)
        }
    }
}