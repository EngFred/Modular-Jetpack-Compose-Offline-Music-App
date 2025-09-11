package com.engfred.musicplayer.feature_playlist.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.engfred.musicplayer.core.domain.model.PlaylistLayoutType
import com.engfred.musicplayer.core.ui.ErrorIndicator
import com.engfred.musicplayer.core.ui.InfoIndicator
import com.engfred.musicplayer.core.ui.LoadingIndicator
import com.engfred.musicplayer.feature_playlist.presentation.components.list.AutomaticPlaylistItem
import com.engfred.musicplayer.feature_playlist.presentation.components.list.CreatePlaylistDialog
import com.engfred.musicplayer.feature_playlist.presentation.components.list.PlaylistGridItem
import com.engfred.musicplayer.feature_playlist.presentation.components.list.PlaylistItem
import com.engfred.musicplayer.feature_playlist.presentation.viewmodel.list.PlaylistEvent
import com.engfred.musicplayer.feature_playlist.presentation.viewmodel.list.PlaylistViewModel

/**
 * Composable for the Playlists screen, displaying automatic playlists as a top scrollable row
 * and user-created playlists below (user can toggle list/grid for their own playlists only).
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

    val gridCellsForUserPlaylists = when (uiState.currentLayout) {
        PlaylistLayoutType.LIST -> GridCells.Fixed(1)
        PlaylistLayoutType.GRID -> when (windowWidthSizeClass) {
            WindowWidthSizeClass.Compact -> GridCells.Fixed(2)
            WindowWidthSizeClass.Medium -> GridCells.Adaptive(minSize = 160.dp)
            WindowWidthSizeClass.Expanded -> GridCells.Adaptive(minSize = 180.dp)
            else -> GridCells.Fixed(2)
        }
    }

    // Background container
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
        Column(modifier = Modifier.fillMaxSize()) {
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
                // If both lists are empty show the general info state
                uiState.automaticPlaylists.isEmpty() && uiState.userPlaylists.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        InfoIndicator(
                            message = "No playlists found.\nTap the '+' button to create your first playlist!",
                            icon = Icons.Default.MusicOff
                        )
                    }
                }
                else -> {
                    // ALWAYS show automatic playlists as a horizontal scrollable row at the top (if any)

                    val automaticItemWidth = when (windowWidthSizeClass) {
                        WindowWidthSizeClass.Compact -> 160.dp
                        WindowWidthSizeClass.Medium -> 180.dp
                        WindowWidthSizeClass.Expanded -> 200.dp
                        else -> 160.dp
                    }

                    if (uiState.automaticPlaylists.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(
                                start = contentHorizontalPadding,
                                end = contentHorizontalPadding,
                                top = 12.dp,
                                bottom = 8.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        ) {
                            items(uiState.automaticPlaylists, key = { it.id }) { playlist ->
                                AutomaticPlaylistItem(
                                    playlist = playlist,
                                    onClick = onPlaylistClick,
                                    modifier = Modifier.width(automaticItemWidth)
                                )
                            }
                        }
                    }

                    // Main area: user playlists (list or grid) — automatic playlists were surfaced above.
                    if (uiState.userPlaylists.isNotEmpty()) {
                        if (uiState.currentLayout == PlaylistLayoutType.LIST) {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    horizontal = contentHorizontalPadding,
                                    vertical = 8.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
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
                        } else {
                            // GRID for user playlists
                            LazyVerticalGrid(
                                columns = gridCellsForUserPlaylists,
                                contentPadding = PaddingValues(
                                    horizontal = contentHorizontalPadding,
                                    vertical = 8.dp
                                ),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
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
                                    )
                                }
                            }
                        }
                    } else {
                        // No user playlists — show hint while keeping automatic row visible above
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Your own playlists will show up here. Create some!",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 16.dp),
                                textAlign = TextAlign.Center
                            )
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
                onClick = { viewModel.onEvent(PlaylistEvent.ToggleLayout) },
                // this FAB now toggles layout for user playlists only (automatic playlists stay in top row)
            ) {
                Icon(
                    modifier = Modifier.size(if (uiState.currentLayout == PlaylistLayoutType.LIST) 24.dp else 30.dp),
                    imageVector = if (uiState.currentLayout == PlaylistLayoutType.LIST) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.List,
                    contentDescription = "Toggle layout for My Playlists"
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

///**
// * Simple compact chip for automatic playlists shown in a horizontal row.
// * Keep this small and tappable — automatic playlists are not deletable and are surfaced here.
// */
//@Composable
//private fun AutomaticPlaylistChip(
//    playlistName: String,
//    onClick: () -> Unit,
//    height: Dp = 84.dp,
//    cornerRadius: Dp = 12.dp
//) {
//    val shape: Shape = RoundedCornerShape(cornerRadius)
//    Box(
//        modifier = Modifier
//            .width(160.dp)
//            .height(height)
//            .clip(shape)
//            .background(MaterialTheme.colorScheme.surfaceVariant)
//            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
//            .clickable { onClick() }
//            .padding(12.dp)
//    ) {
//        Column(modifier = Modifier
//            .wrapContentWidth()
//            .wrapContentHeight()
//        ) {
//            Text(
//                text = playlistName,
//                style = MaterialTheme.typography.titleMedium,
//                fontWeight = FontWeight.SemiBold,
//                maxLines = 2,
//                overflow = TextOverflow.Ellipsis,
//                fontSize = 14.sp,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//        }
//    }
//}
