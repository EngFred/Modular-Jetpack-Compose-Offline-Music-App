package com.engfred.musicplayer.feature_playlist.presentation.screens

import android.annotation.SuppressLint
import android.content.res.Configuration
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

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun PlaylistsScreen(
    viewModel: PlaylistViewModel = hiltViewModel(),
    onPlaylistClick: (Long) -> Unit,
    onCreatePlaylist: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidthDp = configuration.screenWidthDp

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Horizontal padding slightly larger in landscape
    val contentHorizontalPadding = if (isLandscape) 24.dp else 12.dp

    // Compute columns dynamically from available screen width (dp)
    // Tweak minColumnWidthDp to make each column wider/narrower as desired.
    val minColumnWidthDp = if (isLandscape) 200f else 160f
    // Compute columns (as Int). Ensure at least 2 columns and cap at 6.
    val computedColumns = ((screenWidthDp.toFloat() / minColumnWidthDp).toInt()).coerceIn(2, 6)

    // Fallback to ensure we never pass invalid column count
    val gridColumns = max(2, computedColumns)

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
                // Single vertical scroller that contains:
                // 1) optional automatic playlists row
                // 2) header "My Playlists"
                // 3) either list or a chunked grid implemented with rows (so everything scrolls together)
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = contentHorizontalPadding,
                        end = contentHorizontalPadding,
                        top = 12.dp,
                        bottom = 96.dp // leave space for FABs
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Automatic playlists row (if any)
                    if (uiState.automaticPlaylists.isNotEmpty()) {
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = contentHorizontalPadding),
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

                    // Header
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

                        // LIST layout
                        if (uiState.currentLayout == PlaylistLayoutType.LIST) {
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
                        } else {
                            // GRID layout implemented as chunked rows so the entire page is one LazyColumn.
                            // This avoids nested vertical scrolling and works well in landscape and tablets.
                            val chunks = uiState.userPlaylists.chunked(gridColumns)
                            itemsIndexed(chunks) { _, rowPlaylists ->
                                androidx.compose.foundation.layout.Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowPlaylists.forEach { playlist ->
                                        // Each grid cell should expand equally
                                        PlaylistGridItem(
                                            playlist = playlist,
                                            onClick = onPlaylistClick,
                                            onDeleteClick = { playlistId ->
                                                viewModel.onEvent(PlaylistEvent.DeletePlaylist(playlistId))
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    // If the row is not full, add spacer(s) to keep alignment
                                    val emptySlots = gridColumns - rowPlaylists.size
                                    repeat(emptySlots) {
                                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        // No user playlists message (if only automatic exist)
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
        androidx.compose.foundation.layout.Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp) // comfortable spacing
        ) {
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
