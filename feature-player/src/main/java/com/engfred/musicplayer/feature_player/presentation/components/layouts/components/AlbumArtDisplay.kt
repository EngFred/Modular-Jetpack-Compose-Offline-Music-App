package com.engfred.musicplayer.feature_player.presentation.components.layouts.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.core.domain.model.PlayerLayout
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage

/**
 * Displays the album art for the current song, adapting its size, shape, and shadow
 * based on the playback state, window size, and selected player layout.
 *
 * @param albumArtUri The URI of the album art to display. Can be null.
 * @param isPlaying Boolean indicating if the song is currently playing.
 * @param windowWidthSizeClass The current window size class.
 * @param playerLayout The currently active player layout.
 * @param modifier The modifier to be applied to the Album Art display.
 */
@Composable
fun AlbumArtDisplay(
    albumArtUri: Any?, // Can be Uri, String, or null
    isPlaying: Boolean,
    windowWidthSizeClass: WindowWidthSizeClass,
    playerLayout: PlayerLayout,
    modifier: Modifier = Modifier
) {
    // Define base size for non-immersive layouts (Ethereal Flow, Minimalist Groove)
    val baseNonImmersiveAlbumArtSize = when (windowWidthSizeClass) {
        WindowWidthSizeClass.Compact -> 240.dp
        WindowWidthSizeClass.Medium -> 280.dp
        WindowWidthSizeClass.Expanded -> 320.dp
        else -> 240.dp
    }

    // Determine the target size for animation based on playback state for non-immersive layouts
    val albumArtSizePlaying: Dp = baseNonImmersiveAlbumArtSize
    val albumArtSizePaused: Dp = baseNonImmersiveAlbumArtSize * 0.8f

    // Animate size only for non-immersive layouts where it scales with playback
    val animatedNonImmersiveAlbumArtSize by animateDpAsState(
        targetValue = if (isPlaying) albumArtSizePlaying else albumArtSizePaused,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "animatedNonImmersiveAlbumArtSize"
    )

    // Animate shadow elevation based on playback state for non-immersive layouts
    val albumArtShadowElevation by animateDpAsState(
        targetValue = if (isPlaying) 32.dp else 16.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "albumArtShadowElevation"
    )

    BoxWithConstraints(
        modifier = modifier // Apply the external modifier here to control overall placement/sizing
    ) {
        // Determine the actual size of the Box that holds the album art
        val currentBoxSize = if (playerLayout == PlayerLayout.IMMERSIVE_CANVAS) {
            // For ImmersiveCanvas, the size is controlled by external modifiers like fillMaxSize / aspectRatio
            // This 'size' is primarily for the content's aspect ratio if needed, but not for fixed size of the Box itself.
            minOf(maxWidth, maxHeight) // Use available space, but ensure it's not excessively large
        } else {
            // For other layouts, use the animated size
            minOf(animatedNonImmersiveAlbumArtSize, maxWidth, maxHeight)
        }

        Box(
            modifier = Modifier
                .then(
                    if (playerLayout != PlayerLayout.IMMERSIVE_CANVAS) {
                        // Apply fixed size for non-Immersive layouts
                        Modifier.size(currentBoxSize)
                    } else {
                        // For ImmersiveCanvas, the parent modifier should handle size (e.g., fillMaxWidth, aspectRatio)
                        Modifier.fillMaxSize() // Fill its parent BoxWithConstraints
                    }
                )
                .align(Alignment.Center) // Center this Box within BoxWithConstraints
                .clip(
                    if (playerLayout == PlayerLayout.IMMERSIVE_CANVAS) {
                        RoundedCornerShape(0.dp) // Rectangular for full backdrop
                    } else {
                        RoundedCornerShape(24.dp) // Rounded corners for other layouts
                    }
                )
                .shadow(
                    elevation = if (playerLayout == PlayerLayout.IMMERSIVE_CANVAS) 0.dp else albumArtShadowElevation,
                    shape = if (playerLayout == PlayerLayout.IMMERSIVE_CANVAS) {
                        RoundedCornerShape(0.dp) // Rectangular shadow for immersive
                    } else {
                        RoundedCornerShape(24.dp) // Rounded shadow for other layouts
                    },
                    ambientColor = Color.Black.copy(alpha = 0.4f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)), // Placeholder background
            contentAlignment = Alignment.Center // Center content (image or icon) inside this Box
        ) {
            CoilImage(
                imageModel = { albumArtUri },
                imageOptions = ImageOptions(
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop // Crop to fill the bounds
                ),
                modifier = Modifier.fillMaxSize(), // Image fills the Box
                failure = {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = "No Album Art Available",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) // Adjust tint for better visibility
                    )
                },
                loading = {
                    // Display a placeholder icon while loading
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = "Loading Album Art",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            )
        }
    }
}