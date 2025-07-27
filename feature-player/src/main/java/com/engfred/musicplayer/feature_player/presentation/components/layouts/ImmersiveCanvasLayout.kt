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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.repository.PlaybackState
import com.engfred.musicplayer.core.util.shareAudioFile
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
import kotlinx.coroutines.launch
import android.widget.Toast // Import Toast
import com.engfred.musicplayer.feature_player.utils.loadBitmapFromUri
import com.engfred.musicplayer.feature_player.utils.saveBitmapToPictures

/**
 * The Immersive Canvas layout for the music player screen.
 * This layout focuses on a split-screen design in compact mode,
 * featuring the album art prominently on top and controls below.
 * In wider screen sizes, it transitions to a horizontal arrangement.
 * It provides responsive design, haptic feedback, and gesture controls for navigation.
 *
 * @param uiState The current playback state of the player.
 * @param onEvent Callback for dispatching [PlayerEvent]s to the ViewModel.
 * @param onNavigateUp Callback to navigate up in the navigation stack.
 * @param playingQueue The list of songs in the current playback queue.
 * @param currentSongIndex The index of the currently playing song in the queue.
 * @param onPlayQueueItem Callback to play a specific item from the queue.
 * @param onRemoveQueueItem Callback to remove an item from the queue.
 * @param windowSizeClass The current window size class (Compact, Medium, Expanded).
 * @param selectedLayout The currently selected player layout.
 * @param onLayoutSelected Callback to change the selected player layout.
 * @param playingAudio The currently playing [AudioFile].
 */
@RequiresApi(Build.VERSION_CODES.M)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveCanvasLayout(
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
    // Synchronize slider with actual playback position, but only if not actively seeking
    var sliderValue by remember { mutableFloatStateOf(uiState.playbackPositionMs.toFloat()) }
    LaunchedEffect(uiState.playbackPositionMs, uiState.isSeeking) {
        if (!uiState.isSeeking) {
            sliderValue = uiState.playbackPositionMs.toFloat()
        }
    }

    val view = LocalView.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val backgroundColor = MaterialTheme.colorScheme.background
    val contentColor = MaterialTheme.colorScheme.onBackground

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

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                // Add semantics for accessibility actions to the entire Box
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
                            currentSongIndex = currentSongIndex,
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
                            verticalAlignment = Alignment.Top,
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

                        // Row: Download, Share, and Queue Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                uiState.currentAudioFile?.albumArtUri?.let { uri ->
                                    coroutineScope.launch {
                                        val bitmap = loadBitmapFromUri(context, uri)
                                        if (bitmap != null) {
                                            val audioFileName = uiState.currentAudioFile?.title?.replace(" ", "_") ?: "album_art"
                                            val success = saveBitmapToPictures(
                                                context = context,
                                                bitmap = bitmap,
                                                filename = "${audioFileName}_album_art.jpg",
                                                mimeType = "image/jpeg"
                                            )
                                            if (success) {
                                                Toast.makeText(context, "Album art saved!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Failed to save album art.", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "No album art found to save.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } ?: run {
                                    Toast.makeText(context, "No album art available for this song.", Toast.LENGTH_SHORT).show()
                                }
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) // Haptic feedback on click
                            }) {
                                Icon(
                                    Icons.Rounded.Download,
                                    contentDescription = "Download Album Art",
                                    tint = LocalContentColor.current
                                )
                            }
                            IconButton(onClick = {
                                // Ensure currentSongIndex is valid before sharing
                                if (currentSongIndex >= 0 && currentSongIndex < playingQueue.size) {
                                    shareAudioFile(context, playingQueue[currentSongIndex])
                                }
                            }) {
                                Icon(
                                    Icons.Rounded.Share,
                                    contentDescription = "Share",
                                    tint = LocalContentColor.current
                                )
                            }
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
                            onSliderValueChange = { newValue ->
                                sliderValue = newValue
                                if (!uiState.isSeeking) onEvent(PlayerEvent.SetSeeking(true))
                            },
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
                            playerLayout = PlayerLayout.IMMERSIVE_CANVAS
                        )
                        Spacer(modifier = Modifier.height(contentVerticalPadding))
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
                            .weight(1.5f) // Album art takes more space than in desktop mode
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
                                .aspectRatio(1f) // Maintain square aspect ratio
                                .padding(horizontal = 24.dp) // Add padding to album art on larger screens
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
                            currentSongIndex = currentSongIndex,
                            totalQueueSize = playingQueue.size,
                            onOpenQueue = { /* No-op for Expanded, queue is visible */ },
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

                        // Download and Share buttons for wider screens (Queue button not here as queue is visible)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                uiState.currentAudioFile?.albumArtUri?.let { uri ->
                                    coroutineScope.launch {
                                        val bitmap = loadBitmapFromUri(context, uri)
                                        if (bitmap != null) {
                                            val audioFileName = uiState.currentAudioFile?.title?.replace(" ", "_") ?: "album_art"
                                            val success = saveBitmapToPictures(
                                                context = context,
                                                bitmap = bitmap,
                                                filename = "${audioFileName}_album_art.jpg",
                                                mimeType = "image/jpeg"
                                            )
                                            if (success) {
                                                Toast.makeText(context, "Album art saved!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Failed to save album art.", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "No album art found to save.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } ?: run {
                                    Toast.makeText(context, "No album art available for this song.", Toast.LENGTH_SHORT).show()
                                }
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) // Haptic feedback on click
                            }) {
                                Icon(
                                    Icons.Rounded.Download,
                                    contentDescription = "Download Album Art",
                                    tint = LocalContentColor.current
                                )
                            }
                            IconButton(onClick = {
                                if (currentSongIndex >= 0 && currentSongIndex < playingQueue.size) {
                                    shareAudioFile(context, playingQueue[currentSongIndex])
                                }
                            }) {
                                Icon(
                                    Icons.Rounded.Share,
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
                            onSliderValueChange = { newValue ->
                                sliderValue = newValue
                                if (!uiState.isSeeking) onEvent(PlayerEvent.SetSeeking(true))
                            },
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
                            playerLayout = PlayerLayout.IMMERSIVE_CANVAS
                        )
                    }

                    if (windowSizeClass == WindowWidthSizeClass.Expanded) {
                        Spacer(modifier = Modifier.width(32.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .statusBarsPadding()
                                .padding(vertical = contentVerticalPadding), // Add vertical padding to match other sections
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