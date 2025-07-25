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
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.core.domain.repository.RepeatMode
import com.engfred.musicplayer.core.domain.repository.ShuffleMode
import com.engfred.musicplayer.core.domain.model.PlayerLayout

/**
 * A reusable Composable for the music player's control bar.
 * It provides buttons for shuffle, skip previous, play/pause, skip next, and repeat.
 * The appearance adapts based on the selected [PlayerLayout].
 *
 * @param shuffleMode The current shuffle mode state.
 * @param isPlaying Boolean indicating if music is currently playing.
 * @param repeatMode The current repeat mode state.
 * @param onPlayPauseClick Callback for play/pause button clicks.
 * @param onSkipPreviousClick Callback for skip previous button clicks.
 * @param onSkipNextClick Callback for skip next button clicks.
 * @param onSetShuffleMode Callback to set the new shuffle mode.
 * @param onSetRepeatMode Callback to set the new repeat mode.
 * @param playerLayout The currently active player layout, used for styling adjustments.
 * @param modifier The modifier to be applied to the control bar.
 */
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
    playerLayout: PlayerLayout,
    modifier: Modifier = Modifier
) {
    if (playerLayout == PlayerLayout.IMMERSIVE_CANVAS) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(Color.Transparent), // Explicitly transparent for immersive
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaybackControlIconButton(
                icon = Icons.Rounded.Shuffle,
                contentDescription = "Toggle Shuffle Mode",
                onClick = {
                    val newMode = when (shuffleMode) {
                        ShuffleMode.OFF -> ShuffleMode.ON
                        ShuffleMode.ON -> ShuffleMode.OFF
                    }
                    onSetShuffleMode(newMode)
                },
                tint = if (shuffleMode == ShuffleMode.ON) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.7f),
                size = 28.dp
            )
            PlaybackControlIconButton(
                icon = Icons.Rounded.SkipPrevious,
                contentDescription = "Skip Previous Song",
                onClick = onSkipPreviousClick,
                tint = LocalContentColor.current,
                size = 48.dp
            )
            // Play/Pause button with distinct background and size for ImmersiveCanvas
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause Playback" else "Play Playback",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }
            PlaybackControlIconButton(
                icon = Icons.Rounded.SkipNext,
                contentDescription = "Skip Next Song",
                onClick = onSkipNextClick,
                tint = LocalContentColor.current,
                size = 48.dp
            )
            PlaybackControlIconButton(
                icon = when (repeatMode) {
                    RepeatMode.OFF -> Icons.Rounded.Repeat
                    RepeatMode.ALL -> Icons.Rounded.Repeat
                    RepeatMode.ONE -> Icons.Rounded.RepeatOne
                },
                contentDescription = "Toggle Repeat Mode",
                onClick = {
                    val newMode = when (repeatMode) {
                        RepeatMode.OFF -> RepeatMode.ALL
                        RepeatMode.ALL -> RepeatMode.ONE
                        RepeatMode.ONE -> RepeatMode.OFF
                    }
                    onSetRepeatMode(newMode)
                },
                tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.7f),
                size = 28.dp
            )
        }
    } else {
        // EtherealFlow / Minimalist Groove style (original design with background and animation)
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
                PlaybackControlIconButton(
                    icon = Icons.Rounded.Shuffle,
                    contentDescription = "Toggle Shuffle Mode",
                    onClick = {
                        val newShuffleMode = if (shuffleMode == ShuffleMode.ON) ShuffleMode.OFF else ShuffleMode.ON
                        onSetShuffleMode(newShuffleMode)
                    },
                    tint = if (shuffleMode == ShuffleMode.ON) MaterialTheme.colorScheme.secondary else LocalContentColor.current.copy(alpha = 0.6f),
                    size = 32.dp
                )

                PlaybackControlIconButton(
                    icon = Icons.Rounded.SkipPrevious,
                    contentDescription = "Skip Previous Song",
                    onClick = onSkipPreviousClick,
                    tint = LocalContentColor.current,
                    size = 44.dp
                )

                // Play/Pause button with scale animation for EtherealFlow/Minimalist Groove
                PlaybackControlIconButton(
                    icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause Playback" else "Play Playback",
                    onClick = onPlayPauseClick,
                    tint = Color.White,
                    size = 48.dp, // Icon size
                    buttonSize = 72.dp, // Outer button size
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    scaleAnimation = true,
                    isPlaying = isPlaying // Pass isPlaying for animation trigger
                )

                PlaybackControlIconButton(
                    icon = Icons.Rounded.SkipNext,
                    contentDescription = "Skip Next Song",
                    onClick = onSkipNextClick,
                    tint = LocalContentColor.current,
                    size = 44.dp
                )

                PlaybackControlIconButton(
                    icon = when (repeatMode) {
                        RepeatMode.OFF -> Icons.Rounded.Repeat
                        RepeatMode.ALL -> Icons.Rounded.Repeat
                        RepeatMode.ONE -> Icons.Rounded.RepeatOne
                    },
                    contentDescription = "Toggle Repeat Mode",
                    onClick = {
                        val newRepeatMode = when (repeatMode) {
                            RepeatMode.OFF -> RepeatMode.ALL
                            RepeatMode.ALL -> RepeatMode.ONE
                            RepeatMode.ONE -> RepeatMode.OFF
                        }
                        onSetRepeatMode(newRepeatMode)
                    },
                    tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.secondary else LocalContentColor.current.copy(alpha = 0.6f),
                    size = 32.dp
                )
            }
        }
    }
}

/**
 * A private helper Composable for consistent styling of playback control buttons.
 * Reduces duplication within the [ControlBar].
 *
 * @param icon The [ImageVector] for the icon to display.
 * @param contentDescription The content description for accessibility.
 * @param onClick The callback to be invoked when the button is clicked.
 * @param tint The color tint for the icon. Defaults to [LocalContentColor.current].
 * @param size The size of the icon itself.
 * @param buttonSize The overall size of the [IconButton] container. Defaults to [size] + padding.
 * @param backgroundColor Optional background color for the button. Defaults to [Color.Transparent].
 * @param scaleAnimation If true, applies a scale animation based on [isPlaying].
 * @param isPlaying Boolean used as a trigger for scale animation (e.g., for Play/Pause button).
 */
@Composable
private fun PlaybackControlIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color,
    size: Dp,
    buttonSize: Dp = size + 24.dp, // Default button size, allowing icon to be smaller than button
    backgroundColor: Color = Color.Transparent,
    scaleAnimation: Boolean = false,
    isPlaying: Boolean = false // Only relevant if scaleAnimation is true
) {
    val scale by animateFloatAsState(
        targetValue = if (scaleAnimation && isPlaying) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "buttonScale"
    )

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(buttonSize)
            .graphicsLayer {
                if (scaleAnimation) {
                    scaleX = scale
                    scaleY = scale
                }
            }
            .clip(CircleShape) // Always clip to circle, but background applied only if not transparent
            .background(backgroundColor)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size)
        )
    }
}