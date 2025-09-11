package com.engfred.musicplayer.feature_player.presentation.layouts
import android.app.Activity
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.engfred.musicplayer.feature_player.presentation.components.AlbumArtDisplay
import com.engfred.musicplayer.feature_player.presentation.components.ControlBar
import com.engfred.musicplayer.feature_player.presentation.components.FavoriteButton
import com.engfred.musicplayer.feature_player.presentation.components.PlayingQueueSection
import com.engfred.musicplayer.feature_player.presentation.components.QueueBottomSheet
import com.engfred.musicplayer.feature_player.presentation.components.SeekBarSection
import com.engfred.musicplayer.feature_player.presentation.components.TopBar
import com.engfred.musicplayer.feature_player.presentation.components.TrackInfo
import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerEvent
import com.engfred.musicplayer.feature_player.utils.getContentColorForAlbumArt
import com.engfred.musicplayer.feature_player.utils.loadBitmapFromUri
import com.engfred.musicplayer.feature_player.utils.saveBitmapToPictures
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.graphics.luminance
import com.engfred.musicplayer.core.domain.repository.RepeatMode
import com.engfred.musicplayer.core.domain.repository.ShuffleMode


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
    windowWidthSizeClass: WindowWidthSizeClass,
    windowHeightSizeClass: WindowHeightSizeClass,
    selectedLayout: PlayerLayout,
    onLayoutSelected: (PlayerLayout) -> Unit,
    playingAudio: AudioFile?,
    repeatMode: RepeatMode,
    shuffleMode: ShuffleMode
) {
    val view = LocalView.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backgroundColor = MaterialTheme.colorScheme.background
    val defaultContentColor = MaterialTheme.colorScheme.onBackground
    val colorScheme = MaterialTheme.colorScheme
    val dynamicContentColor = if (windowWidthSizeClass == WindowWidthSizeClass.Compact) {
        getContentColorForAlbumArt(context, uiState.currentAudioFile?.albumArtUri?.toString())
    } else {
        defaultContentColor
    }
// Handle status bar color and icon appearance
    DisposableEffect(windowWidthSizeClass, dynamicContentColor, selectedLayout) {
        val window = (context as? Activity)?.window
        val insetsController = window?.let { WindowInsetsControllerCompat(it, view) }
// Set status bar for compact mode in ImmersiveCanvasLayout
        if (selectedLayout == PlayerLayout.IMMERSIVE_CANVAS && windowWidthSizeClass == WindowWidthSizeClass.Compact) {
//            window?.statusBarColor = dynamicContentColor.toArgb()
// Adjust status bar icon appearance based on luminance
            insetsController?.isAppearanceLightStatusBars = (dynamicContentColor.luminance() > 0.5f).not()
        } else {
// Set to default theme status bar color
//            window?.statusBarColor = colorScheme.background.toArgb()
            insetsController?.isAppearanceLightStatusBars = colorScheme.background.luminance() > 0.5f
        }
// Cleanup: Revert to default theme status bar settings on dispose
        onDispose {
//            window?.statusBarColor = colorScheme.background.toArgb()
            insetsController?.isAppearanceLightStatusBars = colorScheme.background.luminance() > 0.5f
        }
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
    CompositionLocalProvider(LocalContentColor provides defaultContentColor) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
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
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount > 20f) {
// Drag down to exit the screen
                            onNavigateUp()
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                    }
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
// Responsive padding based on width and height
            val sectionHorizontalPadding = when {
                windowWidthSizeClass == WindowWidthSizeClass.Expanded || windowHeightSizeClass == WindowHeightSizeClass.Expanded -> 32.dp
                windowWidthSizeClass == WindowWidthSizeClass.Medium || windowHeightSizeClass == WindowHeightSizeClass.Medium -> 28.dp
                else -> 24.dp
            }
            val contentVerticalPadding = when {
                windowHeightSizeClass == WindowHeightSizeClass.Expanded -> 32.dp
                windowHeightSizeClass == WindowHeightSizeClass.Medium -> 28.dp
                else -> 24.dp
            }
            val spacingInfoToButtons = when {
                windowHeightSizeClass == WindowHeightSizeClass.Expanded -> 28.dp
                else -> 24.dp
            }
            val spacingButtonsToSeekBar = when {
                windowHeightSizeClass == WindowHeightSizeClass.Expanded -> 36.dp
                else -> 32.dp
            }
            val spacingSeekBarToControlBar = when {
                windowHeightSizeClass == WindowHeightSizeClass.Expanded -> 28.dp
                else -> 24.dp
            }
            if (windowWidthSizeClass == WindowWidthSizeClass.Compact) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        AlbumArtDisplay(
                            albumArtUri = uiState.currentAudioFile?.albumArtUri,
                            isPlaying = uiState.isPlaying,
                            windowWidthSizeClass = windowWidthSizeClass,
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
                            windowWidthSizeClass = windowWidthSizeClass,
                            selectedLayout = selectedLayout,
                            onLayoutSelected = onLayoutSelected,
                            dynamicContentColor = dynamicContentColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(backgroundColor),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.SpaceAround
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
                                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
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
                                },
                                playerLayout = PlayerLayout.IMMERSIVE_CANVAS
                            )
                            Spacer(Modifier.size(8.dp))
                        }
                        Spacer(modifier = Modifier.height(spacingInfoToButtons))
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
                                                filename = "${audioFileName}album_art.jpg",
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
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
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
                            modifier = Modifier.padding(horizontal = 16.dp),
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
                            playerLayout = PlayerLayout.IMMERSIVE_CANVAS
                        )
                        Spacer(modifier = Modifier.height(spacingSeekBarToControlBar))
                        ControlBar(
                            modifier = Modifier.navigationBarsPadding(),
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
                            playerLayout = PlayerLayout.IMMERSIVE_CANVAS,
                            windowWidthSizeClass = windowWidthSizeClass,
                            windowHeightSizeClass = windowHeightSizeClass
                        )
