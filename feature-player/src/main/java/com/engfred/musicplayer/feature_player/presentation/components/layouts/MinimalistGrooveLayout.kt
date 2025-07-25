package com.engfred.musicplayer.feature_player.presentation.components.layouts

import android.net.Uri
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.engfred.musicplayer.core.domain.model.PlayerLayout
import com.engfred.musicplayer.core.domain.repository.PlaybackState
import com.engfred.musicplayer.core.domain.repository.RepeatMode
import com.engfred.musicplayer.core.domain.repository.ShuffleMode
import com.engfred.musicplayer.feature_player.presentation.components.layouts.components.SeekBarSection
import com.engfred.musicplayer.feature_player.presentation.components.layouts.components.TopBar
import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerEvent
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * The Minimalist Groove layout for the music player screen.
 * This layout features a circular album art that rotates when playing,
 * a clean progress bar, and centered controls. It's designed to be
 * responsive across different screen sizes, adapting to single-column
 * and two-pane layouts based on window width.
 *
 * @param uiState The current playback state of the player.
 * @param onEvent Callback for dispatching [PlayerEvent]s to the ViewModel.
 * @param onNavigateUp Callback to navigate up in the navigation stack.
 * @param currentSongIndex The 0-based index of the currently playing song in the queue.
 * @param totalSongsInQueue The total number of songs in the playback queue.
 * @param selectedLayout The currently selected player layout.
 * @param onLayoutSelected Callback to change the selected player layout.
 * @param windowSizeClass The current window size class from the activity.
 */
