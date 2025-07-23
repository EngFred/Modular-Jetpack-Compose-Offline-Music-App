package com.engfred.musicplayer.feature_player.presentation.components.layouts.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic // Added for queue icon
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.core.domain.model.repository.RepeatMode
import com.engfred.musicplayer.core.domain.model.repository.ShuffleMode
import com.engfred.musicplayer.feature_player.domain.model.PlayerLayout // Import PlayerLayout

@Composable
fun ControlBar(
    shuffleMode: ShuffleMode,
    isPlaying: Boolean,
    repeatMode: RepeatMode,
    onPlayPauseClick: () -> Unit,
    onSkipPreviousClick: () -> Unit,
    onSkipNextClick: () -> Unit,
    onSetShuffleMode: (ShuffleMode) -> Unit,
    onSetRepeatMode: (RepeatMode) -> Unit,
    modifier: Modifier = Modifier,
    playerLayout: PlayerLayout,
    onOpenQueue: (() -> Unit)? = null // Optional callback for queue button (only for ImmersiveCanvas Compact)
) {
    if (playerLayout == PlayerLayout.IMMERSIVE_CANVAS) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(Color.Transparent),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val newMode = when (shuffleMode) {
                    ShuffleMode.OFF -> ShuffleMode.ON
                    ShuffleMode.ON -> ShuffleMode.OFF
                }
                onSetShuffleMode(newMode)
            }) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (shuffleMode == ShuffleMode.ON) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp)
                )
            }
            IconButton(onClick = onSkipPreviousClick) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Skip Previous",
                    tint = LocalContentColor.current,
                    modifier = Modifier.size(48.dp)
                )
            }
            // Play/Pause button with round background for ImmersiveCanvas
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier
                    .size(64.dp) // Maintain consistent size for the button itself
                    .clip(CircleShape) // Apply circular clip
                    .background(MaterialTheme.colorScheme.primary) // Apply primary color background
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onPrimary, // Icon color on primary background
                    modifier = Modifier.size(48.dp) // Icon size within the button
                )
            }
            IconButton(onClick = onSkipNextClick) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Skip Next",
                    tint = LocalContentColor.current,
                    modifier = Modifier.size(48.dp)
                )
            }
            IconButton(onClick = {
                val newMode = when (repeatMode) {
                    RepeatMode.OFF -> RepeatMode.ALL
                    RepeatMode.ALL -> RepeatMode.ONE
                    RepeatMode.ONE -> RepeatMode.OFF
                }
                onSetRepeatMode(newMode)
            }) {
                Icon(
                    when (repeatMode) {
                        RepeatMode.OFF -> Icons.Default.Repeat
                        RepeatMode.ALL -> Icons.Default.Repeat
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                    },
                    contentDescription = "Repeat",
                    tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp)
                )
            }
            // Removed queue button from here for ImmersiveCanvas
        }
    } else {
        // EtherealFlow / Default style (original design with background and animation)
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(LocalContentColor.current.copy(alpha = 0.08f))
                .padding(vertical = 12.dp, horizontal = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val newShuffleMode = if (shuffleMode == ShuffleMode.ON) ShuffleMode.OFF else ShuffleMode.ON
                    onSetShuffleMode(newShuffleMode)
                }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleMode == ShuffleMode.ON) MaterialTheme.colorScheme.secondary else LocalContentColor.current.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = onSkipPreviousClick) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Skip Previous",
                        tint = LocalContentColor.current,
                        modifier = Modifier.size(44.dp)
                    )
                }

                val playPauseScale by animateFloatAsState(
                    targetValue = if (isPlaying) 1.05f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "playPauseScale"
                )
                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer {
                            scaleX = playPauseScale
                            scaleY = playPauseScale
                        }
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                IconButton(onClick = onSkipNextClick) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Skip Next",
                        tint = LocalContentColor.current,
                        modifier = Modifier.size(44.dp)
                    )
                }

                IconButton(onClick = {
                    val newRepeatMode = when (repeatMode) {
                        RepeatMode.OFF -> RepeatMode.ALL
                        RepeatMode.ALL -> RepeatMode.ONE
                        RepeatMode.ONE -> RepeatMode.OFF
                    }
                    onSetRepeatMode(newRepeatMode)
                }) {
                    Icon(
                        imageVector = when (repeatMode) {
                            RepeatMode.OFF -> Icons.Default.Repeat
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            RepeatMode.ALL -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat Mode",
                        tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.secondary else LocalContentColor.current.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}