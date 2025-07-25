package com.engfred.musicplayer.feature_player.presentation.components.layouts.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import com.engfred.musicplayer.core.domain.model.PlayerLayout

/**
 * TopBar component for the music player screen.
 * Adapts its content and arrangement based on the selected [PlayerLayout]
 * and [WindowWidthSizeClass]. Provides navigation, queue status, and layout selection.
 *
 * @param modifier The modifier to be applied to the TopBar.
 * @param onNavigateUp Callback for navigating back.
 * @param currentSongIndex The 0-based index of the currently playing song in the queue.
 * @param totalQueueSize The total number of songs in the playback queue.
 * @param onOpenQueue Callback to open the playback queue (e.g., as a bottom sheet).
 * @param windowWidthSizeClass The current window size class.
 * @param selectedLayout The currently active player layout.
 * @param onLayoutSelected Callback to change the active player layout.
 * @param isFavorite Boolean indicating if the current song is a favorite (only used in Minimalist Groove).
 * @param onToggleFavorite Callback to toggle the favorite status (only used in Minimalist Groove).
 */
@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    onNavigateUp: () -> Unit,
    currentSongIndex: Int,
    totalQueueSize: Int,
    onOpenQueue: () -> Unit,
    windowWidthSizeClass: WindowWidthSizeClass,
    selectedLayout: PlayerLayout,
    onLayoutSelected: (PlayerLayout) -> Unit,
    // These parameters are specific to Minimalist Groove and will be passed conditionally
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {}
) {
    var showLayoutMenu by remember { mutableStateOf(false) }

    when (selectedLayout) {
        PlayerLayout.MINIMALIST_GROOVE -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateUp,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        Icons.Rounded.ArrowBackIosNew,
                        contentDescription = "Navigate up",
                        tint = LocalContentColor.current
                    )
                }

                // Centered "Now Playing" text
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Now Playing",
                        style = MaterialTheme.typography.titleMedium,
                        color = LocalContentColor.current.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                            tint = if (isFavorite) Color(0xFFFF5252) else LocalContentColor.current.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = { showLayoutMenu = true }) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "Change Player Layout",
                            tint = LocalContentColor.current
                        )
                    }
                    // Layout selection dropdown for Minimalist Groove
                    LayoutDropdownMenu(
                        expanded = showLayoutMenu,
                        onDismissRequest = { showLayoutMenu = false },
                        selectedLayout = selectedLayout,
                        onLayoutSelected = onLayoutSelected
                    )
                }
            }
        }

        PlayerLayout.IMMERSIVE_CANVAS -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateUp,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        Icons.Rounded.ArrowBackIosNew,
                        contentDescription = "Navigate up",
                        tint = LocalContentColor.current
                    )
                }

                Text(
                    text = "${currentSongIndex + 1}/$totalQueueSize",
                    style = MaterialTheme.typography.titleMedium,
                    color = LocalContentColor.current.copy(alpha = 0.8f),
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showLayoutMenu = true }) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "Change Player Layout",
                            tint = LocalContentColor.current
                        )
                    }
                    // Layout selection dropdown for Immersive Canvas
                    LayoutDropdownMenu(
                        expanded = showLayoutMenu,
                        onDismissRequest = { showLayoutMenu = false },
                        selectedLayout = selectedLayout,
                        onLayoutSelected = onLayoutSelected
                    )
                }
            }
        }

        PlayerLayout.ETHEREAL_FLOW -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateUp,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        Icons.Rounded.ArrowBackIosNew,
                        contentDescription = "Navigate up",
                        tint = LocalContentColor.current
                    )
                }

                // Centered song index text
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${currentSongIndex + 1}/$totalQueueSize",
                        style = MaterialTheme.typography.titleMedium,
                        color = LocalContentColor.current.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (windowWidthSizeClass != WindowWidthSizeClass.Expanded) {
                        IconButton(onClick = onOpenQueue) {
                            Icon(
                                Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = "Open Queue",
                                tint = LocalContentColor.current
                            )
                        }
                    } else {
                        // Maintain spacing when queue button is not shown in expanded mode
                        Spacer(modifier = Modifier.size(48.dp)) // Matches IconButton size
                    }
                    IconButton(onClick = { showLayoutMenu = true }) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "Change Player Layout",
                            tint = LocalContentColor.current
                        )
                    }
                    // Layout selection dropdown for Ethereal Flow
                    LayoutDropdownMenu(
                        expanded = showLayoutMenu,
                        onDismissRequest = { showLayoutMenu = false },
                        selectedLayout = selectedLayout,
                        onLayoutSelected = onLayoutSelected
                    )
                }
            }
        }
    }
}

/**
 * Reusable Composable for the player layout selection dropdown menu.
 *
 * @param expanded Whether the dropdown menu is currently expanded.
 * @param onDismissRequest Callback to dismiss the dropdown menu.
 * @param selectedLayout The currently selected player layout.
 * @param onLayoutSelected Callback to change the active player layout.
 */
@Composable
private fun LayoutDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    selectedLayout: PlayerLayout,
    onLayoutSelected: (PlayerLayout) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .animateContentSize()
            .alpha(
                animateFloatAsState(
                    targetValue = if (expanded) 1f else 0f,
                    animationSpec = tween(durationMillis = 200),
                    label = "dropdownFade"
                ).value
            ),
        offset = DpOffset(x = (-12).dp, y = 8.dp) // Offset the dropdown for better alignment with the icon
    ) {
        PlayerLayout.entries.forEach { layout ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = when (layout) {
                            PlayerLayout.ETHEREAL_FLOW -> "Ethereal Flow"
                            PlayerLayout.IMMERSIVE_CANVAS -> "Immersive Canvas"
                            PlayerLayout.MINIMALIST_GROOVE -> "Minimalist Groove"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (selectedLayout == layout) FontWeight.Bold else FontWeight.Normal // Use Normal for non-selected
                    )
                },
                onClick = {
                    onLayoutSelected(layout)
                    onDismissRequest() // Dismiss the menu after selection
                },
                modifier = Modifier
                    .background(
                        if (selectedLayout == layout)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else
                            Color.Transparent // Use Color.Transparent for default background
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}