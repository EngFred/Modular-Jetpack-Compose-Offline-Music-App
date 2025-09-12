package com.engfred.musicplayer.feature_player.presentation.layouts

import android.app.Activity
import android.content.res.Configuration
import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
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
import com.engfred.musicplayer.core.domain.repository.RepeatMode
import com.engfred.musicplayer.core.domain.repository.ShuffleMode
import com.engfred.musicplayer.feature_player.presentation.components.AlbumArtDisplay
import com.engfred.musicplayer.feature_player.presentation.components.ControlBar
import com.engfred.musicplayer.feature_player.presentation.components.FavoriteButton
import com.engfred.musicplayer.feature_player.presentation.components.PlayingQueueSection
import com.engfred.musicplayer.feature_player.presentation.components.QueueBottomSheet
import com.engfred.musicplayer.feature_player.presentation.components.SeekBarSection
import com.engfred.musicplayer.feature_player.presentation.components.TopBar
import com.engfred.musicplayer.feature_player.presentation.components.TrackInfo
import com.engfred.musicplayer.feature_player.utils.getContentColorForAlbumArt
import com.engfred.musicplayer.feature_player.utils.loadBitmapFromUri
import com.engfred.musicplayer.feature_player.utils.saveBitmapToPictures
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.core.view.WindowInsetsControllerCompat
import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerEvent

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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val view = LocalView.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backgroundColor = MaterialTheme.colorScheme.background
    val defaultContentColor = MaterialTheme.colorScheme.onBackground
    val colorScheme = MaterialTheme.colorScheme
    val dynamicContentColor = if (!isLandscape) {
        getContentColorForAlbumArt(context, uiState.currentAudioFile?.albumArtUri?.toString())
    } else {
        defaultContentColor
    }
    // Handle status bar color and icon appearance
    DisposableEffect(isLandscape, dynamicContentColor, selectedLayout) {
        val window = (context as? Activity)?.window
        val insetsController = window?.let { WindowInsetsControllerCompat(it, view) }
        // Set status bar for portrait mode in ImmersiveCanvasLayout
        if (selectedLayout == PlayerLayout.IMMERSIVE_CANVAS && !isLandscape) {
            insetsController?.isAppearanceLightStatusBars = (dynamicContentColor.luminance() > 0.5f).not()
        } else {
            insetsController?.isAppearanceLightStatusBars = colorScheme.background.luminance() > 0.5f
        }
        // Cleanup: Revert to default theme status bar settings on dispose
        onDispose {
            insetsController?.isAppearanceLightStatusBars = colorScheme.background.luminance() > 0.5f
        }
    }
    var showQueueBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showQueueBottomSheet && !isLandscape) {
        QueueBottomSheet(
            onDismissRequest = { showQueueBottomSheet = false },
            sheetState = sheetState,
            playingQueue = playingQueue,
            onPlayQueueItem = onPlayQueueItem,
            onRemoveQueueItem = onRemoveQueueItem,
            playingAudio = playingAudio,
            isPlaying = uiState.isPlaying
        )
    }
    var verticalDragCumulative by remember { mutableStateOf(0f) }
    val dragThreshold = 100f
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
                    var horizontalDragCumulative = 0f
                    val horizontalThreshold = 100f
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (horizontalDragCumulative > horizontalThreshold) {
                                onEvent(PlayerEvent.SkipToPrevious)
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            } else if (horizontalDragCumulative < -horizontalThreshold) {
                                onEvent(PlayerEvent.SkipToNext)
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                            horizontalDragCumulative = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            horizontalDragCumulative += dragAmount
                            true
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (verticalDragCumulative > dragThreshold) {
                                // Drag down to exit
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
            val horizontalPadding = if (isLandscape) 32.dp else 24.dp
            val verticalPadding = if (isLandscape) 28.dp else 24.dp
            val spacingInfoToButtons = if (isLandscape) 28.dp else 24.dp
            val spacingButtonsToSeekBar = if (isLandscape) 36.dp else 32.dp
            val spacingSeekBarToControlBar = if (isLandscape) 28.dp else 24.dp

            if (!isLandscape) {
                // Portrait layout
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        AlbumArtDisplay(
                            albumArtUri = uiState.currentAudioFile?.albumArtUri,
                            isPlaying = uiState.isPlaying,
                            windowWidthSizeClass = WindowWidthSizeClass.Compact, // Unused but passed
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
                            windowWidthSizeClass = WindowWidthSizeClass.Compact, // Unused but passed
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
                            .background(backgroundColor)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.SpaceAround
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))
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
                            windowWidthSizeClass = WindowWidthSizeClass.Compact, // Unused but passed
                            windowHeightSizeClass = WindowHeightSizeClass.Medium // Unused but passed
                        )
                        Spacer(modifier = Modifier.height(spacingSeekBarToControlBar))
                        SeekBarSection(
                            modifier = Modifier.navigationBarsPadding().padding(horizontal = 14.dp),
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
                            playerLayout = PlayerLayout.IMMERSIVE_CANVAS,
                            isPlaying = uiState.isPlaying
                        )
                        Spacer(modifier = Modifier.height(spacingSeekBarToControlBar))
                    }
                }
            } else {
                // Landscape layout
                Row(
                    modifier = Modifier.fillMaxSize().systemBarsPadding().padding(bottom = 13.dp),
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
                            windowWidthSizeClass = WindowWidthSizeClass.Expanded, // Unused but passed
                            playerLayout = PlayerLayout.IMMERSIVE_CANVAS,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .padding(start = horizontalPadding)
                        )
                    }
                    Spacer(modifier = Modifier.width(32.dp))
                    // Controls and info section
                    Column(
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.SpaceAround
                    ) {
                        TopBar(
                            onNavigateUp = onNavigateUp,
                            currentSongIndex = currentSongIndex,
                            totalQueueSize = playingQueue.size,
                            onOpenQueue = { /* No-op for landscape, queue is visible */ },
                            windowWidthSizeClass = WindowWidthSizeClass.Expanded, // Unused but passed
                            selectedLayout = selectedLayout,
                            onLayoutSelected = onLayoutSelected,
                            dynamicContentColor = defaultContentColor // Use default for landscape
                        )
//                        Spacer(modifier = Modifier.height(spacingInfoToButtons))
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.SpaceAround
                        ) {
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
                                windowWidthSizeClass = WindowWidthSizeClass.Expanded, // Unused but passed
                                windowHeightSizeClass = WindowHeightSizeClass.Medium // Unused but passed
                            )
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
                                playerLayout = PlayerLayout.IMMERSIVE_CANVAS,
                                isPlaying = uiState.isPlaying
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(32.dp))
                    // Queue section in landscape
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .systemBarsPadding().padding(end = 8.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Top
                    ) {
                        PlayingQueueSection(
                            modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(LocalContentColor.current.copy(alpha = 0.05f)).padding(bottom = 7.dp),
                            playingQueue = playingQueue,
                            playingAudio = playingAudio,
                            onPlayItem = onPlayQueueItem,
                            onRemoveItem = onRemoveQueueItem,
                            isCompact = false,
                            isPlaying = uiState.isPlaying
                        )
                    }
                }
            }
        }
    }
}