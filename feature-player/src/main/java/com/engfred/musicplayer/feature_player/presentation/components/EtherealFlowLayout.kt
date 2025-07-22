package com.engfred.musicplayer.feature_player.presentation.components

import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.annotation.OptIn
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.engfred.musicplayer.core.domain.model.repository.RepeatMode
import com.engfred.musicplayer.core.domain.model.repository.ShuffleMode
import com.engfred.musicplayer.core.util.formatDuration
import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerEvent
import com.engfred.musicplayer.core.domain.model.repository.PlaybackState
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.luminance // Ensure this is imported for Color.luminance()
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
@Composable
fun EtherealFlowLayout(
    uiState: PlaybackState,
    onEvent: (PlayerEvent) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(uiState.playbackPositionMs.toFloat()) }
    val view = LocalView.current // For haptic feedback
    val context = LocalContext.current // For Coil ImageLoader and Palette

    val gradientColors by produceState(
        initialValue = listOf(Color.Black, Color.DarkGray), // Default fallback colors
        uiState.currentAudioFile?.albumArtUri // Key to re-run on new album art
    ) {
        val uri = uiState.currentAudioFile?.albumArtUri
        if (uri == null) {
            value = listOf(Color.Black, Color.DarkGray) // Fallback for no album art
            return@produceState
        }

        val loader = context.imageLoader
        val request = ImageRequest.Builder(context)
            .data(uri)
            .allowHardware(false) // Required for Palette to work correctly on some GPUs
            .build()

        withContext(Dispatchers.IO) { // Perform image loading and palette generation on IO thread
            try {
                val bitmapDrawable = loader.execute(request).drawable as? BitmapDrawable
                val bitmap = bitmapDrawable?.bitmap
                if (bitmap != null) {
                    Palette.from(bitmap).generate { palette ->
                        val dominantColor = palette?.dominantSwatch?.rgb?.let { Color(it) }
                            ?: Color.Black
                        val darkVibrantColor = palette?.darkVibrantSwatch?.rgb?.let { Color(it) }
                            ?: dominantColor.copy(alpha = 0.8f) // Fallback to dominant with alpha
                        val darkMutedColor = palette?.darkMutedSwatch?.rgb?.let { Color(it) }
                            ?: darkVibrantColor.copy(alpha = 0.7f) // Fallback from darkVibrant

                        // Create a gradient using a selection of colors, preferring darker/muted tones
                        val colors = listOf(
                            darkVibrantColor.copy(alpha = 0.9f),
                            darkMutedColor.copy(alpha = 0.8f),
                            dominantColor.copy(alpha = 0.7f)
                        ).distinct() // Remove duplicates
                            .sortedBy { color -> color.luminance() }
                            .take(2) // Take the two darkest for a smooth vertical gradient

                        value = if (colors.size >= 2) colors else listOf(Color.Black, Color.DarkGray)
                    }
                } else {
                    value = listOf(Color.Black, Color.DarkGray)
                }
            } catch (e: Exception) {
                // Log error if image loading or palette generation fails
                println("Error generating palette from album art: ${e.message}")
                value = listOf(Color.Black, Color.DarkGray) // Fallback on error
            }
        }
    }


    LaunchedEffect(uiState.playbackPositionMs, uiState.isSeeking) {
        if (!uiState.isSeeking) {
            sliderValue = uiState.playbackPositionMs.toFloat()
        }
    }

    val systemUiController = rememberSystemUiController()
    LaunchedEffect(gradientColors) {
        val statusBarColor = gradientColors.firstOrNull() ?: Color.Black
        val navigationBarColor = gradientColors.lastOrNull() ?: Color.Black

        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = statusBarColor.luminance() > 0.5f // Dark icons for light colors
        )

        systemUiController.setNavigationBarColor(
            color = navigationBarColor,
            darkIcons = navigationBarColor.luminance() > 0.5f // Dark icons for light colors
        )
    }

    // --- NEW: Dynamic content color based on the current background ---
    val dynamicContentColor by remember(gradientColors) {
        val topGradientColor = gradientColors.firstOrNull() ?: Color.Black
        // If the background color is light, use black text/icons, otherwise use white
        val chosenColor = if (topGradientColor.luminance() > 0.5f) Color.Black else Color.White
        mutableStateOf(chosenColor)
    }
    // --- END NEW ---


    // Dynamic Gradient Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors))
    ) {
        // Main content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Placeholder / Future Top Bar
            Spacer(modifier = Modifier.height(0.dp))

            // Album Art Display
            val albumArtScale by animateFloatAsState(
                targetValue = if (uiState.isPlaying) 1.02f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
                label = "albumArtScale"
            )
            AsyncImage(
                model = uiState.currentAudioFile?.albumArtUri,
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(300.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(elevation = 32.dp, shape = RoundedCornerShape(24.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                    .graphicsLayer {
                        scaleX = albumArtScale
                        scaleY = albumArtScale
                    }
            )

            // Song Info & Favorite
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
            ) {
                Text(
                    text = uiState.currentAudioFile?.title ?: "Unknown Title",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = dynamicContentColor, // USE DYNAMIC COLOR
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.currentAudioFile?.artist ?: "Unknown Artist",
                    style = MaterialTheme.typography.titleLarge,
                    color = dynamicContentColor.copy(alpha = 0.7f), // USE DYNAMIC COLOR WITH ALPHA
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Favorite Button with Animation
                val favoriteScale by animateFloatAsState(
                    targetValue = if (uiState.isFavorite) 1.2f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "favoriteScale"
                )
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
                        tint = if (uiState.isFavorite) Color(0xFFF44336) else dynamicContentColor.copy(alpha = 0.8f), // USE DYNAMIC COLOR FOR NON-FAVORITED
                        modifier = Modifier.graphicsLayer {
                            scaleX = favoriteScale
                            scaleY = favoriteScale
                        }
                    )
                }
            }


            // Playback Slider & Timings
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
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
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
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
            }


            // Playback Controls
            // Frosted Glassmorphism background for controls (NO BLUR)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .graphicsLayer {
                        // Blur removed as per previous request
                    }
                    .padding(vertical = 16.dp, horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle Button
                    IconButton(onClick = {
                        val newShuffleMode = if (uiState.shuffleMode == ShuffleMode.ON) ShuffleMode.OFF else ShuffleMode.ON
                        onEvent(PlayerEvent.SetShuffleMode(newShuffleMode))
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (uiState.shuffleMode == ShuffleMode.ON) MaterialTheme.colorScheme.secondary else dynamicContentColor.copy(alpha = 0.7f), // USE DYNAMIC COLOR FOR NON-ACTIVE
                            modifier = Modifier.size(36.dp)
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
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    // Play/Pause Button
                    val playPauseScale by animateFloatAsState(
                        targetValue = if (uiState.isPlaying) 1f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "playPauseScale"
                    )
                    IconButton(
                        onClick = {
                            onEvent(PlayerEvent.PlayPause)
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer {
                                scaleX = playPauseScale
                                scaleY = playPauseScale
                            }
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary, // Keep static as it's on a themed background
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
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    // Repeat Button
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
                            tint = if (uiState.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.secondary else dynamicContentColor.copy(alpha = 0.7f), // USE DYNAMIC COLOR FOR NON-ACTIVE
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }
}