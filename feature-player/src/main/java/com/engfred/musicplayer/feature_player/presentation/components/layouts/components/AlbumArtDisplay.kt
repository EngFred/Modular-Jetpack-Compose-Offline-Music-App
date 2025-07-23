package com.engfred.musicplayer.feature_player.presentation.components.layouts.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
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
import coil.compose.AsyncImage
import com.engfred.musicplayer.feature_player.domain.model.PlayerLayout // Import PlayerLayout
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage

@Composable
fun AlbumArtDisplay(
    albumArtUri: Any?, // Can be Uri, String, or null
    isPlaying: Boolean,
    windowWidthSizeClass: WindowWidthSizeClass,
    playerLayout: PlayerLayout, // Changed from isImmersiveCanvas: Boolean
    modifier: Modifier = Modifier // Allow external modifiers to be passed
) {
    // Determine base size based on window size class and layout type
    val baseAlbumArtSize = when (windowWidthSizeClass) {
        WindowWidthSizeClass.Compact -> if (playerLayout == PlayerLayout.IMMERSIVE_CANVAS) 300.dp else 240.dp // Larger for immersive
        WindowWidthSizeClass.Medium -> if (playerLayout == PlayerLayout.IMMERSIVE_CANVAS) 400.dp else 280.dp
        WindowWidthSizeClass.Expanded -> if (playerLayout == PlayerLayout.IMMERSIVE_CANVAS) 500.dp else 320.dp
        else -> 240.dp
    }

    val albumArtSizePlaying: Dp = baseAlbumArtSize
    val albumArtSizePaused: Dp = baseAlbumArtSize * 0.8f

    // Conditional animation for size based on playerLayout
    val animatedAlbumArtSize by animateDpAsState(
        targetValue = if (playerLayout == PlayerLayout.IMMERSIVE_CANVAS) baseAlbumArtSize else (if (isPlaying) albumArtSizePlaying else albumArtSizePaused),
        animationSpec = if (playerLayout == PlayerLayout.IMMERSIVE_CANVAS) spring(stiffness = Spring.StiffnessMediumLow) else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "animatedAlbumArtSize"
    )

    // Shadow elevation is 0.dp for ImmersiveCanvas
    val albumArtShadowElevation by animateDpAsState(
        targetValue = if (playerLayout == PlayerLayout.IMMERSIVE_CANVAS) 0.dp else (if (isPlaying) 32.dp else 16.dp),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "albumArtShadowElevation"
    )

    BoxWithConstraints(
        modifier = modifier // Apply the external modifier here
    ) {
        val size = if (playerLayout == PlayerLayout.IMMERSIVE_CANVAS) {
            // For ImmersiveCanvas, we use fillMaxWidth and fillMaxHeight(0.5f) directly
            // so this size calculation is mainly for the internal Box content aspect ratio.
            minOf(maxWidth, maxHeight) // Ensure it doesn't exceed bounds
        } else {
            minOf(animatedAlbumArtSize, maxWidth, maxHeight)
        }


        Box(
            modifier = Modifier
                .then(
                    if (playerLayout == PlayerLayout.IMMERSIVE_CANVAS) {
                        Modifier // No fixed size here, parent modifier handles it (e.g., fillMaxSize, aspect ratio)
                    } else {
                        Modifier.size(size) // Fixed size for EtherealFlow
                    }
                )
                .align(Alignment.Center) // Center this Box within BoxWithConstraints
                // --- Start of Changes for ImmersiveCanvasLayout ---
                .clip(
                    if (playerLayout == PlayerLayout.IMMERSIVE_CANVAS) {
                        // No clipping for ImmersiveCanvas to make it a full backdrop
                        // You can use RectangleShape or just not apply clip directly
                        // Using a 0.dp RoundedCornerShape effectively makes it a rectangle.
                        RoundedCornerShape(0.dp)
                    } else {
                        RoundedCornerShape(24.dp) // Keep rounded for EtherealFlow
                    }
                )
                .shadow(
                    elevation = albumArtShadowElevation, // Now controlled by animatedAlbumArtShadowElevation
                    // Shape for shadow should also be rectangular for immersive
                    shape = if (playerLayout == PlayerLayout.IMMERSIVE_CANVAS) {
                        RoundedCornerShape(0.dp) // Rectangular shadow for immersive
                    } else {
                        RoundedCornerShape(24.dp) // Rounded shadow for EtherealFlow
                    },
                    ambientColor = Color.Black.copy(alpha = 0.4f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
                // --- End of Changes ---
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center // Center content (image or icon) inside this Box
        ) {

            CoilImage(
                imageModel = { albumArtUri },
                imageOptions = ImageOptions(
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop
                ),
                modifier = Modifier
                    .fillMaxSize(),
                failure = {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = "No Album Art",
                    )
                },
                loading = {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = "No Album Art",
                    )
                }
            )
        }
    }
}