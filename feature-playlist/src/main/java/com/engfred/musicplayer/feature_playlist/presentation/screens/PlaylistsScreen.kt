package com.engfred.musicplayer.feature_playlist.presentation.screens

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
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
import com.engfred.musicplayer.feature_playlist.presentation.components.list.PlaylistGridItem
import com.engfred.musicplayer.feature_playlist.presentation.components.list.PlaylistItem
import com.engfred.musicplayer.feature_playlist.presentation.viewmodel.list.PlaylistEvent
import com.engfred.musicplayer.feature_playlist.presentation.viewmodel.list.PlaylistViewModel
import kotlin.math.max

/**
 * Main screen for displaying and managing playlists.
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun PlaylistsScreen(
    viewModel: PlaylistViewModel = hiltViewModel(),
    onPlaylistClick: (Long) -> Unit,
    onCreatePlaylist: () -> Unit,
) {
    // State and context initialization
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidthDp = configuration.screenWidthDp

    // Listen for one-time UI events (like Toast messages)
    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Dynamic layout calculations
    val contentHorizontalPadding = if (isLandscape) 24.dp else 12.dp
    val minColumnWidthDp = if (isLandscape) 200f else 160f
    val computedColumns = ((screenWidthDp.toFloat() / minColumnWidthDp).toInt()).coerceIn(2, 6)
    val gridColumns = max(2, computedColumns)

    // Main screen container
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
        // Handle different UI states
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorIndicator(
                        message = uiState.error ?: "",
                        onRetry = { viewModel.onEvent(PlaylistEvent.LoadPlaylists) }
                    )
                }
            }

            uiState.automaticPlaylists.isEmpty() && uiState.userPlaylists.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    InfoIndicator(
                        message = "No playlists found.\nTap the '+' button to create your first playlist!",
                        icon = Icons.Default.MusicOff
                    )
                }
            }

            else -> {
                // Main content: a single vertical scroller
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = contentHorizontalPadding,
                        end = contentHorizontalPadding,
                        top = 12.dp,
                        bottom = 96.dp // Leave space for FABs
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Automatic playlists row (if any)
                    if (uiState.automaticPlaylists.isNotEmpty()) {
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                            ) {
                                val automaticItemWidth = if (isLandscape) 200.dp else 160.dp
                                items(uiState.automaticPlaylists, key = { it.id }) { playlist ->
                                    AutomaticPlaylistItem(
                                        playlist = playlist,
                                        onClick = onPlaylistClick,
                                        modifier = Modifier.width(automaticItemWidth)
                                    )
                                }
                            }
                        }
                    }

                    // My Playlists section
                    if (uiState.userPlaylists.isNotEmpty()) {
                        item {
                            Text(
                                text = "My Playlists",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                            )
                        }

                        // Render user playlists based on the current layout
                        if (uiState.currentLayout == PlaylistLayoutType.LIST) {
                            items(uiState.userPlaylists, key = { it.id }) { playlist ->
                                PlaylistItem(
                                    playlist = playlist,
                                    onClick = onPlaylistClick,
                                    onDeleteClick = { playlistId ->
                                        viewModel.onEvent(PlaylistEvent.DeletePlaylist(playlistId))
                                    },
                                    isDeletable = !playlist.name.equals("Favorites", ignoreCase = true)
                                )
                            }
                        } else {
                            // GRID layout: implemented as chunked rows within the LazyColumn
                            val chunks = uiState.userPlaylists.chunked(gridColumns)
                            itemsIndexed(chunks) { _, rowPlaylists ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowPlaylists.forEach { playlist ->
                                        PlaylistGridItem(
                                            playlist = playlist,
                                            onClick = onPlaylistClick,
                                            onDeleteClick = { playlistId ->
                                                viewModel.onEvent(PlaylistEvent.DeletePlaylist(playlistId))
                                            },
                                            modifier = Modifier.weight(1f),
                                            isDeletable = !playlist.name.equals("Favorites", ignoreCase = true)
                                        )
                                    }
                                    // Add spacers for incomplete rows to maintain alignment
                                    val emptySlots = gridColumns - rowPlaylists.size
                                    repeat(emptySlots) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        // Message when there are no user playlists
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Your own playlists will show up here. Create some!",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(horizontal = 30.dp),
                                    textAlign = TextAlign.Center
                                )
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
                .padding(end = 16.dp, bottom = 16.dp)
        ) {
            // Add new playlist FAB
            FloatingActionButton(
                onClick = onCreatePlaylist,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(
                    modifier = Modifier.size(36.dp),
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Create new playlist"
                )
            }

            // Toggle layout FAB
            FloatingActionButton(
                onClick = { viewModel.onEvent(PlaylistEvent.ToggleLayout) },
            ) {
                Icon(
                    modifier = Modifier.size(if (uiState.currentLayout == PlaylistLayoutType.LIST) 24.dp else 30.dp),
                    imageVector = if (uiState.currentLayout == PlaylistLayoutType.LIST) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.List,
                    contentDescription = "Toggle layout for My Playlists"
                )
            }
        }
    }
}