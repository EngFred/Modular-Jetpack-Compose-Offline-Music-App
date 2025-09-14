package com.engfred.musicplayer.feature_player.presentation.layouts

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.model.PlayerLayout
import com.engfred.musicplayer.core.domain.repository.PlaybackState
import com.engfred.musicplayer.core.domain.repository.RepeatMode
import com.engfred.musicplayer.core.domain.repository.ShuffleMode
import com.engfred.musicplayer.core.util.shareAudioFile
import com.engfred.musicplayer.feature_player.presentation.components.AlbumArtDisplay
import com.engfred.musicplayer.feature_player.presentation.components.ControlBar
import com.engfred.musicplayer.feature_player.presentation.components.FavoriteButton
import com.engfred.musicplayer.feature_player.presentation.components.PlayingQueueSection
import com.engfred.musicplayer.feature_player.presentation.components.QueueBottomSheet
import com.engfred.musicplayer.feature_player.presentation.components.SeekBarSection
import com.engfred.musicplayer.feature_player.presentation.components.TopBar
import com.engfred.musicplayer.feature_player.presentation.components.TrackInfo
import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerEvent
import com.engfred.musicplayer.feature_player.utils.getDynamicGradientColors
import kotlinx.coroutines.launch

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
    selectedLayout: PlayerLayout,
    onLayoutSelected: (PlayerLayout) -> Unit,
    playingAudio: AudioFile?,
    repeatMode: RepeatMode,
    shuffleMode: ShuffleMode
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidthDp = configuration.screenWidthDp

    val view = LocalView.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    // responsive breakpoint (same as other screens)
    val isTablet = screenWidthDp >= 900

    // Handle status bar color and icon appearance
    DisposableEffect(isLandscape, selectedLayout) {
        val window = (context as? Activity)?.window
        val insetsController = window?.let { WindowInsetsControllerCompat(it, view) }

        // Set status bar icons to dark for light themes, and light for dark themes
        window?.let {
            WindowCompat.getInsetsController(it, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }

        onDispose {
            insetsController?.isAppearanceLightStatusBars = colorScheme.background.luminance() > 0.5f
        }
    }

    // Dynamic gradient based on album art
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

    if (showQueueBottomSheet && !isLandscape) {
        QueueBottomSheet(
            onDismissRequest = { showQueueBottomSheet = false },
            sheetState = sheetState,
            playingQueue = playingQueue,
            onPlayQueueItem = onPlayQueueItem,
            onRemoveQueueItem = onRemoveQueueItem,
            playingAudio = playingAudio,
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
            isPlaying = uiState.isPlaying
        )
    }

    var verticalDragCumulative by remember { mutableFloatStateOf(0f) }
    val dragThreshold = 100f

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
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (verticalDragCumulative > dragThreshold) {
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
                .systemBarsPadding()
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
        ) {
            // responsive paddings & spacing
            val horizontalPadding = when {
                isTablet -> 40.dp
                isLandscape -> 32.dp
                else -> 0.dp
            }
            val verticalPadding = when {
                isTablet -> 36.dp
                isLandscape -> 0.dp
                else -> 0.dp
            }
            val spacing = when {
                isTablet -> 24.dp
                isLandscape -> 20.dp
                else -> 16.dp
            }

            if (!isLandscape) {
                // Portrait — enlarge hit targets / art on tablets
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalPadding, vertical = verticalPadding),
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
                        selectedLayout = selectedLayout,
                        onLayoutSelected = onLayoutSelected,
                        modifier = Modifier.fillMaxWidth(),
                        onShareAudio = {
                            uiState.currentAudioFile?.let { shareAudioFile(context, it) }
                        }
                    )

                    // Album art — larger on tablet
                    AlbumArtDisplay(
                        albumArtUri = uiState.currentAudioFile?.albumArtUri,
                        isPlaying = uiState.isPlaying,
                        playerLayout = PlayerLayout.ETHEREAL_FLOW,
                        modifier = Modifier
                            .fillMaxWidth()
                    )

                    TrackInfo(
                        title = uiState.currentAudioFile?.title,
                        artist = uiState.currentAudioFile?.artist,
                        playerLayout = PlayerLayout.ETHEREAL_FLOW,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(spacing))

                    FavoriteButton(
                        isFavorite = uiState.isFavorite,
                        onToggleFavorite = {
                            uiState.currentAudioFile?.let {
                                if (uiState.isFavorite) onEvent(PlayerEvent.RemoveFromFavorites(it.id))
                                else onEvent(PlayerEvent.AddToFavorites(it))
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            }
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
                        modifier = Modifier.padding(horizontal = if (isTablet) 28.dp else 24.dp)
                    )

                    Spacer(modifier = Modifier.height(spacing))

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
                        playerLayout = PlayerLayout.ETHEREAL_FLOW,
                        modifier = Modifier.navigationBarsPadding().padding(horizontal = if (isTablet) 28.dp else 24.dp).padding(bottom = 16.dp, top = 8.dp)
                    )
                }
            } else {
                // Landscape — three-column arrangement (left: art, middle: controls, right: queue)
                val leftWeight = if (isTablet) 1.2f else 1f
                val middleWeight = if (isTablet) 1.6f else 1.5f
                val rightWeight = if (isTablet) 1.2f else 1f

                Row(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: album art
                    Column(
                        modifier = Modifier
                            .weight(leftWeight)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AlbumArtDisplay(
                            albumArtUri = uiState.currentAudioFile?.albumArtUri,
                            isPlaying = uiState.isPlaying,
                            playerLayout = PlayerLayout.ETHEREAL_FLOW,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(start = if (isTablet) 24.dp else 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // Middle: controls & info
                    Column(
                        modifier = Modifier
                            .weight(middleWeight)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        TopBar(
                            onNavigateUp = onNavigateUp,
                            currentSongIndex = currentSongIndex,
                            totalQueueSize = playingQueue.size,
                            onOpenQueue = { /* no-op in wide */ },
                            selectedLayout = selectedLayout,
                            onLayoutSelected = onLayoutSelected,
                            modifier = Modifier.fillMaxWidth(),
                            onShareAudio = {
                                uiState.currentAudioFile?.let { shareAudioFile(context, it) }
                            }
                        )

                        TrackInfo(
                            title = uiState.currentAudioFile?.title,
                            artist = uiState.currentAudioFile?.artist,
                            playerLayout = PlayerLayout.ETHEREAL_FLOW,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(spacing))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FavoriteButton(
                                isFavorite = uiState.isFavorite,
                                onToggleFavorite = {
                                    uiState.currentAudioFile?.let {
                                        if (uiState.isFavorite) onEvent(PlayerEvent.RemoveFromFavorites(it.id))
                                        else onEvent(PlayerEvent.AddToFavorites(it))
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    }
                                },
                                playerLayout = PlayerLayout.ETHEREAL_FLOW
                            )
                        }

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
                            playerLayout = PlayerLayout.ETHEREAL_FLOW)

                    }

                    Spacer(modifier = Modifier.width(if (isTablet) 36.dp else 24.dp))

                    // Right: queue
                    Column(
                        modifier = Modifier
                            .weight(rightWeight)
                            .fillMaxHeight()
                            .navigationBarsPadding()
                            .padding(end = if (isTablet) 12.dp else 8.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Top
                    ) {
                        PlayingQueueSection(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(LocalContentColor.current.copy(alpha = 0.06f))
                                .padding(bottom = 7.dp),
                            playingQueue = playingQueue,
                            playingAudio = uiState.currentAudioFile,
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
