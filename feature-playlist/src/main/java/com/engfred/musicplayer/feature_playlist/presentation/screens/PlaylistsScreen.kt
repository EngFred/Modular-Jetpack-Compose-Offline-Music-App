package com.engfred.musicplayer.feature_playlist.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.engfred.musicplayer.core.ui.ErrorIndicator
import com.engfred.musicplayer.core.ui.InfoIndicator
import com.engfred.musicplayer.core.ui.LoadingIndicator
import com.engfred.musicplayer.core.domain.model.PlaylistLayoutType
import com.engfred.musicplayer.feature_playlist.presentation.components.list.CreatePlaylistDialog
import com.engfred.musicplayer.feature_playlist.presentation.components.list.PlaylistGridItem
import com.engfred.musicplayer.feature_playlist.presentation.components.list.PlaylistItem
import com.engfred.musicplayer.feature_playlist.presentation.viewmodel.list.PlaylistEvent
import com.engfred.musicplayer.feature_playlist.presentation.viewmodel.list.PlaylistViewModel

/**
 * Composable for the Playlists screen, displaying user-created and automatic playlists.
 *
 * @param viewModel The ViewModel for managing playlist state and events.
 * @param onPlaylistClick Callback when a playlist is clicked, providing its ID.
 * @param windowWidthSizeClass The current window width size class for responsive layout.
 */
@Composable
fun PlaylistsScreen(
    viewModel: PlaylistViewModel = hiltViewModel(),
    onPlaylistClick: (Long) -> Unit,
    windowWidthSizeClass: WindowWidthSizeClass,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val contentHorizontalPadding = when (windowWidthSizeClass) {
        WindowWidthSizeClass.Compact -> 8.dp
        WindowWidthSizeClass.Medium -> 24.dp
        WindowWidthSizeClass.Expanded -> 32.dp
        else -> 8.dp
    }

    val gridCells = when (uiState.currentLayout) {
        PlaylistLayoutType.LIST -> GridCells.Fixed(1)
        PlaylistLayoutType.GRID -> when (windowWidthSizeClass) {
            WindowWidthSizeClass.Compact -> GridCells.Fixed(2)
            WindowWidthSizeClass.Medium -> GridCells.Adaptive(minSize = 160.dp)
            WindowWidthSizeClass.Expanded -> GridCells.Adaptive(minSize = 180.dp)
            else -> GridCells.Fixed(2)
        }
    }

    // Replace Scaffold with a Box layout
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
        // Main content column
        Column(
            modifier = Modifier.fillMaxSize()
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
                uiState.automaticPlaylists.isEmpty() && uiState.userPlaylists.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        InfoIndicator(
                            message = "No playlists found.\nTap the '+' button to create your first playlist!",
                            icon = Icons.Default.MusicOff
                        )
                    }
                }
                else -> {
                    if (uiState.currentLayout == PlaylistLayoutType.LIST) {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                horizontal = contentHorizontalPadding,
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.automaticPlaylists, key = { it.id }) { playlist ->
                                PlaylistItem(
                                    playlist = playlist,
                                    onClick = onPlaylistClick,
                                    onDeleteClick = { playlistId ->
                                        viewModel.onEvent(PlaylistEvent.DeletePlaylist(playlistId))
                                    },
                                    isDeletable = false // Automatic playlists are not deletable
                                )
                            }

                            if (uiState.userPlaylists.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "My Playlists",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                                items(uiState.userPlaylists, key = { it.id }) { playlist ->
                                    PlaylistItem(
                                        playlist = playlist,
                                        onClick = onPlaylistClick,
                                        onDeleteClick = { playlistId ->
                                            viewModel.onEvent(PlaylistEvent.DeletePlaylist(playlistId))
                                        },
                                        isDeletable = true
                                    )
                                }
                            }
                        }
                    } else { // Grid Layout
                        LazyVerticalGrid(
                            columns = gridCells,
                            contentPadding = PaddingValues(contentHorizontalPadding),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.automaticPlaylists, key = { it.id }) { playlist ->
                                PlaylistGridItem(
                                    playlist = playlist,
                                    onClick = onPlaylistClick,
                                    onDeleteClick = { playlistId ->
                                        viewModel.onEvent(PlaylistEvent.DeletePlaylist(playlistId))
                                    },
                                    isDeletable = false // Automatic playlists are not deletable
                                )
                            }
                            if (uiState.automaticPlaylists.isNotEmpty() && uiState.userPlaylists.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) { // Spacer for grid
                                    Spacer(Modifier.height(16.dp))
                                }
                            }
                            if (uiState.userPlaylists.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) { // Span across all columns for header
                                    Text(
                                        text = "My Playlists",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                                items(uiState.userPlaylists, key = { it.id }) { playlist ->
                                    PlaylistGridItem(
                                        playlist = playlist,
                                        onClick = onPlaylistClick,
                                        onDeleteClick = { playlistId ->
                                            viewModel.onEvent(PlaylistEvent.DeletePlaylist(playlistId))
                                        },
                                        isDeletable = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Buttons (FABs)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp)
        ) {
            FloatingActionButton(
                onClick = { viewModel.onEvent(PlaylistEvent.ShowCreatePlaylistDialog) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(
                    modifier = Modifier.size(36.dp),
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Create new playlist"
                )
            }
            FloatingActionButton(
                onClick = { viewModel.onEvent(PlaylistEvent.ToggleLayout) }
            ) {
                Icon(
                    modifier = Modifier.size(if (uiState.currentLayout == PlaylistLayoutType.LIST) 24.dp else 30.dp),
                    imageVector = if (uiState.currentLayout == PlaylistLayoutType.LIST) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.List,
                    contentDescription = "Toggle layout"
                )
            }
        }

        // Create Playlist Dialog
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