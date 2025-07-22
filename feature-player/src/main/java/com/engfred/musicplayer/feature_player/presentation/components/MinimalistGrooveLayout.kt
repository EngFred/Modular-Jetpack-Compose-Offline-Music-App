package com.engfred.musicplayer.feature_player.presentation.components

import androidx.annotation.OptIn
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.engfred.musicplayer.core.domain.model.repository.PlaybackState
import com.engfred.musicplayer.core.domain.model.repository.RepeatMode
import com.engfred.musicplayer.core.domain.model.repository.ShuffleMode
import com.engfred.musicplayer.core.util.formatDuration
import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerEvent

@OptIn(UnstableApi::class)
@Composable
fun MinimalistGrooveLayout(
    uiState: PlaybackState,
    onEvent: (PlayerEvent) -> Unit
) {
    val view = LocalView.current

    // Progress for LinearProgressIndicator
    val progress by animateFloatAsState(
        targetValue = if (uiState.totalDurationMs > 0) uiState.playbackPositionMs.toFloat() / uiState.totalDurationMs.toFloat() else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "playbackProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Use primary background color
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // Center content vertically
    ) {
        // Album Art (small and circular)
        AsyncImage(
            model = uiState.currentAudioFile?.albumArtUri,
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp) // Smaller size
                .clip(CircleShape) // Circular shape
                .shadow(elevation = 16.dp, shape = CircleShape, ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) // Subtle shadow
                .background(MaterialTheme.colorScheme.surfaceVariant) // Fallback background
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Song Info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = uiState.currentAudioFile?.title ?: "No Song Playing",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground, // Text color on background
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = uiState.currentAudioFile?.artist ?: "Select a song",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Progress Indicator and Timings
        Column(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp) // Thin progress bar
                    .clip(CircleShape), // Rounded ends
                color = MaterialTheme.colorScheme.primary, // Active progress color
                trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f) // Inactive track color
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDuration(uiState.playbackPositionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Text(
                    text = formatDuration(uiState.totalDurationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle Button
            IconButton(onClick = {
                val newShuffleMode = if (uiState.shuffleMode == ShuffleMode.ON) ShuffleMode.OFF else ShuffleMode.ON
                onEvent(PlayerEvent.SetShuffleMode(newShuffleMode))
                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            }) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (uiState.shuffleMode == ShuffleMode.ON) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.size(28.dp)
                )
            }

            // Skip Previous
            IconButton(onClick = {
                onEvent(PlayerEvent.SkipToPrevious)
                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            }) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Skip Previous",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Play/Pause Button
            IconButton(
                onClick = {
                    onEvent(PlayerEvent.PlayPause)
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                },
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary) // Primary color for the main action
            ) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }

            // Skip Next
            IconButton(onClick = {
                onEvent(PlayerEvent.SkipToNext)
                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            }) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Skip Next",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Repeat Button
            IconButton(onClick = {
                val newRepeatMode = when (uiState.repeatMode) {
                    RepeatMode.OFF -> RepeatMode.ALL
                    RepeatMode.ALL -> RepeatMode.ONE
                    RepeatMode.ONE -> RepeatMode.OFF
                }
                onEvent(PlayerEvent.SetRepeatMode(newRepeatMode))
                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            }) {
                Icon(
                    imageVector = when (uiState.repeatMode) {
                        RepeatMode.OFF -> Icons.Default.Repeat
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        RepeatMode.ALL -> Icons.Default.Repeat
                    },
                    contentDescription = "Repeat Mode",
                    tint = if (uiState.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Favorite Button
        IconButton(
            onClick = {
                uiState.currentAudioFile?.let {
                    if (uiState.isFavorite) {
                        onEvent(PlayerEvent.RemoveFromFavorites(it.id))
                    } else {
                        onEvent(PlayerEvent.AddToFavorites(it))
                    }
                }
                view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (uiState.isFavorite) "Remove from Favorites" else "Add to Favorites",
                tint = if (uiState.isFavorite) Color(0xFFFF5252) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}