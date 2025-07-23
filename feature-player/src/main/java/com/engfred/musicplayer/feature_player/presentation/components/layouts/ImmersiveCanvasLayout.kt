package com.engfred.musicplayer.feature_player.presentation.components.layouts

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.model.repository.PlaybackState
import com.engfred.musicplayer.feature_player.domain.model.PlayerLayout
import com.engfred.musicplayer.feature_player.presentation.components.layouts.components.AlbumArtDisplay
import com.engfred.musicplayer.feature_player.presentation.components.layouts.components.ControlBar
import com.engfred.musicplayer.feature_player.presentation.components.layouts.components.FavoriteButton
import com.engfred.musicplayer.feature_player.presentation.components.layouts.components.PlayingQueueSection
import com.engfred.musicplayer.feature_player.presentation.components.layouts.components.QueueBottomSheet
import com.engfred.musicplayer.feature_player.presentation.components.layouts.components.SeekBarSection
import com.engfred.musicplayer.feature_player.presentation.components.layouts.components.TrackInfo
import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerEvent
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch
import com.engfred.musicplayer.feature_player.presentation.components.layouts.components.TopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveCanvasLayout(
    uiState: PlaybackState,
    onEvent: (PlayerEvent) -> Unit,
    onNavigateUp: () -> Unit = {},
    playingQueue: List<AudioFile> = emptyList(),
    currentSongIndex: Int = 3,
    onPlayQueueItem: (AudioFile) -> Unit = {},
    onRemoveQueueItem: (AudioFile) -> Unit = {},
    windowSizeClass: WindowWidthSizeClass,
    selectedLayout: PlayerLayout, // Added for layout selection
    onLayoutSelected: (PlayerLayout) -> Unit // Added for layout selection callback
) {
    var sliderValue by remember { mutableFloatStateOf(uiState.playbackPositionMs.toFloat()) }
    val view = LocalView.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val backgroundColor = MaterialTheme.colorScheme.background
    val contentColor = MaterialTheme.colorScheme.onBackground

//    val systemUiController = rememberSystemUiController()
//    LaunchedEffect(Unit) {
//        systemUiController.setStatusBarColor(
//            color = Color.Transparent,
//            darkIcons = false
//        )
//        systemUiController.setNavigationBarColor(
//            color = backgroundColor,
//            darkIcons = contentColor.luminance() > 0.5f
//        )
//    }

    var showQueueBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showQueueBottomSheet && windowSizeClass != WindowWidthSizeClass.Expanded) {
        QueueBottomSheet(
            onDismissRequest = { showQueueBottomSheet = false },
            sheetState = sheetState,
            playingQueue = playingQueue,
            currentSongIndex = currentSongIndex,
            onPlayQueueItem = onPlayQueueItem,
            onRemoveQueueItem = onRemoveQueueItem
        )
    }

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
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
                }
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
            val sectionHorizontalPadding = 24.dp
            val contentVerticalPadding = 24.dp
            val spacingInfoToButtons = 24.dp
            val spacingButtonsToSeekBar = 32.dp
            val spacingSeekBarToControlBar = 24.dp

            if (windowSizeClass == WindowWidthSizeClass.Compact) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // TOP HALF: Album Art (backdrop) + Overlaying Top Controls
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) // Takes 50% of the screen height
                    ) {
                        AlbumArtDisplay(
                            albumArtUri = uiState.currentAudioFile?.albumArtUri,
                            isPlaying = uiState.isPlaying,
                            windowWidthSizeClass = windowSizeClass,
                            playerLayout = PlayerLayout.IMMERSIVE_CANVAS,
                            modifier = Modifier.fillMaxSize()
                        )
                        TopBar(
                            onNavigateUp = onNavigateUp,
                            currentSongIndex = currentSongIndex + 1,
                            totalQueueSize = playingQueue.size,
                            onOpenQueue = {
                                coroutineScope.launch { sheetState.show() }
                                showQueueBottomSheet = true
                            },
                            windowWidthSizeClass = windowSizeClass,
                            selectedLayout = selectedLayout,
                            onLayoutSelected = onLayoutSelected,
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = sectionHorizontalPadding, vertical = 8.dp)
                                .align(Alignment.TopCenter)
                        )
                    }

                    // BOTTOM HALF: Info, Buttons (Download, Share, Queue), Seekbar, Control Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) // Takes remaining 50% of the screen height
                            .background(backgroundColor)
                            .padding(horizontal = sectionHorizontalPadding),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.SpaceAround // Use SpaceAround to fill remaining height
                    ) {
                        Spacer(modifier = Modifier.height(contentVerticalPadding))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TrackInfo(
                                title = uiState.currentAudioFile?.title,
                                artist = uiState.currentAudioFile?.artist,
                                playerLayout = PlayerLayout.IMMERSIVE_CANVAS,
                                modifier = Modifier.weight(1f)
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
                        }
                        Spacer(modifier = Modifier.height(spacingInfoToButtons))

                        // Row: Download, Share, and now Queue Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround, // Changed to SpaceAround for 3 buttons
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { /* Will implement later */ }) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = LocalContentColor.current
                                )
                            }
                            IconButton(onClick = { /* Will implement later */ }) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = LocalContentColor.current
                                )
                            }
                            // Moved Queue button here
                            IconButton(onClick = {
                                coroutineScope.launch { sheetState.show() }
                                showQueueBottomSheet = true
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.QueueMusic,
                                    contentDescription = "Open Queue",
                                    tint = LocalContentColor.current
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(spacingButtonsToSeekBar))

                        SeekBarSection(
                            sliderValue = sliderValue,
                            totalDurationMs = uiState.totalDurationMs,
                            playbackPositionMs = uiState.playbackPositionMs,
                            isSeeking = uiState.isSeeking,
                            onSliderValueChange = { newValue -> sliderValue = newValue },
                            onSliderValueChangeFinished = {
                                onEvent(PlayerEvent.SeekTo(sliderValue.toLong()))
                                onEvent(PlayerEvent.SetSeeking(false))
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            },
                            playerLayout = PlayerLayout.IMMERSIVE_CANVAS
                        )
                        Spacer(modifier = Modifier.height(spacingSeekBarToControlBar))

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
                            playerLayout = PlayerLayout.IMMERSIVE_CANVAS,
                            onOpenQueue = null
                        )
                        Spacer(modifier = Modifier.height(contentVerticalPadding)) // Bottom padding for this section
                    }
                }
            } else { // Medium or Expanded Window Width Class (Tablet/Desktop Layout)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AlbumArtDisplay(
                            albumArtUri = uiState.currentAudioFile?.albumArtUri,
                            isPlaying = uiState.isPlaying,
                            windowWidthSizeClass = windowSizeClass,
                            playerLayout = PlayerLayout.IMMERSIVE_CANVAS,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .statusBarsPadding()
                        )
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    Column(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight()
                            .padding(horizontal = sectionHorizontalPadding, vertical = contentVerticalPadding)
                            .statusBarsPadding(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.SpaceAround
                    ) {
                        TopBar(
                            onNavigateUp = onNavigateUp,
                            currentSongIndex = currentSongIndex + 1,
                            totalQueueSize = playingQueue.size,
                            onOpenQueue = {
                                coroutineScope.launch { sheetState.show() }
                                showQueueBottomSheet = true
                            },
                            windowWidthSizeClass = windowSizeClass,
                            selectedLayout = selectedLayout,
                            onLayoutSelected = onLayoutSelected
                        )
                        Spacer(modifier = Modifier.height(spacingInfoToButtons))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TrackInfo(
                                title = uiState.currentAudioFile?.title,
                                artist = uiState.currentAudioFile?.artist,
                                playerLayout = PlayerLayout.IMMERSIVE_CANVAS,
                                modifier = Modifier.weight(1f)
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
                        }
                        Spacer(modifier = Modifier.height(spacingButtonsToSeekBar))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { /* Will implement later */ }) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = LocalContentColor.current
                                )
                            }
                            IconButton(onClick = { /* Will implement later */ }) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = LocalContentColor.current
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(spacingButtonsToSeekBar))

                        SeekBarSection(
                            sliderValue = sliderValue,
                            totalDurationMs = uiState.totalDurationMs,
                            playbackPositionMs = uiState.playbackPositionMs,
                            isSeeking = uiState.isSeeking,
                            onSliderValueChange = { newValue -> sliderValue = newValue },
                            onSliderValueChangeFinished = {
                                onEvent(PlayerEvent.SeekTo(sliderValue.toLong()))
                                onEvent(PlayerEvent.SetSeeking(false))
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            },
                            playerLayout = PlayerLayout.IMMERSIVE_CANVAS
                        )
                        Spacer(modifier = Modifier.height(spacingSeekBarToControlBar))

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
                            playerLayout = PlayerLayout.IMMERSIVE_CANVAS,
                            onOpenQueue = null
                        )
                    }

                    if (windowSizeClass == WindowWidthSizeClass.Expanded) {
                        Spacer(modifier = Modifier.width(32.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .statusBarsPadding(),
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