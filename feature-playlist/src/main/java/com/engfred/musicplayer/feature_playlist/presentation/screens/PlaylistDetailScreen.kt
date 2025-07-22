package com.engfred.musicplayer.feature_playlist.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.engfred.musicplayer.core.ui.AudioFileItem
import com.engfred.musicplayer.core.ui.ErrorIndicator
import com.engfred.musicplayer.core.ui.InfoIndicator
import com.engfred.musicplayer.core.ui.LoadingIndicator
import com.engfred.musicplayer.feature_playlist.presentation.components.AddSongsDialog
import com.engfred.musicplayer.feature_playlist.presentation.components.PlaylistDetailHeader
import com.engfred.musicplayer.feature_playlist.presentation.components.RenamePlaylistDialog
import com.engfred.musicplayer.feature_playlist.presentation.viewmodel.PlaylistDetailEvent
import com.engfred.musicplayer.feature_playlist.presentation.viewmodel.PlaylistDetailViewModel
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember

@Composable
fun PlaylistDetailScreen(
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onAudioFileClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                FloatingActionButton(
                    onClick = { viewModel.onEvent(PlaylistDetailEvent.ShowAddSongsDialog) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, "Add songs to playlist")
                }
                FloatingActionButton(
                    onClick = { viewModel.onEvent(PlaylistDetailEvent.ShowRenameDialog) },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ) {
                    Icon(Icons.Default.Edit, "Rename playlist")
                }
            }
        }
    ) { paddingValues ->
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
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicator(modifier = Modifier.fillMaxSize())
                }
                uiState.error != null -> {
                    ErrorIndicator(
                        modifier = Modifier.fillMaxSize(),
                        message = uiState.error!!,
                        onRetry = {
                            uiState.playlist?.playlistId?.let { playlistId ->
                                viewModel.onEvent(PlaylistDetailEvent.LoadPlaylist(playlistId))
                            } ?: run {
                                onNavigateBack()
                            }
                        }
                    )
                }
                uiState.playlist == null -> {
                    InfoIndicator(
                        modifier = Modifier.fillMaxSize(),
                        message = "Playlist not found or could not be loaded. It might have been deleted.",
                        icon = Icons.Outlined.LibraryMusic
                    )
                }
                else -> {
                    PlaylistDetailHeader(playlist = uiState.playlist)

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedVisibility(
                        visible = uiState.playlist?.songs!!.isEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        InfoIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            message = "This playlist is empty. Tap the '+' button to add songs!",
                            icon = Icons.Outlined.LibraryMusic
                        )
                    }

                    AnimatedVisibility(
                        visible = uiState.playlist?.songs!!.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.playlist?.songs!!, key = { it.id }) { audioFile ->
                                AudioFileItem(
                                    audioFile = audioFile,
                                    isPlaying = (audioFile.id == uiState.currentPlayingId),
                                    onClick = { clickedAudioFile ->
                                        viewModel.onEvent(PlaylistDetailEvent.PlaySong(clickedAudioFile, onAudioFileClick))
                                    },
                                    onDelete = {
                                        viewModel.onEvent(PlaylistDetailEvent.RemoveSong(it.id))
                                    },
                                    modifier = Modifier.animateItem(),
                                    snackbarHostState = snackbarHostState
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.showRenameDialog && uiState.playlist != null) {
            RenamePlaylistDialog(
                currentName = uiState.playlist?.name!!,
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
                currentPlaylistSongs = uiState.playlist?.songs!!,
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