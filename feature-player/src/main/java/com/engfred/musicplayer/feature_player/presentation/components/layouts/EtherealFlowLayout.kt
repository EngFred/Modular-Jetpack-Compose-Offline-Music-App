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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
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
    windowWidthSizeClass: WindowWidthSizeClass,
    windowHeightSizeClass: WindowHeightSizeClass,
    selectedLayout: PlayerLayout,
    onLayoutSelected: (PlayerLayout) -> Unit,
    playingAudio: AudioFile?
) {
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

    val dynamicContentColor by remember(gradientColors) {
        val topGradientColor = gradientColors.firstOrNull() ?: Color.Black
        val targetLuminance = topGradientColor.luminance()
        val chosenColor = if (targetLuminance > 0.5f) Color.Black else Color.White
        mutableStateOf(chosenColor)
    }

    var showQueueBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showQueueBottomSheet && windowWidthSizeClass != WindowWidthSizeClass.Expanded) {
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
            // Responsive padding based on width and height
            val horizontalPadding = when {
                windowWidthSizeClass == WindowWidthSizeClass.Expanded || windowHeightSizeClass == WindowHeightSizeClass.Expanded -> 32.dp
                windowWidthSizeClass == WindowWidthSizeClass.Medium || windowHeightSizeClass == WindowHeightSizeClass.Medium -> 28.dp
                else -> 24.dp
            }
            val verticalPadding = when {
                windowHeightSizeClass == WindowHeightSizeClass.Expanded -> 36.dp
                windowHeightSizeClass == WindowHeightSizeClass.Medium -> 32.dp
                else -> 28.dp
            }
            val spacing = when {
                windowHeightSizeClass == WindowHeightSizeClass.Expanded -> 24.dp
                windowHeightSizeClass == WindowHeightSizeClass.Medium -> 20.dp
                else -> 16.dp
            }

            when (windowWidthSizeClass) {
                WindowWidthSizeClass.Compact -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = verticalPadding)
                            .semantics {
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
                            windowWidthSizeClass = windowWidthSizeClass,
                            selectedLayout = selectedLayout,
                            onLayoutSelected = onLayoutSelected,
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                        )
                        AlbumArtDisplay(
                            albumArtUri = uiState.currentAudioFile?.albumArtUri,
                            isPlaying = uiState.isPlaying,
                            windowWidthSizeClass = windowWidthSizeClass,
                            playerLayout = PlayerLayout.ETHEREAL_FLOW
                        )
                        TrackInfo(
                            title = uiState.currentAudioFile?.title,
                            artist = uiState.currentAudioFile?.artist,
                            playerLayout = PlayerLayout.ETHEREAL_FLOW,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(spacing / 2))
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
                            },
                            playerLayout = PlayerLayout.ETHEREAL_FLOW
                        )
                        Spacer(modifier = Modifier.height(spacing))
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
                            playerLayout = PlayerLayout.ETHEREAL_FLOW,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(spacing))
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
                            playerLayout = PlayerLayout.ETHEREAL_FLOW,
                            windowWidthSizeClass = windowWidthSizeClass,
                            windowHeightSizeClass = windowHeightSizeClass,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
                WindowWidthSizeClass.Medium -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                            .semantics {
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
                                windowWidthSizeClass = windowWidthSizeClass,
                                playerLayout = PlayerLayout.ETHEREAL_FLOW
                            )
                        }
                        Spacer(modifier = Modifier.width(horizontalPadding))
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
                                windowWidthSizeClass = windowWidthSizeClass,
                                selectedLayout = selectedLayout,
                                onLayoutSelected = onLayoutSelected
                            )
                            Spacer(modifier = Modifier.height(spacing))
                            TrackInfo(
                                title = uiState.currentAudioFile?.title,
                                artist = uiState.currentAudioFile?.artist,
                                playerLayout = PlayerLayout.ETHEREAL_FLOW,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(spacing / 2))
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
                                },
                                playerLayout = PlayerLayout.ETHEREAL_FLOW
                            )
                            Spacer(modifier = Modifier.height(spacing))
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
                                playerLayout = PlayerLayout.ETHEREAL_FLOW
                            )
                            Spacer(modifier = Modifier.height(spacing))
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
                                playerLayout = PlayerLayout.ETHEREAL_FLOW,
                                windowWidthSizeClass = windowWidthSizeClass,
                                windowHeightSizeClass = windowHeightSizeClass
                            )
                        }
                    }
                }
                WindowWidthSizeClass.Expanded -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                            .semantics {
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
                                windowWidthSizeClass = windowWidthSizeClass,
                                playerLayout = PlayerLayout.ETHEREAL_FLOW
                            )
                        }
                        Spacer(modifier = Modifier.width(horizontalPadding))
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
                                windowWidthSizeClass = windowWidthSizeClass,
                                selectedLayout = selectedLayout,
                                onLayoutSelected = onLayoutSelected
                            )
                            Spacer(modifier = Modifier.height(spacing))
                            TrackInfo(
                                title = uiState.currentAudioFile?.title,
                                artist = uiState.currentAudioFile?.artist,
                                playerLayout = PlayerLayout.ETHEREAL_FLOW,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(spacing / 2))
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
                                },
                                playerLayout = PlayerLayout.ETHEREAL_FLOW
                            )
                            Spacer(modifier = Modifier.height(spacing))
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
                                playerLayout = PlayerLayout.ETHEREAL_FLOW
                            )
                            Spacer(modifier = Modifier.height(spacing))
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
                                playerLayout = PlayerLayout.ETHEREAL_FLOW,
                                windowWidthSizeClass = windowWidthSizeClass,
                                windowHeightSizeClass = windowHeightSizeClass
                            )
                        }
                        Spacer(modifier = Modifier.width(horizontalPadding))
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