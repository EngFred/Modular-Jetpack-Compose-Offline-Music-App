package com.engfred.musicplayer.feature_playlist.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.engfred.musicplayer.core.ui.CustomSnackbar
import com.engfred.musicplayer.core.ui.ErrorIndicator
import com.engfred.musicplayer.core.ui.InfoIndicator
import com.engfred.musicplayer.core.ui.LoadingIndicator
import com.engfred.musicplayer.feature_playlist.domain.model.LayoutType
import com.engfred.musicplayer.feature_playlist.presentation.components.CreatePlaylistDialog
import com.engfred.musicplayer.feature_playlist.presentation.components.PlaylistItem
import com.engfred.musicplayer.feature_playlist.presentation.components.PlaylistGridItem
import com.engfred.musicplayer.feature_playlist.presentation.viewmodel.PlaylistEvent
import com.engfred.musicplayer.feature_playlist.presentation.viewmodel.PlaylistViewModel

@Composable
fun PlaylistsScreen(
    viewModel: PlaylistViewModel = hiltViewModel(),
    onPlaylistClick: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                FloatingActionButton(
                    onClick = { viewModel.onEvent(PlaylistEvent.ShowCreatePlaylistDialog) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create new playlist"
                    )
                }
                FloatingActionButton(
                    onClick = { viewModel.onEvent(PlaylistEvent.ToggleLayout) },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ) {
                    Icon(
                        imageVector = if (uiState.currentLayout == LayoutType.LIST) Icons.Default.GridView else Icons.Default.List,
                        contentDescription = "Toggle layout"
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                CustomSnackbar(snackbarData = data)
            }
        },
        containerColor = Color.Transparent
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingIndicator()
                    }
                }
                uiState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ErrorIndicator(
                            message = uiState.error ?: "",
                            onRetry = { viewModel.onEvent(PlaylistEvent.LoadPlaylists) }
                        )
                    }
                }
                uiState.playlists.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        InfoIndicator(
                            message = "No playlists found.\nTap the '+' button to create your first playlist!",
                            icon = Icons.Default.MusicOff
                        )
                    }
                }
                else -> {
                    if (uiState.currentLayout == LayoutType.LIST) {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.playlists, key = { it.playlistId }) { playlist ->
                                PlaylistItem(
                                    playlist = playlist,
                                    onClick = onPlaylistClick,
                                    onDeleteClick = { playlistId ->
                                        viewModel.onEvent(PlaylistEvent.DeletePlaylist(playlistId))
                                    }
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.playlists, key = { it.playlistId }) { playlist ->
                                PlaylistGridItem(
                                    playlist = playlist,
                                    onClick = onPlaylistClick,
                                    onDeleteClick = { playlistId ->
                                        viewModel.onEvent(PlaylistEvent.DeletePlaylist(playlistId))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onConfirm = { playlistName ->
                    viewModel.onEvent(PlaylistEvent.CreatePlaylist(playlistName))
                },
                onDismiss = {
                    viewModel.onEvent(PlaylistEvent.HideCreatePlaylistDialog)
                },
                errorMessage = uiState.dialogInputError
            )
        }
    }
}