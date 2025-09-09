package com.engfred.musicplayer.core.ui

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.util.MediaUtils
import com.skydoves.landscapist.coil.CoilImage
import androidx.compose.ui.zIndex
import kotlinx.coroutines.isActive

/**
 * A single audio file row styled like a chat list item (no card).
 *
 * - album art is circular
 * - rotates continuously when item is current AND playing
 * - pauses in place when current but paused
 * - resets to 0° when no longer the current audio
 * - play count displayed as a small circular badge overlapping the top-left edge
 */
@Composable
fun AudioFileItem(
    modifier: Modifier = Modifier,
    audioFile: AudioFile,
    isCurrentPlayingAudio: Boolean,
    isAudioPlaying: Boolean,
    onClick: (AudioFile) -> Unit,
    onPlayNext: (AudioFile) -> Unit = {},
    onAddToPlaylist: (AudioFile) -> Unit,
    onRemoveOrDelete: (AudioFile) -> Unit,
    isFromAutomaticPlaylist: Boolean = false,
    isFromLibrary: Boolean = false,
    playCount: Int? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Use an Animatable so we can (a) perform continuous linear rotation, and
    // (b) preserve the rotation value when paused so it resumes from that angle.
    val rotationAnim = remember { Animatable(0f) }

    // Start / stop the continuous rotation based on playback state.
    LaunchedEffect(isCurrentPlayingAudio, isAudioPlaying) {
        // If this item is the currently playing audio and playback is active -> rotate.
        if (isCurrentPlayingAudio && isAudioPlaying) {
            // Keep animating by +360 repeatedly. Using LinearEasing avoids slow-in/slow-out pauses.
            while (isActive) {
                val target = rotationAnim.value + 360f
                rotationAnim.animateTo(
                    targetValue = target,
                    animationSpec = tween(durationMillis = 4000, easing = LinearEasing)
                )
                // Loop continues immediately to the next +360 — no extra delay or easing pause.
            }
        } else {
            // If the item is not current, reset to 0 degrees (like your original behavior).
            // If it's current but paused, we DO NOT reset so it "pauses" at the current angle.
            if (!isCurrentPlayingAudio) {
                rotationAnim.snapTo(0f)
            }
            // otherwise (isCurrentPlayingAudio && !isAudioPlaying) -> do nothing (preserve angle)
        }
    }

    // Keep rotation value bounded for display and pass to graphicsLayer.
    val rotationDegrees = if (isCurrentPlayingAudio) (rotationAnim.value % 360f) else 0f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(audioFile) }
            .padding(top = 4.dp, bottom = 4.dp, start = 15.dp, end = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Slightly larger outer box so the badge can overlap while remaining inside the row's hit area.
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            // Inner clipped box: the album art which rotates when playing.
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .graphicsLayer { rotationZ = rotationDegrees }
            ) {
                CoilImage(
                    imageModel = { audioFile.albumArtUri },
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = "Loading icon",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    failure = {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = "No album art available",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                )
            }

            // Play count badge: positioned slightly down-right relative to the album art's top-left,
            // so it visually overlaps the edge (half-in / half-out). It's clickable and placed above the art.
            if (playCount != null) {
                val badgeSize = 20.dp
                Box(
                    modifier = Modifier
                        // align near the top-start of the outer box
                        .align(Alignment.TopStart)
                        // move a little right and down so the badge sits half-over the album art edge
                        .offset(x = 0.dp, y = 3.dp)
                        .size(badgeSize)
                        .zIndex(1f) // ensure above the album art
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        // make the badge itself clickable (delegates to row click for now)
                        .clickable { onClick(audioFile) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = playCount.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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
            val artistText = audioFile.artist ?: "Unknown Artist"
            Text(
                text = artistText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = MediaUtils.formatDuration(audioFile.duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            if (isCurrentPlayingAudio && isAudioPlaying) {
                VisualizerBars()
            }
        }

        // Dropdown menu
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                DropdownMenuItem(
                    text = { Text("Play Next") },
                    onClick = {
                        onPlayNext(audioFile)
                        Toast.makeText(
                            context,
                            "Added '${audioFile.title}' to play next.",
                            Toast.LENGTH_SHORT
                        ).show()
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
                if (!isFromAutomaticPlaylist) {
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
                DropdownMenuItem(
                    text = { Text("Share Song") },
                    onClick = {
                        MediaUtils.shareAudioFile(context, audioFile)
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

@Composable
private fun VisualizerBars() {
    val transition = rememberInfiniteTransition(label = "visualizer")
    val heights = List(4) { i ->
        transition.animateFloat(
            initialValue = 4f,
            targetValue = 12f + (i * 4),
            animationSpec = infiniteRepeatable(
                animation = tween(400 + (i * 100)),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar$i"
        )
    }

    Row(
        modifier = Modifier
            .padding(top = 2.dp)
            .height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEach { anim ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(anim.value.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
