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
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import com.engfred.musicplayer.feature_player.domain.model.PlayerLayout
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass

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
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = LocalContentColor.current
                    )
                }

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

                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                                tint = if (isFavorite) Color(0xFFFF5252) else LocalContentColor.current.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = { showLayoutMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Change Player Layout",
                                tint = LocalContentColor.current
                            )
                        }
                    }

                    // --- Layout Selection Dropdown Menu ---
                    DropdownMenu(
                        expanded = showLayoutMenu,
                        onDismissRequest = { showLayoutMenu = false },
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .shadow(8.dp, RoundedCornerShape(12.dp))
                            .animateContentSize()
                            .alpha(
                                animateFloatAsState(
                                    targetValue = if (showLayoutMenu) 1f else 0f,
                                    animationSpec = tween(durationMillis = 200),
                                    label = "dropdownFade"
                                ).value
                            ),
                        offset = DpOffset(x = (-12).dp, y = 8.dp)
                    ) {
                        PlayerLayout.entries.forEach { layout ->
                            var isPressed by remember { mutableStateOf(false) }
                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.95f else 1f,
                                animationSpec = tween(durationMillis = 100),
                                label = "itemScale"
                            )

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
                                        fontWeight = if (selectedLayout == layout) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    isPressed = true
                                    onLayoutSelected(layout)
                                    showLayoutMenu = false
                                },
                                modifier = Modifier
                                    .background(
                                        if (selectedLayout == layout)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else
                                            MaterialTheme.colorScheme.surface
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .scale(scale)
                            )
                        }
                    }
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
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = LocalContentColor.current
                    )
                }

                Text(
                    text = "$currentSongIndex/$totalQueueSize",
                    style = MaterialTheme.typography.titleMedium,
                    color = LocalContentColor.current.copy(alpha = 0.8f),
                    fontWeight = FontWeight.SemiBold
                )

                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showLayoutMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Change Player Layout",
                                tint = LocalContentColor.current
                            )
                        }
                    }

                    // --- Layout Selection Dropdown Menu ---
                    DropdownMenu(
                        expanded = showLayoutMenu,
                        onDismissRequest = { showLayoutMenu = false },
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .shadow(8.dp, RoundedCornerShape(12.dp))
                            .animateContentSize()
                            .alpha(
                                animateFloatAsState(
                                    targetValue = if (showLayoutMenu) 1f else 0f,
                                    animationSpec = tween(durationMillis = 200),
                                    label = "dropdownFade"
                                ).value
                            ),
                        offset = DpOffset(x = (-12).dp, y = 8.dp)
                    ) {
                        PlayerLayout.entries.forEach { layout ->
                            var isPressed by remember { mutableStateOf(false) }
                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.95f else 1f,
                                animationSpec = tween(durationMillis = 100),
                                label = "itemScale"
                            )

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
                                        fontWeight = if (selectedLayout == layout) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    isPressed = true
                                    onLayoutSelected(layout)
                                    showLayoutMenu = false
                                },
                                modifier = Modifier
                                    .background(
                                        if (selectedLayout == layout)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else
                                            MaterialTheme.colorScheme.surface
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .scale(scale)
                            )
                        }
                    }
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
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = LocalContentColor.current
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$currentSongIndex/$totalQueueSize",
                        style = MaterialTheme.typography.titleMedium,
                        color = LocalContentColor.current.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box {
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
                            Spacer(modifier = Modifier.size(48.dp)) // Match IconButton size
                        }
                        IconButton(onClick = { showLayoutMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Change Player Layout",
                                tint = LocalContentColor.current
                            )
                        }
                    }

                    // --- Layout Selection Dropdown Menu ---
                    DropdownMenu(
                        expanded = showLayoutMenu,
                        onDismissRequest = { showLayoutMenu = false },
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .shadow(8.dp, RoundedCornerShape(12.dp))
                            .animateContentSize()
                            .alpha(
                                animateFloatAsState(
                                    targetValue = if (showLayoutMenu) 1f else 0f,
                                    animationSpec = tween(durationMillis = 200),
                                    label = "dropdownFade"
                                ).value
                            ),
                        offset = DpOffset(x = (-12).dp, y = 8.dp)
                    ) {
                        PlayerLayout.entries.forEach { layout ->
                            var isPressed by remember { mutableStateOf(false) }
                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.95f else 1f,
                                animationSpec = tween(durationMillis = 100),
                                label = "itemScale"
                            )

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
                                        fontWeight = if (selectedLayout == layout) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    isPressed = true
                                    onLayoutSelected(layout)
                                    showLayoutMenu = false
                                },
                                modifier = Modifier
                                    .background(
                                        if (selectedLayout == layout)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else
                                            MaterialTheme.colorScheme.surface
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .scale(scale)
                            )
                        }
                    }
                }
            }
        }
    }
}