//                        Spacer(modifier = Modifier.height(contentVerticalPadding))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
// Album art section
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
                            playerLayout = PlayerLayout.IMMERSIVE_CANVAS,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .padding(start = sectionHorizontalPadding)
                        )
                    }
                    Spacer(modifier = Modifier.width(32.dp))
// Info section
                    Column(
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxHeight()
                            .statusBarsPadding(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.SpaceAround
                    ) {
                        TopBar(
                            onNavigateUp = onNavigateUp,
                            currentSongIndex = currentSongIndex,
                            totalQueueSize = playingQueue.size,
                            onOpenQueue = { /* No-op for Expanded, queue is visible */ },
                            windowWidthSizeClass = windowWidthSizeClass,
                            selectedLayout = selectedLayout,
                            onLayoutSelected = onLayoutSelected,
                            dynamicContentColor = defaultContentColor // Use default for non-compact
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
                                },
                                playerLayout = PlayerLayout.IMMERSIVE_CANVAS
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
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
                                            val audioFileName = uiState.currentAudioFile?.title?.replace(" ", "") ?: "album_art"
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
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
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
                            playerLayout = PlayerLayout.IMMERSIVE_CANVAS
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
                            playerLayout = PlayerLayout.IMMERSIVE_CANVAS,
                            windowWidthSizeClass = windowWidthSizeClass,
                            windowHeightSizeClass = windowHeightSizeClass
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    if (windowWidthSizeClass == WindowWidthSizeClass.Expanded) {
                        Spacer(modifier = Modifier.width(32.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .navigationBarsPadding()
                                .padding(end = 13.dp, top = sectionHorizontalPadding, bottom = sectionHorizontalPadding),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Top
                        ) {
                            PlayingQueueSection(
                                playingQueue = playingQueue,
                                playingAudio = playingAudio,
                                onPlayItem = onPlayQueueItem,
                                onRemoveItem = onRemoveQueueItem,
                                isCompact = false
                            )
                        }
                    }
                }
            }
        }
    }
}