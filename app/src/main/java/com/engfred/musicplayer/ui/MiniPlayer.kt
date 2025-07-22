package com.engfred.musicplayer.ui

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerEvent
import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerViewModel

/**
 * Composable for the mini-player bar displayed at the bottom of the main screens.
 */
@OptIn(UnstableApi::class)
@Composable
fun MiniPlayer(
    onMiniPlayerClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    AnimatedVisibility(
        visible = uiState.currentAudioFile != null,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
        modifier = modifier
    ) {
        uiState.currentAudioFile?.let { audioFile ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMiniPlayerClick(audioFile.uri.toString()) }
                    .height(72.dp)
                    .padding(horizontal = 12.dp, vertical = 4.dp), // Reduced padding
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp).copy(alpha = 0.95f) // Slightly less transparent for better contrast
                ),
                shape = RoundedCornerShape(16.dp), // Slightly less rounded for a tighter feel
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp) // Reduced elevation slightly
            ) {
                Column{
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp), // Adjusted padding inside row
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Album Art or Placeholder
                        if (audioFile.albumArtUri != null) {
                            AsyncImage(
                                model = audioFile.albumArtUri,
                                contentDescription = "Album Art for ${audioFile.title}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp) // <<< SMALLER album art
                                    .clip(RoundedCornerShape(8.dp)) // Smaller rounded corners
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Album,
                                contentDescription = "No Album Art for ${audioFile.title}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(48.dp) // <<< SMALLER placeholder icon
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp)) // Adjusted spacing

                        // Song Info
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = audioFile.title,
                                style = MaterialTheme.typography.titleSmall, // <<< SMALLER title text
                                fontWeight = FontWeight.Bold, // Still bold for readability
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = audioFile.artist ?: "Unknown Artist",
                                style = MaterialTheme.typography.bodySmall, // <<< SMALLER artist text
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Play/Pause Button
                        IconButton(onClick = { viewModel.onEvent(PlayerEvent.PlayPause) }) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (uiState.isPlaying) "Pause playback" else "Play playback",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp) // <<< SMALLER button
                            )
                        }

                        // Skip Next Button
                        IconButton(onClick = { viewModel.onEvent(PlayerEvent.SkipToNext) }) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Skip to next track",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp) // <<< SMALLER button
                            )
                        }
                    }
                }
            }
        }
    }
}