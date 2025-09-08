package com.engfred.musicplayer.feature_player.presentation.layouts

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.annotation.RequiresApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
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
import com.engfred.musicplayer.feature_player.presentation.components.*
import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerEvent
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.M)
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
    val view = LocalView.current
    //context
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



    val useTwoPane = windowWidthSizeClass == WindowWidthSizeClass.Expanded

    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxSize().systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -20f) {
                            coroutineScope.launch { sheetState.show() }
                            showQueueBottomSheet = true
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TopBar(
                onNavigateUp = onNavigateUp,
                currentSongIndex = currentSongIndex + 1,
                totalQueueSize = totalSongsInQueue,
                onOpenQueue = { },
                windowWidthSizeClass = windowWidthSizeClass,
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

            if (useTwoPane) {
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
                            windowWidthSizeClass = windowWidthSizeClass,
                            windowHeightSizeClass = windowHeightSizeClass,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .wrapContentHeight()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    RotatingWaveAlbumArt(
                        albumArtUri = uiState.currentAudioFile?.albumArtUri,
                        isPlaying = uiState.isPlaying,
                        modifier = Modifier.size(260.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
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
//                    Spacer(modifier = Modifier.height(32.dp))
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
                        windowWidthSizeClass = windowWidthSizeClass,
                        windowHeightSizeClass = windowHeightSizeClass,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                Text(
                    text = "${currentSongIndex + 1}/${totalSongsInQueue}",
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