package com.engfred.musicplayer.feature_player.presentation.components.layouts

import androidx.annotation.OptIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.engfred.musicplayer.core.domain.model.repository.PlaybackState
import com.engfred.musicplayer.core.domain.model.repository.RepeatMode
import com.engfred.musicplayer.core.domain.model.repository.ShuffleMode
import com.engfred.musicplayer.core.util.formatDuration
import com.engfred.musicplayer.feature_player.domain.model.PlayerLayout
import com.engfred.musicplayer.feature_player.presentation.components.layouts.components.TopBar
import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerEvent
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun MinimalistGrooveLayout(
    uiState: PlaybackState,
    onEvent: (PlayerEvent) -> Unit,
    onNavigateUp: () -> Unit = {},
    currentSongIndex: Int = 3,
    totalSongsInQueue: Int = 10,
    selectedLayout: PlayerLayout, // Added for layout selection
    onLayoutSelected: (PlayerLayout) -> Unit // Added for layout selection callback
) {
    val view = LocalView.current

    // Progress for LinearProgressIndicator
    val progress by animateFloatAsState(
        targetValue = if (uiState.totalDurationMs > 0) uiState.playbackPositionMs.toFloat() / uiState.totalDurationMs.toFloat() else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "playbackProgress"
    )

    // --- Album Art Rotation Logic (FIXED AGAIN!) ---
    val rotationSpeedMillis = 8000L // Milliseconds for one full rotation (e.g., 8 seconds)

    // This state will hold the target angle for the rotation.
    // When playing, it will increment by 360.
    // When paused, it will hold the current animated value.
    var targetRotationAngle by remember { mutableFloatStateOf(0f) }

    // This is the actual animated value that Modifier.rotate uses.
    // Declare it *before* the LaunchedEffect that might reference it.
    val animatedRotation by animateFloatAsState(
        targetValue = targetRotationAngle,
        animationSpec = tween(durationMillis = rotationSpeedMillis.toInt(), easing = LinearEasing),
        label = "albumArtRotation"
    )

    LaunchedEffect(uiState.isPlaying) {
        if (uiState.isPlaying) {
            // When playing, we want to continuously rotate.
            // Calculate how much more rotation is needed to reach the next full 360-degree cycle
            // from the current animated position.
            val currentVisualAngle = animatedRotation % 360f // Get current angle within 0-360 range
            val rotationsCompleted = (animatedRotation / 360f).roundToInt() // How many full rotations have been completed

            // If currentVisualAngle is not exactly 0, start the next target from the current spot + a full 360.
            // This ensures a smooth transition from a paused state.
            val startAngleForNextCycle = if (currentVisualAngle != 0f) {
                // If it's not at 0, calculate the next 360-degree mark from the current animated value
                // We want to ensure it's *at least* 360 degrees ahead of the current visual value to ensure movement
                (rotationsCompleted * 360f) + 360f
            } else {
                // If it's exactly 0 (or just started), set initial target to 360
                360f
            }

            // Set the initial target to ensure smooth pickup
            targetRotationAngle = startAngleForNextCycle

            // Continuously update the target angle to keep the animation running
            while (true) {
                targetRotationAngle += 360f // Add another full rotation
                delay(rotationSpeedMillis) // Wait for one rotation duration
            }
        } else {
            // When paused, stop the target from changing and hold the current visual rotation.
            // `animateFloatAsState` will naturally animate to and hold this value.
            targetRotationAngle = animatedRotation
        }
    }
    // --- End Album Art Rotation Logic ---

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP BAR
        TopBar(
            onNavigateUp = onNavigateUp,
            currentSongIndex = currentSongIndex + 1,
            totalQueueSize = totalSongsInQueue,
            onOpenQueue = { /* No-op: Queue not interactive in top bar for this layout */ },
            windowWidthSizeClass = WindowWidthSizeClass.Compact, // Default to Compact as layout doesn't use window size
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
                view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            },
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
        )

        // --- Main Content (Wrapped in its own Column for centering) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Album Art (with rotation modifier)
            CoilImage(
                imageModel = { uiState.currentAudioFile?.albumArtUri },
                imageOptions = ImageOptions(
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop
                ),
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .shadow(elevation = 16.dp, shape = CircleShape, ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .rotate(animatedRotation),
                failure = {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = "No Album Art",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxSize(0.6f)
                    )
                },
                loading = {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = "No Album Art",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxSize(0.6f)
                    )
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Song Info (unchanged)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = uiState.currentAudioFile?.title ?: "No Song Playing",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
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

            // Progress Indicator and Timings (unchanged)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
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

            // Playback Controls (unchanged)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
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
                        .background(MaterialTheme.colorScheme.primary)
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
        }
        // --- End of Main Content Column ---

        // Song Queue Indicator at the very bottom (unchanged)
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