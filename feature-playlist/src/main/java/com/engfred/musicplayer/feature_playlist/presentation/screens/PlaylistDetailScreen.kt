package com.engfred.musicplayer.feature_playlist.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.engfred.musicplayer.core.ui.ErrorIndicator
import com.engfred.musicplayer.core.ui.InfoIndicator
import com.engfred.musicplayer.core.ui.LoadingIndicator
import com.engfred.musicplayer.feature_playlist.domain.model.PlaylistDetailScreenMode
import com.engfred.musicplayer.feature_playlist.presentation.components.PlaylistDetailHeaderSection
import com.engfred.musicplayer.feature_playlist.presentation.components.RenamePlaylistDialog
import com.engfred.musicplayer.feature_playlist.presentation.components.detail.AddSongsScreenContent
import com.engfred.musicplayer.feature_playlist.presentation.components.detail.PlaylistActionButtons
import com.engfred.musicplayer.feature_playlist.presentation.components.detail.PlaylistEmptyState
import com.engfred.musicplayer.feature_playlist.presentation.components.detail.PlaylistSongs
import com.engfred.musicplayer.feature_playlist.presentation.components.detail.PlaylistSongsHeader
import com.engfred.musicplayer.feature_playlist.presentation.viewmodel.PlaylistDetailEvent
import com.engfred.musicplayer.feature_playlist.presentation.viewmodel.PlaylistDetailViewModel

// Enum to define sorting options (remains in the same file or a common util file)
enum class PlaylistSortOrder {
    DATE_ADDED,
    ALPHABETICAL
}

@Composable
fun PlaylistDetailScreen(
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onAudioFileClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    var moreMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var currentSortOrder by remember { mutableStateOf(PlaylistSortOrder.DATE_ADDED) }

    val sortedSongs = remember(uiState.playlist?.songs, currentSortOrder) {
        uiState.playlist?.songs?.let { songs ->
            when (currentSortOrder) {
                PlaylistSortOrder.DATE_ADDED -> songs
                PlaylistSortOrder.ALPHABETICAL -> songs.sortedBy { it.title.lowercase() }
            }
        } ?: emptyList()
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
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
        ) { // Use AnimatedContent to transition between the two main modes
            AnimatedContent(
                targetState = uiState.screenMode,
                transitionSpec = {
                    // Custom transition: slide in/out vertically
                    (slideInVertically { height -> height } + fadeIn())
                        .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                }, label = "playlistScreenModeTransition"
            ) { targetScreenMode ->
                when (targetScreenMode) {
                    PlaylistDetailScreenMode.VIEW_PLAYLIST -> {
                        // All existing playlist detail content goes here
                        Column(modifier = Modifier.fillMaxSize()) {
                            PlaylistDetailHeaderSection(
                                playlist = uiState.playlist,
                                onNavigateBack = onNavigateBack,
                                onAddSongsClick = { viewModel.onEvent(PlaylistDetailEvent.ChangeScreenMode(PlaylistDetailScreenMode.ADD_SONGS)) }, // Change mode
                                onRenamePlaylistClick = { viewModel.onEvent(PlaylistDetailEvent.ShowRenameDialog) },
                                moreMenuExpanded = moreMenuExpanded,
                                onMoreMenuExpandedChange = { moreMenuExpanded = it }
                            )

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
                                    Spacer(modifier = Modifier.height(32.dp))

                                    PlaylistActionButtons(
                                        onPlayClick = {
                                            uiState.playlist?.songs?.firstOrNull()?.let { firstSong ->
                                                viewModel.onEvent(PlaylistDetailEvent.PlaySong(firstSong, onAudioFileClick))
                                            }
                                        },
                                        onShuffleClick = {
                                            if (uiState.playlist?.songs?.isNotEmpty() == true) {
                                                viewModel.onEvent(PlaylistDetailEvent.ShufflePlay)
                                                uiState.playlist?.songs?.firstOrNull()?.let {
                                                    onAudioFileClick(it.id.toString())
                                                }
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    PlaylistSongsHeader(
                                        songCount = uiState.playlist?.songs?.size ?: 0,
                                        currentSortOrder = currentSortOrder,
                                        onSortOrderChange = { currentSortOrder = it },
                                        sortMenuExpanded = sortMenuExpanded,
                                        onSortMenuExpandedChange = { sortMenuExpanded = it }
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (uiState.playlist?.songs.isNullOrEmpty()) {
                                        PlaylistEmptyState()
                                    } else {
                                        PlaylistSongs(
                                            songs = sortedSongs,
                                            currentPlayingId = uiState.currentPlayingId,
                                            onSongClick = { clickedAudioFile ->
                                                viewModel.onEvent(PlaylistDetailEvent.PlaySong(clickedAudioFile, onAudioFileClick))
                                            },
                                            onSongDelete = { song ->
                                                viewModel.onEvent(PlaylistDetailEvent.RemoveSong(song.id))
                                            },
                                            snackbarHostState = snackbarHostState,
                                            listState = listState
                                        )
                                    }
                                }
                            }
                        }
                    }
                    PlaylistDetailScreenMode.ADD_SONGS -> {
                        // Display the Add Songs content
                        AddSongsScreenContent(
                            allAudioFiles = uiState.allAudioFiles,
                            currentPlaylistSongs = uiState.playlist?.songs.orEmpty(),
                            onSongsSelected = { selectedSongs ->
                                selectedSongs.forEach { audioFile ->
                                    viewModel.onEvent(PlaylistDetailEvent.AddSong(audioFile))
                                }
                                viewModel.onEvent(PlaylistDetailEvent.ChangeScreenMode(PlaylistDetailScreenMode.VIEW_PLAYLIST)) // Go back to view mode
                            },
                            onNavigateBack = {
                                viewModel.onEvent(PlaylistDetailEvent.ChangeScreenMode(PlaylistDetailScreenMode.VIEW_PLAYLIST)) // Go back without adding
                            }
                        )
                    }
                }
            }
        }

        // Dialogs remain at the top level of the composable (outside AnimatedContent)
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
    }
}