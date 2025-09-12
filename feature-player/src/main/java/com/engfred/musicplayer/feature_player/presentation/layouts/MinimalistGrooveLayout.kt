package com.engfred.musicplayer.feature_player.presentation.layouts

import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.model.PlayerLayout
import com.engfred.musicplayer.core.domain.repository.PlaybackState
import com.engfred.musicplayer.core.domain.repository.RepeatMode
import com.engfred.musicplayer.core.domain.repository.ShuffleMode
import com.engfred.musicplayer.core.util.shareAudioFile
import com.engfred.musicplayer.feature_player.presentation.components.ControlBar
import com.engfred.musicplayer.feature_player.presentation.components.QueueBottomSheet
import com.engfred.musicplayer.feature_player.presentation.components.RotatingWaveAlbumArt
import com.engfred.musicplayer.feature_player.presentation.components.SeekBarSection
import com.engfred.musicplayer.feature_player.presentation.components.TopBar
import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerEvent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalistGrooveLayout(
    uiState: PlaybackState,
    onEvent: (PlayerEvent) -> Unit,
    onNavigateUp: () -> Unit,
    currentSongIndex: Int,
    totalSongsInQueue: Int,
    selectedLayout: PlayerLayout,
    onLayoutSelected: (PlayerLayout) -> Unit,
    windowWidthSizeClass: WindowWidthSizeClass,
    windowHeightSizeClass: WindowHeightSizeClass,
    playingQueue: List<AudioFile>,
    onPlayQueueItem: (AudioFile) -> Unit,
    onRemoveQueueItem: (AudioFile) -> Unit,
    repeatMode: RepeatMode,
    shuffleMode: ShuffleMode
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val view = LocalView.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showQueueBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showQueueBottomSheet) {
        QueueBottomSheet(
            onDismissRequest = { showQueueBottomSheet = false },
            sheetState = sheetState,
            playingQueue = playingQueue,
            onPlayQueueItem = onPlayQueueItem,
            onRemoveQueueItem = onRemoveQueueItem,
            playingAudio = uiState.currentAudioFile,
        )
    }
    var verticalDragCumulative by remember { mutableFloatStateOf(0f) }
    val dragThreshold = 100f // Adjust as needed for sensitivity
    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxSize().systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (verticalDragCumulative < -dragThreshold) {
                                // Drag up to open queue
                                coroutineScope.launch { sheetState.show() }
                                showQueueBottomSheet = true
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            } else if (verticalDragCumulative > dragThreshold) {
                                // Drag down to exit the screen
                                onNavigateUp()
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                            verticalDragCumulative = 0f
                        },
                        onVerticalDrag = { _, dragAmount ->
                            verticalDragCumulative += dragAmount
                            true
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TopBar(
                onNavigateUp = onNavigateUp,
                currentSongIndex = currentSongIndex + 1,
                totalQueueSize = totalSongsInQueue,
                onOpenQueue = { },
                windowWidthSizeClass = WindowWidthSizeClass.Compact, // Unused but passed
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
                modifier = Modifier.fillMaxWidth(),
                onShareAudio = {
                    uiState.currentAudioFile?.let {
                        shareAudioFile(context, it)
                    }
                }
            )
            if (isLandscape) {
                // Landscape layout (organized based on previous two-pane implementation)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RotatingWaveAlbumArt(
                        albumArtUri = uiState.currentAudioFile?.albumArtUri,
                        isPlaying = uiState.isPlaying,
                        modifier = Modifier
                            .aspectRatio(1f)
                    )
                    Spacer(modifier = Modifier.width(76.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
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
                        Spacer(modifier = Modifier.height(8.dp))
                        ControlBar(
                            shuffleMode = shuffleMode,
                            isPlaying = uiState.isPlaying,
                            repeatMode = repeatMode,
                            onPlayPauseClick = {
                                onEvent(PlayerEvent.PlayPause)
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            },
                            onSkipPreviousClick = {
                                onEvent(PlayerEvent.SkipToPrevious)
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            },
                            onSkipNextClick = {
                                onEvent(PlayerEvent.SkipToNext)
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            },
                            onSetShuffleMode = { newMode -> onEvent(PlayerEvent.SetShuffleMode(newMode)) },
                            onSetRepeatMode = { newMode -> onEvent(PlayerEvent.SetRepeatMode(newMode)) },
                            playerLayout = PlayerLayout.MINIMALIST_GROOVE,
                            windowWidthSizeClass = WindowWidthSizeClass.Expanded, // Unused but passed
                            windowHeightSizeClass = WindowHeightSizeClass.Medium, // Unused but passed
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                // Portrait layout
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceAround
                        ) {
                            RotatingWaveAlbumArt(
                                albumArtUri = uiState.currentAudioFile?.albumArtUri,
                                isPlaying = uiState.isPlaying,
                                modifier = Modifier.size(260.dp)
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                            SongInfoSection(
                                title = uiState.currentAudioFile?.title,
                                artist = uiState.currentAudioFile?.artist,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            SeekBarSection(
                                sliderValue = uiState.playbackPositionMs.toFloat(),
                                totalDurationMs = uiState.totalDurationMs,
                                playbackPositionMs = uiState.playbackPositionMs,
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
                            ControlBar(
                                shuffleMode = shuffleMode,
                                isPlaying = uiState.isPlaying,
                                repeatMode = repeatMode,
                                onPlayPauseClick = {
                                    onEvent(PlayerEvent.PlayPause)
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                },
                                onSkipPreviousClick = {
                                    onEvent(PlayerEvent.SkipToPrevious)
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                },
                                onSkipNextClick = {
                                    onEvent(PlayerEvent.SkipToNext)
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                },
                                onSetShuffleMode = { newMode -> onEvent(PlayerEvent.SetShuffleMode(newMode)) },
                                onSetRepeatMode = { newMode -> onEvent(PlayerEvent.SetRepeatMode(newMode)) },
                                playerLayout = PlayerLayout.MINIMALIST_GROOVE,
                                windowWidthSizeClass = WindowWidthSizeClass.Compact, // Unused but passed
                                windowHeightSizeClass = WindowHeightSizeClass.Medium, // Unused but passed
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 13.dp)
                    .align(Alignment.CenterHorizontally)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                coroutineScope.launch { sheetState.show() }
                                showQueueBottomSheet = true
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                        )
                    }
            ) {
                val currentSongI = currentSongIndex + 1
                val currentSongNumText = if (currentSongI > totalSongsInQueue) "" else currentSongI.toString()
                Text(
                    text = "${currentSongNumText}/${totalSongsInQueue}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Open Queue",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
    }
}
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
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee()
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