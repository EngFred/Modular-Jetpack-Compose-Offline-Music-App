package com.engfred.musicplayer.core.ui

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.util.MediaUtils
import com.skydoves.landscapist.coil.CoilImage

@Composable
fun AudioFileItem(
    modifier: Modifier = Modifier,
    audioFile: AudioFile,
    isCurrentPlayingAudio: Boolean,
    isAudioPlaying: Boolean,
    onClick: (AudioFile) -> Unit,
    onSwipeLeft: (AudioFile) -> Unit = {},
    onSwipeRight: (AudioFile) -> Unit = {},
    onPlayNext: (AudioFile) -> Unit = {},
    onAddToPlaylist: (AudioFile) -> Unit,
    onRemoveOrDelete: (AudioFile) -> Unit,
    isFromAutomaticPlaylist: Boolean = false,
    isFromLibrary: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current

    val scale by animateFloatAsState(
        targetValue = if (isCurrentPlayingAudio) 1.05f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "album_art_scale_animation"
    )
    val pulse by animateFloatAsState(
        targetValue = if (isCurrentPlayingAudio) 1.1f else 1f,
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
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick(audioFile) }
            .draggable(
                state = rememberDraggableState { delta ->
                    // Allow dragging left (negative delta) always
                    // Allow dragging right (positive delta) only if playing
                    dragOffset = if (delta < 0 || (delta > 0 && isAudioPlaying && isCurrentPlayingAudio)) {
                        (dragOffset + delta).coerceIn(-100f, 100f) // Allow right drag up to 100f
                    } else {
                        dragOffset // Do not update offset if trying to drag right when not playing
                    }
                },
                orientation = Orientation.Horizontal,
                onDragStopped = {
                    when {
                        dragOffset < -75f -> { // Swiped left
                            onSwipeLeft(audioFile)
                        }
                        dragOffset > 75f && isCurrentPlayingAudio -> { // Swiped right and playing
                            onSwipeRight(audioFile)
                        }
                    }
                    dragOffset = 0f // Reset drag offset after drag stops
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrentPlayingAudio) 6.dp else 3.dp,
            pressedElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = if (isCurrentPlayingAudio) {
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
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
                    .scale(if (isCurrentPlayingAudio) scale * pulse else scale)
                    .graphicsLayer { shadowElevation = if (isCurrentPlayingAudio) 8f else 0f },
                contentAlignment = Alignment.Center
            ) {
                CoilImage(
                    imageModel = { audioFile.albumArtUri },
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = "Loading icon",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(56.dp)
                        )
                    },
                    failure = {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
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
                text = MediaUtils.formatDuration(audioFile.duration), // Use MediaUtils
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Anchor for the DropdownMenu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
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
                            onPlayNext(audioFile)
                            Toast.makeText(context, "Added '${audioFile.title}' to play next.", Toast.LENGTH_SHORT).show()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.QueuePlayNext,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Playlist") },
                        onClick = {
                            onAddToPlaylist(audioFile)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Rounded.PlaylistAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )
                    if (isFromAutomaticPlaylist.not()) {
                        DropdownMenuItem(
                            text = { Text(if (isFromLibrary) "Delete song" else "Remove song") },
                            onClick = {
                                onRemoveOrDelete(audioFile)
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                    // Call the separate share function
                    DropdownMenuItem(
                        text = { Text("Share Song") },
                        onClick = {
                            MediaUtils.shareAudioFile(context, audioFile) // Use MediaUtils
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Share,
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