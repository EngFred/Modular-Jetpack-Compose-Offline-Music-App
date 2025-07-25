package com.engfred.musicplayer.feature_player.presentation.components.layouts

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.repository.PlaybackState
import com.engfred.musicplayer.core.domain.model.PlayerLayout
import com.engfred.musicplayer.feature_player.presentation.components.layouts.components.AlbumArtDisplay
import com.engfred.musicplayer.feature_player.presentation.components.layouts.components.ControlBar
import com.engfred.musicplayer.feature_player.presentation.components.layouts.components.FavoriteButton
import com.engfred.musicplayer.feature_player.presentation.components.layouts.components.PlayingQueueSection
import com.engfred.musicplayer.feature_player.presentation.components.layouts.components.QueueBottomSheet
import com.engfred.musicplayer.feature_player.presentation.components.layouts.components.SeekBarSection
import com.engfred.musicplayer.feature_player.presentation.components.layouts.components.TopBar
import com.engfred.musicplayer.feature_player.presentation.components.layouts.components.TrackInfo
import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerEvent
import com.engfred.musicplayer.feature_player.utils.getDynamicGradientColors
import kotlinx.coroutines.launch

/**
 * The Ethereal Flow layout for the music player screen.
 * Adapts its structure and behavior based on the [WindowWidthSizeClass].
 * Provides dynamic background gradients, haptic feedback, and gesture controls.
 *
 * @param uiState The current playback state of the player.
 * @param onEvent Callback for dispatching [PlayerEvent]s to the ViewModel.
 * @param onNavigateUp Callback to navigate up in the navigation stack.
 * @param playingQueue The list of songs in the current playback queue.
 * @param currentSongIndex The index of the currently playing song in the queue.
 * @param onPlayQueueItem Callback to play a specific item from the queue.
 * @param onRemoveQueueItem Callback to remove an item from the queue (used in Expanded layout).
 * @param windowSizeClass The current window size class (Compact, Medium, Expanded).
 * @param selectedLayout The currently selected player layout.
 * @param onLayoutSelected Callback to change the selected player layout.
 * @param playingAudio The currently playing [AudioFile].
 */