@RequiresApi(Build.VERSION_CODES.M)
@OptIn(UnstableApi::class)
@Composable
fun MinimalistGrooveLayout(
    uiState: PlaybackState,
    onEvent: (PlayerEvent) -> Unit,
    onNavigateUp: () -> Unit = {},
    currentSongIndex: Int,
    totalSongsInQueue: Int,
    selectedLayout: PlayerLayout,
    onLayoutSelected: (PlayerLayout) -> Unit,
    windowSizeClass: WindowWidthSizeClass
) {
    val view = LocalView.current

    // Progress for LinearProgressIndicator
    val progress by animateFloatAsState(
        targetValue = if (uiState.totalDurationMs > 0) uiState.playbackPositionMs.toFloat() / uiState.totalDurationMs.toFloat() else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "playbackProgress"
    )

    // --- Album Art Rotation Logic ---
    val rotationSpeedMillis = 8000L // Milliseconds for one full rotation (e.g., 8 seconds)
    var targetRotationAngle by remember { mutableFloatStateOf(0f) }

    val animatedRotation by animateFloatAsState(
        targetValue = targetRotationAngle,
        animationSpec = tween(durationMillis = rotationSpeedMillis.toInt(), easing = LinearEasing),
        label = "albumArtRotation"
    )

    LaunchedEffect(uiState.isPlaying) {
        if (uiState.isPlaying) {
            val currentAnimatedValue = animatedRotation
            val rotationsCompleted = (currentAnimatedValue / 360f).roundToInt()
            targetRotationAngle = (rotationsCompleted * 360f) + 360f

            while (true) {
                targetRotationAngle += 360f
                delay(rotationSpeedMillis)
            }
        } else {
            targetRotationAngle = animatedRotation
        }
    }
    // --- End Album Art Rotation Logic ---

    // Responsive Album Art Size - This will be the direct size for compact, or max preferred for expanded.
    val albumArtSize: Dp = when (windowSizeClass) {
        WindowWidthSizeClass.Compact -> 200.dp // Direct size for compact
        WindowWidthSizeClass.Medium -> 260.dp  // Direct size for medium
        WindowWidthSizeClass.Expanded -> 320.dp // Max preferred for expanded
        else -> 200.dp
    }

    // Responsive Spacing
    val horizontalContentPadding: Dp = when (windowSizeClass) {
        WindowWidthSizeClass.Compact -> 24.dp
        WindowWidthSizeClass.Medium -> 32.dp
        WindowWidthSizeClass.Expanded -> 48.dp
        else -> 24.dp
    }

    // Determine if we should use a two-pane layout
    val useTwoPane = windowSizeClass == WindowWidthSizeClass.Expanded

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = horizontalContentPadding, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP BAR
        TopBar(
            onNavigateUp = onNavigateUp,
            currentSongIndex = currentSongIndex + 1,
            totalQueueSize = totalSongsInQueue,
            onOpenQueue = { /* Not applicable for Minimalist Groove layout's TopBar */ },
            windowWidthSizeClass = windowSizeClass,
            selectedLayout = selectedLayout,
            onLayoutSelected = onLayoutSelected,
            isFavorite = uiState.isFavorite,
            onToggleFavorite = {
                uiState.currentAudioFile?.let {
                    if (uiState.isFavorite) {
                        onEvent(PlayerEvent.RemoveFromFavorites(it.id))
                    } else {
                        onEvent(PlayerEvent.AddToFavorites(it))
                    }
                }
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            },
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight() // Wrap height to content
                .padding(top = 16.dp) // Consistent top padding
        )

        if (useTwoPane) {
            // TWO-PANE LAYOUT for EXPANDED width
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Take available vertical space
                    .padding(vertical = 24.dp), // Add vertical padding to the row
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Pane: Album Art
                Box(
                    modifier = Modifier
                        .weight(1f) // Take half the width
                        .fillMaxHeight(), // Fill available height in the pane
                    contentAlignment = Alignment.Center
                ) {
                    AlbumArtSection(
                        albumArtUri = uiState.currentAudioFile?.albumArtUri,
                        animatedRotation = animatedRotation,
                        sizeModifier = Modifier
                            .fillMaxSize(0.8f) // Fill 80% of the available space in its Box
                            .aspectRatio(1f) // Force it to be square based on the smaller dimension
                            .sizeIn(maxHeight = albumArtSize, maxWidth = albumArtSize) // Cap its maximum size
                    )
                }

                Spacer(modifier = Modifier.width(horizontalContentPadding * 1.5f)) // Spacing between panes

                // Right Pane: Song Info, Seek Bar, Controls
                Column(
                    modifier = Modifier
                        .weight(1f) // Take other half the width
                        .fillMaxHeight() // Fill available height
                        .padding(horizontal = horizontalContentPadding / 2), // Adjust horizontal padding within the pane
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center // Center content vertically within this pane
                ) {
                    SongInfoSection(
                        title = uiState.currentAudioFile?.title,
                        artist = uiState.currentAudioFile?.artist
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    SeekBarSection(
                        sliderValue = uiState.playbackPositionMs.toFloat(),
                        totalDurationMs = uiState.totalDurationMs,
                        playbackPositionMs = uiState.playbackPositionMs,
                        isSeeking = uiState.isSeeking,
                        onSliderValueChange = { newValue ->
                            onEvent(PlayerEvent.SetSeeking(true))
                            onEvent(PlayerEvent.SeekTo(newValue.toLong()))
                        },
                        onSliderValueChangeFinished = {
                            onEvent(PlayerEvent.SetSeeking(false))
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        },
                        playerLayout = PlayerLayout.MINIMALIST_GROOVE,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    PlaybackControls(
                        uiState = uiState,
                        onEvent = onEvent,
                        view = view
                    )
                }
            }
        } else {
            // SINGLE-COLUMN LAYOUT for COMPACT and MEDIUM width
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Allow this column to take available space
                    .wrapContentHeight() // Wrap content height to ensure proper vertical distribution
                    .padding(vertical = 24.dp), // Add vertical padding to the column
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround // Distribute content vertically
            ) {
                // Album Art
                AlbumArtSection(
                    albumArtUri = uiState.currentAudioFile?.albumArtUri,
                    animatedRotation = animatedRotation,
                    sizeModifier = Modifier.size(albumArtSize) // Directly set size for compact
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Song Info
                SongInfoSection(
                    title = uiState.currentAudioFile?.title,
                    artist = uiState.currentAudioFile?.artist,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Seek Bar Section
                SeekBarSection(
                    sliderValue = uiState.playbackPositionMs.toFloat(),
                    totalDurationMs = uiState.totalDurationMs,
                    playbackPositionMs = uiState.playbackPositionMs,
                    isSeeking = uiState.isSeeking,
                    onSliderValueChange = { newValue ->
                        onEvent(PlayerEvent.SetSeeking(true))
                        onEvent(PlayerEvent.SeekTo(newValue.toLong()))
                    },
                    onSliderValueChangeFinished = {
                        onEvent(PlayerEvent.SetSeeking(false))
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    },
                    playerLayout = PlayerLayout.MINIMALIST_GROOVE,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Playback Controls
                PlaybackControls(
                    uiState = uiState,
                    onEvent = onEvent,
                    view = view,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Song Queue Indicator at the very bottom
        Text(
            text = "${currentSongIndex + 1}/${totalSongsInQueue}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier
                .padding(bottom = 16.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

/**
 * Composable for displaying the rotating album art.
 * Now takes a `sizeModifier` to apply different sizing logic based on context.
 */
@OptIn(UnstableApi::class)
@Composable
private fun AlbumArtSection(
    albumArtUri: Uri?,
    animatedRotation: Float,
    sizeModifier: Modifier // This modifier now dictates the sizing behavior
) {
    Box(
        modifier = sizeModifier // Use the passed-in sizeModifier
            .clip(CircleShape)
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .rotate(animatedRotation),
        contentAlignment = Alignment.Center
    ) {
        CoilImage(
            imageModel = { albumArtUri },
            imageOptions = ImageOptions(
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop
            ),
            modifier = Modifier.fillMaxSize(), // Image fills its perfectly square parent
            failure = {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = "No Album Art Available",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxSize(0.6f)
                )
            },
            loading = {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxSize(0.4f)
                )
            }
        )
    }
}

/**
 * Composable for displaying song title and artist.
 */
@Composable
private fun SongInfoSection(
    title: String?,
    artist: String?,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = title ?: "No Song Playing",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = artist ?: "Select a song",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Composable for playback control buttons.
 */
@RequiresApi(Build.VERSION_CODES.M)
@Composable
private fun PlaybackControls(
    uiState: PlaybackState,
    onEvent: (PlayerEvent) -> Unit,
    view: android.view.View, // Pass the LocalView here for haptic feedback
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle Button
        IconButton(onClick = {
            val newShuffleMode = if (uiState.shuffleMode == ShuffleMode.ON) ShuffleMode.OFF else ShuffleMode.ON
            onEvent(PlayerEvent.SetShuffleMode(newShuffleMode))
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = "Toggle Shuffle Mode",
                tint = if (uiState.shuffleMode == ShuffleMode.ON) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp)
            )
        }
        // Skip Previous
        IconButton(onClick = {
            onEvent(PlayerEvent.SkipToPrevious)
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Skip Previous Song",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(48.dp)
            )
        }
        // Play/Pause Button
        IconButton(
            onClick = {
                onEvent(PlayerEvent.PlayPause)
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            },
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (uiState.isPlaying) "Pause Playback" else "Resume Playback",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(56.dp)
            )
        }
        // Skip Next
        IconButton(onClick = {
            onEvent(PlayerEvent.SkipToNext)
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Skip Next Song",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(48.dp)
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
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }) {
            Icon(
                imageVector = when (uiState.repeatMode) {
                    RepeatMode.OFF -> Icons.Rounded.Repeat
                    RepeatMode.ONE -> Icons.Rounded.RepeatOne
                    RepeatMode.ALL -> Icons.Rounded.Repeat
                },
                contentDescription = "Toggle Repeat Mode",
                tint = if (uiState.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}