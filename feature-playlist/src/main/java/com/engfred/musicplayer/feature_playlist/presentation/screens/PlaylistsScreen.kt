package com.engfred.musicplayer.feature_playlist.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass // Import for responsiveness
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

    // Determine padding and grid columns based on window size class
    val contentHorizontalPadding = when (windowWidthSizeClass) {
        WindowWidthSizeClass.Compact -> 8.dp
        WindowWidthSizeClass.Medium -> 24.dp // More padding for tablets portrait/foldables
        WindowWidthSizeClass.Expanded -> 32.dp // Even more for tablets landscape/desktops
        else -> 8.dp // Default case
    }

    val gridCells = when (windowWidthSizeClass) {
        WindowWidthSizeClass.Compact -> GridCells.Fixed(2) // 2 columns for phones
        WindowWidthSizeClass.Medium -> GridCells.Adaptive(minSize = 160.dp) // Adaptive for medium screens, aiming for 3-4 items
        WindowWidthSizeClass.Expanded -> GridCells.Adaptive(minSize = 180.dp) // Adaptive for large screens, aiming for 4+ items
        else -> GridCells.Fixed(2)
    }

    Scaffold(
        floatingActionButton = {
            val bottomPadding = if(uiState.isPlaying) 142.dp else 75.dp
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .padding(bottom = bottomPadding)
                    .padding(end = 16.dp) // Consistent right padding for FABs
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
                    onClick = { viewModel.onEvent(PlaylistEvent.ToggleLayout) },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ) {
                    Icon(
                        modifier = Modifier.size(30.dp),
                        imageVector = if (uiState.currentLayout == PlaylistLayoutType.LIST) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.List,
                        contentDescription = "Toggle layout"
                    )
                }
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
                    if (uiState.currentLayout == PlaylistLayoutType.LIST) {
                        LazyColumn(
                            // Apply responsive horizontal padding here
                            contentPadding = PaddingValues(
                                horizontal = contentHorizontalPadding,
                                vertical = 8.dp
                            ),
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
                            columns = gridCells, // Use responsive grid cells
                            // Apply responsive padding for the grid
                            contentPadding = PaddingValues(contentHorizontalPadding),
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