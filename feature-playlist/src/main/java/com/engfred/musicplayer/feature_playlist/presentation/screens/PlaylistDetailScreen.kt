package com.engfred.musicplayer.feature_playlist.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.engfred.musicplayer.core.ui.CustomTopBar
import com.engfred.musicplayer.core.util.formatDate
import com.engfred.musicplayer.feature_playlist.presentation.components.AddSongsDialog
import com.engfred.musicplayer.feature_playlist.presentation.components.PlaylistSongItem
import com.engfred.musicplayer.feature_playlist.presentation.components.RenamePlaylistDialog
import com.engfred.musicplayer.feature_playlist.presentation.viewmodel.PlaylistDetailEvent
import com.engfred.musicplayer.feature_playlist.presentation.viewmodel.PlaylistDetailViewModel

/**
 * Composable screen for displaying the details of a specific playlist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onAudioFileClick: (String) -> Unit
) {
    val uiState = viewModel.uiState

    Scaffold(
        // Remove topBar parameter from Scaffold
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Do not apply top padding from Scaffold here, CustomTopBar will handle it
                .padding(bottom = paddingValues.calculateBottomPadding())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- Custom Top Bar ---
            CustomTopBar(
                title = uiState.playlist?.name ?: "Playlist Details",
                showNavigationIcon = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    if (uiState.playlist != null) {
                        IconButton(onClick = { viewModel.onEvent(PlaylistDetailEvent.ShowRenameDialog) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename Playlist",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(onClick = { viewModel.onEvent(PlaylistDetailEvent.ShowAddSongsDialog) }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Songs to Playlist",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            )
            // --- End Custom Top Bar ---

            // Content below the custom top bar
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp) // Apply padding for content
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
                    uiState.playlist == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Playlist not found.",
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                            // Removed top/bottom padding from here, added to parent Column for all content
                        ) {
                            Text(
                                text = "Created: ${formatDate(uiState.playlist.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Total songs: ${uiState.playlist.songs.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (uiState.playlist.songs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "This playlist is empty. Add some songs!",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(uiState.playlist.songs, key = { it.id }) { audioFile ->
                                    PlaylistSongItem(
                                        audioFile = audioFile,
                                        onClick = { clickedAudioFile ->
                                            viewModel.onEvent(PlaylistDetailEvent.PlaySong(clickedAudioFile, onAudioFileClick))
                                        },
                                        onRemoveClick = { audioFileId ->
                                            viewModel.onEvent(PlaylistDetailEvent.RemoveSong(audioFileId))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.showRenameDialog && uiState.playlist != null) {
            RenamePlaylistDialog(
                currentName = uiState.playlist.name,
                onConfirm = { newName ->
                    viewModel.onEvent(PlaylistDetailEvent.RenamePlaylist(newName))
                },
                onDismiss = {
                    viewModel.onEvent(PlaylistDetailEvent.HideRenameDialog)
                },
                errorMessage = uiState.error
            )
        }

        if (uiState.showAddSongsDialog && uiState.playlist != null) {
            AddSongsDialog(
                allAudioFiles = viewModel.getAllDeviceAudioFiles(),
                currentPlaylistSongs = uiState.playlist.songs,
                onAddSongs = { selectedSongs ->
                    selectedSongs.forEach { audioFile ->
                        viewModel.onEvent(PlaylistDetailEvent.AddSong(audioFile))
                    }
                },
                onDismiss = {
                    viewModel.onEvent(PlaylistDetailEvent.HideAddSongsDialog)
                }
            )
        }
    }
}