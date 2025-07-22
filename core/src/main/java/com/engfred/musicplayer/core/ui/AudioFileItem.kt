package com.engfred.musicplayer.core.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.util.formatDuration
import com.skydoves.landscapist.coil.CoilImage
import kotlinx.coroutines.launch // Import launch for coroutine scope

/**
 * @param audioFile The AudioFile domain model to display.
 * @param isPlaying Whether the audio file is currently playing, triggering album art animation.
 * @param onClick Callback when the item is clicked to initiate playback.
 * @param onSwipeToNowPlaying Callback when the item is swiped right-to-left to navigate to the now-playing screen.
 * @param onPlayNext Callback for the "Play Next" menu action (VM will still be called).
 * @param onAddToAlbum Callback for the "Add to Album" menu action.
 * @param onDelete Callback for the "Delete" menu action, customizable per screen (e.g., remove from playlist or device).
 * @param onShare Callback for the "Share" menu action.
 * @param snackbarHostState The SnackbarHostState to show immediate UI feedback.
 * @param modifier Modifier for the item, allowing customization of size or padding.
 */
@Composable
fun AudioFileItem(
    modifier: Modifier = Modifier,
    audioFile: AudioFile,
    isPlaying: Boolean = false,
    onClick: (AudioFile) -> Unit,
    onSwipeToNowPlaying: (AudioFile) -> Unit = {},
    onPlayNext: (AudioFile) -> Unit = {},
    onAddToAlbum: (AudioFile) -> Unit = {},
    onDelete: (AudioFile) -> Unit,
    onShare: (AudioFile) -> Unit = {},
    snackbarHostState: SnackbarHostState
) {
    var showMenu by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope() // Remember a coroutine scope for showing snackbar

    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1.05f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "album_art_scale_animation"
    )
    val pulse by animateFloatAsState(
        targetValue = if (isPlaying) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "album_art_pulse_animation"
    )

    LaunchedEffect(audioFile) {
        dragOffset = 0f
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .offset(x = dragOffset.dp)
            .padding(vertical = 4.dp, horizontal = 10.dp)
            .draggable(
                state = rememberDraggableState { delta ->
                    dragOffset = (dragOffset + delta).coerceIn(-100f, 0f)
                },
                orientation = Orientation.Horizontal,
                onDragStopped = {
                    if (dragOffset < -75f) {
                        onSwipeToNowPlaying(audioFile)
                    }
                    dragOffset = 0f
                }
            )
            .clickable { onClick(audioFile) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPlaying) 6.dp else 3.dp,
            pressedElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = if (isPlaying) {
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent
                            )
                        )
                    }
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .scale(if (isPlaying) scale * pulse else scale)
                    .graphicsLayer { shadowElevation = if (isPlaying) 8f else 0f },
                contentAlignment = Alignment.Center
            ) {
                CoilImage(
                    imageModel = { audioFile.albumArtUri },
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Loading icon",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(56.dp)
                        )
                    },
                    failure = {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "No album art available",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(56.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = audioFile.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = audioFile.artist ?: "Unknown Artist",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = formatDuration(audioFile.duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Anchor for the DropdownMenu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options for ${audioFile.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("Play Next") },
                        onClick = {
                            // 1. Trigger the ViewModel action (still important for player logic)
                            onPlayNext(audioFile)
                            // 2. Immediately show the Snackbar feedback
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Added '${audioFile.title}' to play next.",
                                    duration = SnackbarDuration.Short // Or Short
                                )
                            }
                            showMenu = false // Close the menu
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.QueuePlayNext,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Playlist") },
                        onClick = {
                            onAddToAlbum(audioFile)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.PlaylistAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Song") },
                        onClick = {
                            onDelete(audioFile)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share Song") },
                        onClick = {
                            onShare(audioFile)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )
                }
            }
        }
    }
}