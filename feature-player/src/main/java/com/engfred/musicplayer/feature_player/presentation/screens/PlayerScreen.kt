package com.engfred.musicplayer.feature_player.presentation.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.engfred.musicplayer.feature_player.domain.model.PlaybackState
import com.engfred.musicplayer.feature_player.domain.model.RepeatMode
import com.engfred.musicplayer.feature_player.domain.model.ShuffleMode
import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerEvent
import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerViewModel
import java.util.concurrent.TimeUnit
import kotlin.time.DurationUnit // Import for DurationUnit

/**
 * Composable screen for displaying the music player controls and current song information.
 */
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // uiState.error is the screen-level error
        if (uiState.error != null) {
            Text(
                text = "Error: ${uiState.error}",
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        } else if (uiState.playbackState.currentAudioFile == null) { // Access nested playbackState
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // uiState.isLoading is the screen-level loading
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    Text(
                        text = "No song selected or loading...",
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Album Art
            AsyncImage(
                model = uiState.playbackState.currentAudioFile?.albumArtUri, // Access nested playbackState
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(250.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant) // Placeholder background
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Song Title
            Text(
                text = uiState.playbackState.currentAudioFile?.title ?: "Unknown Title", // Access nested playbackState
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Artist Name
            Text(
                text = uiState.playbackState.currentAudioFile?.artist ?: "Unknown Artist", // Access nested playbackState
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Seek Bar
            Slider(
                value = uiState.playbackState.playbackPositionMs.toFloat(), // Access nested playbackState
                onValueChange = { newValue ->
                    viewModel.onEvent(PlayerEvent.SeekTo(newValue.toLong()))
                },
                valueRange = 0f..uiState.playbackState.totalDurationMs.toFloat().coerceAtLeast(0f), // Access nested playbackState
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Using currentPlaybackTime and totalPlaybackDuration from nested playbackState
                Text(
                    text = uiState.playbackState.currentPlaybackTime.toComponents { minutes, seconds, _, _ ->
                        String.format("%02d:%02d", minutes, seconds)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Text(
                    text = uiState.playbackState.totalPlaybackDuration.toComponents { minutes, seconds, _, _ ->
                        String.format("%02d:%02d", minutes, seconds)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                IconButton(onClick = {
                    val newShuffleMode = if (uiState.playbackState.shuffleMode == ShuffleMode.ON) ShuffleMode.OFF else ShuffleMode.ON // Access nested playbackState
                    viewModel.onEvent(PlayerEvent.SetShuffleMode(newShuffleMode))
                }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (uiState.playbackState.shuffleMode == ShuffleMode.ON) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground, // Access nested playbackState
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Previous Button
                IconButton(onClick = { viewModel.onEvent(PlayerEvent.SkipToPrevious) }) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Skip Previous",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(48.dp)
                    )
                }

                // Play/Pause Button
                IconButton(onClick = { viewModel.onEvent(PlayerEvent.PlayPause) }) {
                    Icon(
                        imageVector = if (uiState.playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, // Access nested playbackState
                        contentDescription = if (uiState.playbackState.isPlaying) "Pause" else "Play", // Access nested playbackState
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    )
                }

                // Next Button
                IconButton(onClick = { viewModel.onEvent(PlayerEvent.SkipToNext) }) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Skip Next",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(48.dp)
                    )
                }

                // Repeat Button
                IconButton(onClick = {
                    val newRepeatMode = when (uiState.playbackState.repeatMode) { // Access nested playbackState
                        RepeatMode.OFF -> RepeatMode.ALL
                        RepeatMode.ALL -> RepeatMode.ONE
                        RepeatMode.ONE -> RepeatMode.OFF
                    }
                    viewModel.onEvent(PlayerEvent.SetRepeatMode(newRepeatMode))
                }) {
                    Icon(
                        imageVector = when (uiState.playbackState.repeatMode) { // Access nested playbackState
                            RepeatMode.OFF -> Icons.Default.Repeat
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            RepeatMode.ALL -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat Mode",
                        tint = if (uiState.playbackState.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground, // Access nested playbackState
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}
