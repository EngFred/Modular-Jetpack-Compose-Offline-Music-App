package com.engfred.musicplayer.feature_player.presentation.components.layouts.components

import android.app.Activity
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.core.domain.model.PlayerLayout
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.ui.semantics.customActions

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
    val context = LocalContext.current
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Function to toggle device orientation
    fun toggleOrientation() {
        val activity = context as? Activity
        if (activity != null) {
            try {
                activity.requestedOrientation = if (isLandscape) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to change orientation", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Orientation change not supported", Toast.LENGTH_SHORT).show()
        }
    }

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
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Navigate up",
                        tint = LocalContentColor.current
                    )
                }

                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.titleMedium,
                    color = LocalContentColor.current.copy(alpha = 0.8f),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(8.dp))
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
                    // Rotation icon
                    IconButton(
                        onClick = { toggleOrientation() },
                        modifier = Modifier.semantics {
                            customActions = listOf(
                                CustomAccessibilityAction(
                                    label = if (isLandscape) "Switch to portrait" else "Switch to landscape",
                                    action = {
                                        toggleOrientation()
                                        true
                                    }
                                )
                            )
                        }
                    ) {
                        Icon(
                            Icons.Rounded.ScreenRotation,
                            contentDescription = if (isLandscape) "Switch to portrait" else "Switch to landscape",
                            tint = LocalContentColor.current,
                            modifier = Modifier.rotate(if (isLandscape) 90f else 0f)
                        )
                    }
                    IconButton(onClick = { showLayoutMenu = true }) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "Change Player Layout",
                            tint = LocalContentColor.current
                        )
                    }
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
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Navigate up",
                        tint = LocalContentColor.current
                    )
                }

                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "${currentSongIndex + 1}/$totalQueueSize",
                    style = MaterialTheme.typography.titleMedium,
                    color = LocalContentColor.current.copy(alpha = 0.8f),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rotation icon
                    IconButton(
                        onClick = { toggleOrientation() },
                        modifier = Modifier.semantics {
                            customActions = listOf(
                                CustomAccessibilityAction(
                                    label = if (isLandscape) "Switch to portrait" else "Switch to landscape",
                                    action = {
                                        toggleOrientation()
                                        true
                                    }
                                )
                            )
                        }
                    ) {
                        Icon(
                            Icons.Rounded.ScreenRotation,
                            contentDescription = if (isLandscape) "Switch to portrait" else "Switch to landscape",
                            tint = LocalContentColor.current,
                            modifier = Modifier.rotate(if (isLandscape) 90f else 0f)
                        )
                    }
                    IconButton(onClick = { showLayoutMenu = true }) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "Change Player Layout",
                            tint = LocalContentColor.current
                        )
                    }
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
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Navigate up",
                        tint = LocalContentColor.current
                    )
                }

                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "${currentSongIndex + 1}/$totalQueueSize",
                    style = MaterialTheme.typography.titleMedium,
                    color = LocalContentColor.current.copy(alpha = 0.8f),
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(8.dp))

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
                        Spacer(modifier = Modifier.size(48.dp)) // Matches IconButton size
                    }
                    // Rotation icon
                    IconButton(
                        onClick = { toggleOrientation() },
                        modifier = Modifier.semantics {
                            customActions = listOf(
                                CustomAccessibilityAction(
                                    label = if (isLandscape) "Switch to portrait" else "Switch to landscape",
                                    action = {
                                        toggleOrientation()
                                        true
                                    }
                                )
                            )
                        }
                    ) {
                        Icon(
                            Icons.Rounded.ScreenRotation,
                            contentDescription = if (isLandscape) "Switch to portrait" else "Switch to landscape",
                            tint = LocalContentColor.current,
                            modifier = Modifier.rotate(if (isLandscape) 90f else 0f)
                        )
                    }
                    IconButton(onClick = { showLayoutMenu = true }) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "Change Player Layout",
                            tint = LocalContentColor.current
                        )
                    }
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
 * Reusable Composable for the player layout selection dropdown menu, styled like WhatsApp.
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
            .animateContentSize(animationSpec = tween(durationMillis = 200))
            .alpha(
                animateFloatAsState(
                    targetValue = if (expanded) 1f else 0f,
                    animationSpec = tween(durationMillis = 200),
                    label = "dropdownFade"
                ).value
            ),
        offset = DpOffset(x = (-8).dp, y = 8.dp) // Aligns menu to the right of MoreVert icon
    ) {
        PlayerLayout.entries.forEachIndexed { index, layout ->
            // Menu item with ripple effect and highlight for selected item
            Box(
                modifier = Modifier
                    .background(
                        if (selectedLayout == layout)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else
                            Color.Transparent
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null // Ripple is handled by MaterialTheme
                    ) {
                        onLayoutSelected(layout)
                        onDismissRequest()
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = when (layout) {
                        PlayerLayout.ETHEREAL_FLOW -> "Ethereal Flow"
                        PlayerLayout.IMMERSIVE_CANVAS -> "Immersive Canvas"
                        PlayerLayout.MINIMALIST_GROOVE -> "Minimalist Groove"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (selectedLayout == layout) FontWeight.Medium else FontWeight.Normal
                )
            }
            // Add divider between items, except for the last one
            if (index < PlayerLayout.entries.size - 1) {
                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}