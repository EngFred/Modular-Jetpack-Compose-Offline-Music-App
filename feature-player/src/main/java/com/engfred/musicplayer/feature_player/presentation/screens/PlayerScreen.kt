package com.engfred.musicplayer.feature_player.presentation.screens

import androidx.annotation.OptIn
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerEvent
import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerViewModel
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.LaunchedEffect
import com.engfred.musicplayer.core.domain.model.repository.RepeatMode
import com.engfred.musicplayer.core.domain.model.repository.ShuffleMode
import com.engfred.musicplayer.core.util.formatDuration

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var sliderValue by remember { mutableFloatStateOf(uiState.playbackPositionMs.toFloat()) }

    LaunchedEffect(uiState.playbackPositionMs) {
        sliderValue = uiState.playbackPositionMs.toFloat()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (uiState.error != null) {
            Text(
                text = "Error: ${uiState.error}",
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        } else if (uiState.currentAudioFile == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No song selected!!",
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            AsyncImage(
                model = uiState.currentAudioFile?.albumArtUri,
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(250.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = uiState.currentAudioFile?.title ?: "Unknown Title",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = uiState.currentAudioFile?.artist ?: "Unknown Artist",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = {
                    uiState.currentAudioFile?.let {
                        if (uiState.isFavorite) {
                            viewModel.onEvent(PlayerEvent.RemoveFromFavorites(it.id))
                        } else {
                            viewModel.onEvent(PlayerEvent.AddToFavorites(it))
                        }
                    }
                }) {
                    Icon(
                        imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (uiState.isFavorite) "Remove from Favorites" else "Add to Favorites",
                        tint = if (uiState.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Slider(
                value = sliderValue,
                onValueChange = { newValue ->
                    sliderValue = newValue
                },
                onValueChangeFinished = {
                    viewModel.onEvent(PlayerEvent.SeekTo(sliderValue.toLong()))
                },
                valueRange = 0f..uiState.totalDurationMs.toFloat().coerceAtLeast(0f),
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
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val newShuffleMode = if (uiState.shuffleMode == ShuffleMode.ON) ShuffleMode.OFF else ShuffleMode.ON
                    viewModel.onEvent(PlayerEvent.SetShuffleMode(newShuffleMode))
                }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (uiState.shuffleMode == ShuffleMode.ON) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = { viewModel.onEvent(PlayerEvent.SkipToPrevious) }) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Skip Previous",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(48.dp)
                    )
                }

                IconButton(onClick = {
                    viewModel.onEvent(PlayerEvent.PlayPause)
                }) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    )
                }

                IconButton(onClick = { viewModel.onEvent(PlayerEvent.SkipToNext) }) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Skip Next",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(48.dp)
                    )
                }

                IconButton(onClick = {
                    val newRepeatMode = when (uiState.repeatMode) {
                        RepeatMode.OFF -> RepeatMode.ALL
                        RepeatMode.ALL -> RepeatMode.ONE
                        RepeatMode.ONE -> RepeatMode.OFF
                    }
                    viewModel.onEvent(PlayerEvent.SetRepeatMode(newRepeatMode))
                }) {
                    Icon(
                        imageVector = when (uiState.repeatMode) {
                            RepeatMode.OFF -> Icons.Default.Repeat
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            RepeatMode.ALL -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat Mode",
                        tint = if (uiState.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}