package com.engfred.musicplayer.feature_player.presentation.components

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.OptIn
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.engfred.musicplayer.core.domain.model.repository.PlaybackState
import com.engfred.musicplayer.core.domain.model.repository.RepeatMode
import com.engfred.musicplayer.core.domain.model.repository.ShuffleMode
import com.engfred.musicplayer.core.util.formatDuration
import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerEvent

// NEW IMPORT for luminance
import androidx.compose.ui.graphics.luminance
import com.google.accompanist.systemuicontroller.rememberSystemUiController // For system bar colors

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(UnstableApi::class)
@Composable
fun ImmersiveCanvasLayout(
    uiState: PlaybackState,
    onEvent: (PlayerEvent) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(uiState.playbackPositionMs.toFloat()) }
    val view = LocalView.current

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    // State to toggle visibility of controls
    var controlsVisible by remember { mutableStateOf(true) }

    val controlsAlpha by animateFloatAsState(
        targetValue = if (controlsVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "controlsAlpha"
    )
    val controlsTranslationY by animateFloatAsState(
        targetValue = if (controlsVisible) 0f else 100f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "controlsTranslationY"
    )

    LaunchedEffect(uiState.playbackPositionMs, uiState.isSeeking) {
        if (!uiState.isSeeking) {
            sliderValue = uiState.playbackPositionMs.toFloat()
        }
    }

    // --- NEW: Determine dynamic content color for text/icons ---
    // For ImmersiveCanvas, the background is always dimmed/darkened by the black gradient.
    // So, we'll generally default to white text.
    // However, for system bars, we still rely on the underlying album art.
    val systemUiController = rememberSystemUiController()
    val dynamicContentColor = remember(uiState.currentAudioFile?.albumArtUri) {
        // Since we're applying a heavy blur and a black gradient,
        // it's highly probable the background will be dark.
        // We can make an assumption or, more robustly, calculate it from a sample of the blurred image.
        // For simplicity and given the dark overlay, defaulting to white is usually safe here.
        // If you had a way to get the *final* color of the background after blur/gradient, that would be ideal.
        // For now, let's assume the dark overlay makes content prefer white.
        Color.White
    }

    // Set system bar colors dynamically based on original album art dominant color
    LaunchedEffect(uiState.currentAudioFile?.albumArtUri) {
        // In a real scenario, you'd extract dominant colors from the *original* album art here
        // to set the system bar colors appropriately. For simplicity, we'll use a consistent dark color
        // for the system bars since the background of this layout is always darkened.
        // If you want actual album-art-derived system bar colors like EtherealFlow,
        // you'd need a similar Palette generation logic here.
        val defaultDarkSystemBarColor = Color.Black.copy(alpha = 0.7f) // A semi-transparent dark
        systemUiController.setStatusBarColor(
            color = defaultDarkSystemBarColor,
            darkIcons = defaultDarkSystemBarColor.luminance() > 0.5f // False for dark color
        )
        systemUiController.setNavigationBarColor(
            color = defaultDarkSystemBarColor,
            darkIcons = defaultDarkSystemBarColor.luminance() > 0.5f // False for dark color
        )
        // If you *really* want album-art derived system bars for Immersive,
        // you'd re-add the `produceState` with Palette logic from EtherealFlow here,
        // and use its results for systemUiController.
    }
    // --- END NEW ---

    // Dynamic Blurred Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { controlsVisible = !controlsVisible } // Toggle controls on tap
            }
    ) {
        val backgroundAlpha = 0.6f
        val colorMatrix = remember { ColorMatrix().apply { setToSaturation(0.2f) } }

        AsyncImage(
            model = uiState.currentAudioFile?.albumArtUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = backgroundAlpha,
            colorFilter = ColorFilter.colorMatrix(colorMatrix),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        this.renderEffect = androidx.compose.ui.graphics.BlurEffect(
                            radiusX = 70f,
                            radiusY = 70f,
                        )
                    }
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.9f),
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = screenHeight.value * 0.6f
                    )
                )
        )

        // Main content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Song Info (always visible for now)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = uiState.currentAudioFile?.title ?: "No Song Playing",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = dynamicContentColor, // USE DYNAMIC COLOR
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.currentAudioFile?.artist ?: "Select a song",
                    style = MaterialTheme.typography.titleLarge,
                    color = dynamicContentColor.copy(alpha = 0.8f), // USE DYNAMIC COLOR WITH ALPHA
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Album Art - Larger and centered, potentially interactive
            val albumArtSize = (screenWidth.value * 0.8f).dp.coerceAtMost(380.dp)
            AsyncImage(
                model = uiState.currentAudioFile?.albumArtUri,
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(albumArtSize)
                    .clip(RoundedCornerShape(32.dp))
                    .shadow(elevation = 40.dp, shape = RoundedCornerShape(32.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
            )

            // Playback Controls & Slider (toggle visibility)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        this.alpha = controlsAlpha
                        this.translationY = controlsTranslationY
                    }
            ) {
                // Slider & Timings
                Slider(
                    value = sliderValue,
                    onValueChange = { newValue ->
                        sliderValue = newValue
                        onEvent(PlayerEvent.SetSeeking(true))
                    },
                    onValueChangeFinished = {
                        onEvent(PlayerEvent.SeekTo(sliderValue.toLong()))
                        onEvent(PlayerEvent.SetSeeking(false))
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                    },
                    valueRange = 0f..uiState.totalDurationMs.toFloat().coerceAtLeast(0f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary,
                        inactiveTrackColor = dynamicContentColor.copy(alpha = 0.3f) // USE DYNAMIC COLOR WITH ALPHA
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDuration(uiState.playbackPositionMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = dynamicContentColor.copy(alpha = 0.7f) // USE DYNAMIC COLOR WITH ALPHA
                    )
                    Text(
                        text = formatDuration(uiState.totalDurationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = dynamicContentColor.copy(alpha = 0.7f) // USE DYNAMIC COLOR WITH ALPHA
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                // Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(onClick = {
                        val newShuffleMode = if (uiState.shuffleMode == ShuffleMode.ON) ShuffleMode.OFF else ShuffleMode.ON
                        onEvent(PlayerEvent.SetShuffleMode(newShuffleMode))
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (uiState.shuffleMode == ShuffleMode.ON) MaterialTheme.colorScheme.tertiary else dynamicContentColor.copy(alpha = 0.6f), // USE DYNAMIC COLOR FOR NON-ACTIVE
                            modifier = Modifier.size(32.dp)
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
                            tint = dynamicContentColor, // USE DYNAMIC COLOR
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Play/Pause
                    IconButton(
                        onClick = {
                            onEvent(PlayerEvent.PlayPause)
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(
                            imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onSecondary, // This is on MaterialTheme.colorScheme.secondary, which is a themed color. It should contrast well.
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
                            tint = dynamicContentColor, // USE DYNAMIC COLOR
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Repeat
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
                            tint = if (uiState.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.tertiary else dynamicContentColor.copy(alpha = 0.6f), // USE DYNAMIC COLOR FOR NON-ACTIVE
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Favorite Button (bottom right, subtle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = {
                            uiState.currentAudioFile?.let {
                                if (uiState.isFavorite) {
                                    onEvent(PlayerEvent.RemoveFromFavorites(it.id))
                                } else {
                                    onEvent(PlayerEvent.AddToFavorites(it))
                                }
                            }
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (uiState.isFavorite) "Remove from Favorites" else "Add to Favorites",
                            tint = if (uiState.isFavorite) Color(0xFFFF5252) else dynamicContentColor.copy(alpha = 0.7f), // USE DYNAMIC COLOR FOR NON-FAVORITED
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}