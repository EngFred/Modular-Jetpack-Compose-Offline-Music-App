package com.engfred.musicplayer.core.ui.components

import android.content.res.Configuration
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.engfred.musicplayer.core.domain.model.AudioFile

/**
 * Composable for the mini-player bar displayed at the bottom of the main screens.
 */
@OptIn(UnstableApi::class)
@Composable
fun MiniPlayer(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPlayPause: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrev: () -> Unit,
    playingAudioFile: AudioFile?,
    isPlaying: Boolean,
) {
    // Determine compact status based on window width class
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    AnimatedVisibility(
        visible = playingAudioFile != null || isPlaying,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
        modifier = modifier
    ) {
        playingAudioFile?.let { audioFile ->
            Log.d("MiniPlayer", "MiniPlayer is showing for: ${audioFile.title}")

            val cardHeight = if (isLandscape.not()) 72.dp else 88.dp // Taller on larger screens
            val horizontalCardPadding = if (isLandscape.not()) 12.dp else 24.dp // More padding on sides for wider screens
            val contentHorizontalPadding = if (isLandscape.not()) 12.dp else 20.dp // Inner padding for row content

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .padding(horizontal = horizontalCardPadding, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onClick() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp).copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(), // Fill the card, then apply internal padding within the row
                    verticalArrangement = Arrangement.Center // Vertically center content within the card
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = contentHorizontalPadding), // Apply internal padding to the row
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween // Distributes elements evenly
                    ) {
                        // Album Art or Placeholder
                        RotatingAlbumArt(
                            imageModel = audioFile.albumArtUri,
                            size = if (isLandscape.not()) 48.dp else 64.dp,
                            trackId = audioFile.id,
                            isRotating = isPlaying // rotates while playback is active
                        )
                        Spacer(modifier = Modifier.width(if (isLandscape.not()) 12.dp else 16.dp))

                        // Song Info
                        Column(
                            modifier = Modifier.weight(1f) // Takes remaining space
                        ) {
                            Text(
                                text = audioFile.title,
                                style = if (isLandscape.not()) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium, // Larger title on wider screens
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee(),
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = audioFile.artist ?: "Unknown Artist",
                                style = if (isLandscape.not()) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium, // Larger artist text on wider screens
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Playback Controls
                        // Grouping buttons in another Row to manage their spacing more easily
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Skip Previous Button
                            IconButton(onClick = { onPlayPrev() }) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipPrevious,
                                    contentDescription = "Skip to previous track",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(if (isLandscape.not()) 32.dp else 36.dp)
                                )
                            }

                            // Play/Pause Button
                            IconButton(onClick = { onPlayPause() }) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause playback" else "Play playback",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(if (isLandscape.not()) 36.dp else 40.dp) // Main action button slightly larger
                                )
                            }

                            // Skip Next Button
                            IconButton(onClick = { onPlayNext() }) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = "Skip to next track",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(if (isLandscape.not()) 32.dp else 36.dp)
                                )
                            }
                        }
                    }
                }
            }
        } ?: Log.w("MiniPlayer", "MiniPlayer not showing!!!! (playingAudioFile is null)")
    }
}