@RequiresApi(Build.VERSION_CODES.M)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EtherealFlowLayout(
    uiState: PlaybackState,
    onEvent: (PlayerEvent) -> Unit,
    onNavigateUp: () -> Unit,
    playingQueue: List<AudioFile>,
    currentSongIndex: Int,
    onPlayQueueItem: (AudioFile) -> Unit,
    onRemoveQueueItem: (AudioFile) -> Unit = {},
    windowSizeClass: WindowWidthSizeClass,
    selectedLayout: PlayerLayout,
    onLayoutSelected: (PlayerLayout) -> Unit,
    playingAudio: AudioFile?
) {
    // Assuming playbackPositionMs exists in PlaybackState.
    var sliderValue by remember { mutableFloatStateOf(uiState.playbackPositionMs.toFloat()) }
    val view = LocalView.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val gradientColors by produceState(
        initialValue = listOf(Color(0xFF1E1E1E), Color(0xFF333333)),
        uiState.currentAudioFile?.albumArtUri
    ) {
        val uri = uiState.currentAudioFile?.albumArtUri
        value = getDynamicGradientColors(context, uri?.toString())
    }

    LaunchedEffect(uiState.playbackPositionMs, uiState.isSeeking) {
        if (!uiState.isSeeking) {
            sliderValue = uiState.playbackPositionMs.toFloat()
        }
    }

    val dynamicContentColor by remember(gradientColors) {
        val topGradientColor = gradientColors.firstOrNull() ?: Color.Black
        val targetLuminance = topGradientColor.luminance()
        val chosenColor = if (targetLuminance > 0.5f) {
            Color.Black
        } else {
            Color.White
        }
        mutableStateOf(chosenColor)
    }

    var showQueueBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showQueueBottomSheet && windowSizeClass != WindowWidthSizeClass.Expanded) {
        QueueBottomSheet(
            onDismissRequest = { showQueueBottomSheet = false },
            sheetState = sheetState,
            playingQueue = playingQueue,
            onPlayQueueItem = onPlayQueueItem,
            onRemoveQueueItem = onRemoveQueueItem,
            playingAudio = playingAudio
        )
    }

    CompositionLocalProvider(LocalContentColor provides dynamicContentColor) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(gradientColors))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            uiState.currentAudioFile?.let {
                                if (uiState.isFavorite) {
                                    onEvent(PlayerEvent.RemoveFromFavorites(it.id))
                                } else {
                                    onEvent(PlayerEvent.AddToFavorites(it))
                                }
                            }
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        }
                    )
                }
        ) {
            when (windowSizeClass) {
                WindowWidthSizeClass.Compact -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 32.dp)
                            .semantics {
                                // CORRECT WAY to add custom accessibility actions
                                customActions = listOf(
                                    CustomAccessibilityAction(
                                        label = "Skip to previous song",
                                        action = {
                                            onEvent(PlayerEvent.SkipToPrevious)
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            true
                                        }
                                    ),
                                    CustomAccessibilityAction(
                                        label = "Skip to next song",
                                        action = {
                                            onEvent(PlayerEvent.SkipToNext)
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            true
                                        }
                                    )
                                )
                            }
                            .pointerInput(Unit) {
                                var dragAmountCumulative = 0f
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        val dragThreshold = 100f
                                        if (dragAmountCumulative > dragThreshold) {
                                            onEvent(PlayerEvent.SkipToPrevious)
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        } else if (dragAmountCumulative < -dragThreshold) {
                                            onEvent(PlayerEvent.SkipToNext)
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        }
                                        dragAmountCumulative = 0f
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        dragAmountCumulative += dragAmount
                                    }
                                )
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        TopBar(
                            onNavigateUp = onNavigateUp,
                            currentSongIndex = currentSongIndex,
                            totalQueueSize = playingQueue.size,
                            onOpenQueue = {
                                coroutineScope.launch { sheetState.show() }
                                showQueueBottomSheet = true
                            },
                            windowWidthSizeClass = windowSizeClass,
                            selectedLayout = selectedLayout,
                            onLayoutSelected = onLayoutSelected
                        )

                        AlbumArtDisplay(
                            albumArtUri = uiState.currentAudioFile?.albumArtUri,
                            isPlaying = uiState.isPlaying,
                            windowWidthSizeClass = windowSizeClass,
                            playerLayout = PlayerLayout.ETHEREAL_FLOW
                        )

                        TrackInfo(
                            title = uiState.currentAudioFile?.title,
                            artist = uiState.currentAudioFile?.artist,
                            playerLayout = PlayerLayout.ETHEREAL_FLOW
                        )

                        FavoriteButton(
                            isFavorite = uiState.isFavorite,
                            onToggleFavorite = {
                                uiState.currentAudioFile?.let {
                                    if (uiState.isFavorite) {
                                        onEvent(PlayerEvent.RemoveFromFavorites(it.id))
                                    } else {
                                        onEvent(PlayerEvent.AddToFavorites(it))
                                    }
                                }
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            }
                        )

                        SeekBarSection(
                            sliderValue = sliderValue,
                            totalDurationMs = uiState.totalDurationMs,
                            playbackPositionMs = uiState.playbackPositionMs,
                            isSeeking = uiState.isSeeking,
                            onSliderValueChange = { newValue ->
                                sliderValue = newValue
                                if (!uiState.isSeeking) onEvent(PlayerEvent.SetSeeking(true))
                            },
                            onSliderValueChangeFinished = {
                                onEvent(PlayerEvent.SeekTo(sliderValue.toLong()))
                                onEvent(PlayerEvent.SetSeeking(false))
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            },
                            playerLayout = PlayerLayout.ETHEREAL_FLOW
                        )

                        ControlBar(
                            shuffleMode = uiState.shuffleMode,
                            isPlaying = uiState.isPlaying,
                            repeatMode = uiState.repeatMode,
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
                            playerLayout = PlayerLayout.ETHEREAL_FLOW
                        )
                    }
                }

                WindowWidthSizeClass.Medium -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 24.dp)
                            .semantics {
                                // CORRECT WAY to add custom accessibility actions
                                this.customActions = listOf(
                                    CustomAccessibilityAction(
                                        label = "Skip to previous song",
                                        action = {
                                            onEvent(PlayerEvent.SkipToPrevious)
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            true
                                        }
                                    ),
                                    CustomAccessibilityAction(
                                        label = "Skip to next song",
                                        action = {
                                            onEvent(PlayerEvent.SkipToNext)
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            true
                                        }
                                    )
                                )
                            }
                            .pointerInput(Unit) {
                                var dragAmountCumulative = 0f
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        val dragThreshold = 100f
                                        if (dragAmountCumulative > dragThreshold) {
                                            onEvent(PlayerEvent.SkipToPrevious)
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        } else if (dragAmountCumulative < -dragThreshold) {
                                            onEvent(PlayerEvent.SkipToNext)
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        }
                                        dragAmountCumulative = 0f
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        dragAmountCumulative += dragAmount
                                    }
                                )
                            },
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AlbumArtDisplay(
                                albumArtUri = uiState.currentAudioFile?.albumArtUri,
                                isPlaying = uiState.isPlaying,
                                windowWidthSizeClass = windowSizeClass,
                                playerLayout = PlayerLayout.ETHEREAL_FLOW
                            )
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        Column(
                            modifier = Modifier
                                .weight(1.5f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            TopBar(
                                onNavigateUp = onNavigateUp,
                                currentSongIndex = currentSongIndex,
                                totalQueueSize = playingQueue.size,
                                onOpenQueue = {
                                    coroutineScope.launch { sheetState.show() }
                                    showQueueBottomSheet = true
                                },
                                windowWidthSizeClass = windowSizeClass,
                                selectedLayout = selectedLayout,
                                onLayoutSelected = onLayoutSelected
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            TrackInfo(
                                title = uiState.currentAudioFile?.title,
                                artist = uiState.currentAudioFile?.artist,
                                playerLayout = PlayerLayout.ETHEREAL_FLOW
                            )

                            FavoriteButton(
                                isFavorite = uiState.isFavorite,
                                onToggleFavorite = {
                                    uiState.currentAudioFile?.let {
                                        if (uiState.isFavorite) {
                                            onEvent(PlayerEvent.RemoveFromFavorites(it.id))
                                        } else {
                                            onEvent(PlayerEvent.AddToFavorites(it))
                                        }
                                    }
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            SeekBarSection(
                                sliderValue = sliderValue,
                                totalDurationMs = uiState.totalDurationMs,
                                playbackPositionMs = uiState.playbackPositionMs,
                                isSeeking = uiState.isSeeking,
                                onSliderValueChange = { newValue ->
                                    sliderValue = newValue
                                    if (!uiState.isSeeking) onEvent(PlayerEvent.SetSeeking(true))
                                },
                                onSliderValueChangeFinished = {
                                    onEvent(PlayerEvent.SeekTo(sliderValue.toLong()))
                                    onEvent(PlayerEvent.SetSeeking(false))
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                },
                                playerLayout = PlayerLayout.ETHEREAL_FLOW
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            ControlBar(
                                shuffleMode = uiState.shuffleMode,
                                isPlaying = uiState.isPlaying,
                                repeatMode = uiState.repeatMode,
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
                                playerLayout = PlayerLayout.ETHEREAL_FLOW
                            )
                        }
                    }
                }

                WindowWidthSizeClass.Expanded -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp, vertical = 24.dp)
                            .semantics {
                                // CORRECT WAY to add custom accessibility actions
                                this.customActions = listOf(
                                    CustomAccessibilityAction(
                                        label = "Skip to previous song",
                                        action = {
                                            onEvent(PlayerEvent.SkipToPrevious)
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            true
                                        }
                                    ),
                                    CustomAccessibilityAction(
                                        label = "Skip to next song",
                                        action = {
                                            onEvent(PlayerEvent.SkipToNext)
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            true
                                        }
                                    )
                                )
                            }
                            .pointerInput(Unit) {
                                var dragAmountCumulative = 0f
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        val dragThreshold = 100f
                                        if (dragAmountCumulative > dragThreshold) {
                                            onEvent(PlayerEvent.SkipToPrevious)
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        } else if (dragAmountCumulative < -dragThreshold) {
                                            onEvent(PlayerEvent.SkipToNext)
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        }
                                        dragAmountCumulative = 0f
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        dragAmountCumulative += dragAmount
                                    }
                                )
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AlbumArtDisplay(
                                albumArtUri = uiState.currentAudioFile?.albumArtUri,
                                isPlaying = uiState.isPlaying,
                                windowWidthSizeClass = windowSizeClass,
                                playerLayout = PlayerLayout.ETHEREAL_FLOW
                            )
                        }

                        Spacer(modifier = Modifier.width(32.dp))

                        Column(
                            modifier = Modifier
                                .weight(1.5f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            TopBar(
                                onNavigateUp = onNavigateUp,
                                currentSongIndex = currentSongIndex,
                                totalQueueSize = playingQueue.size,
                                onOpenQueue = { /* No-op for expanded, queue is visible */ },
                                windowWidthSizeClass = windowSizeClass,
                                selectedLayout = selectedLayout,
                                onLayoutSelected = onLayoutSelected
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            TrackInfo(
                                title = uiState.currentAudioFile?.title,
                                artist = uiState.currentAudioFile?.artist,
                                playerLayout = PlayerLayout.ETHEREAL_FLOW
                            )

                            FavoriteButton(
                                isFavorite = uiState.isFavorite,
                                onToggleFavorite = {
                                    uiState.currentAudioFile?.let {
                                        if (uiState.isFavorite) {
                                            onEvent(PlayerEvent.RemoveFromFavorites(it.id))
                                        } else {
                                            onEvent(PlayerEvent.AddToFavorites(it))
                                        }
                                    }
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                }
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            SeekBarSection(
                                sliderValue = sliderValue,
                                totalDurationMs = uiState.totalDurationMs,
                                playbackPositionMs = uiState.playbackPositionMs,
                                isSeeking = uiState.isSeeking,
                                onSliderValueChange = { newValue ->
                                    sliderValue = newValue
                                    if (!uiState.isSeeking) onEvent(PlayerEvent.SetSeeking(true))
                                },
                                onSliderValueChangeFinished = {
                                    onEvent(PlayerEvent.SeekTo(sliderValue.toLong()))
                                    onEvent(PlayerEvent.SetSeeking(false))
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                },
                                playerLayout = PlayerLayout.ETHEREAL_FLOW
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            ControlBar(
                                shuffleMode = uiState.shuffleMode,
                                isPlaying = uiState.isPlaying,
                                repeatMode = uiState.repeatMode,
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
                                playerLayout = PlayerLayout.ETHEREAL_FLOW
                            )
                        }

                        Spacer(modifier = Modifier.width(32.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Top
                        ) {
                            PlayingQueueSection(
                                queue = playingQueue,
                                currentPlayingIndex = currentSongIndex,
                                onPlayItem = onPlayQueueItem,
                                onRemoveItem = onRemoveQueueItem
                            )
                        }
                    }
                }
            }
        }
    }
}