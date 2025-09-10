package com.engfred.musicplayer.feature_player.presentation.components

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

@Composable
fun AlbumArtDisplay(
    albumArtUri: Any?,
    isPlaying: Boolean,
    windowWidthSizeClass: WindowWidthSizeClass,
    playerLayout: PlayerLayout,
    modifier: Modifier = Modifier
) {
    if (playerLayout == PlayerLayout.ETHEREAL_FLOW) {
        // ---- ETHEREAL_FLOW only ----
        // Increased sizes
        val compactBaseSize = 340.dp   // was 280.dp
        val compactPausedSize = compactBaseSize * 0.7f  // was 0.8f
        val mediumBaseSize = 400.dp   // was 320.dp
        val expandedBaseSize = 460.dp // was 380.dp

        // Fixed container to prevent layout from shrinking
        val fixedContainerSize = when (windowWidthSizeClass) {
            WindowWidthSizeClass.Compact -> compactBaseSize
            WindowWidthSizeClass.Medium -> mediumBaseSize
            WindowWidthSizeClass.Expanded -> expandedBaseSize
            else -> compactBaseSize
        }

        // Inner animated size (only for compact layouts)
        val targetSize: Dp = when {
            windowWidthSizeClass == WindowWidthSizeClass.Compact -> if (isPlaying) compactBaseSize else compactPausedSize
            else -> compactPausedSize // Always paused size for Medium & Expanded
        }

        val animatedAlbumArtSize by animateDpAsState(
            targetValue = targetSize,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "etherealFlowAlbumArtSize"
        )

        val albumArtShadowElevation by animateDpAsState(
            targetValue = if (windowWidthSizeClass == WindowWidthSizeClass.Compact && isPlaying) 36.dp else 20.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
            label = "etherealFlowAlbumArtShadowElevation"
        )

        Box(
            modifier = modifier.size(fixedContainerSize), // Fixed container prevents layout shift
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(animatedAlbumArtSize) // Animate only the inner album art
                    .clip(RoundedCornerShape(12.dp))
                    .shadow(
                        elevation = albumArtShadowElevation,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = Color.Black.copy(alpha = 0.4f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                CoilImage(
                    imageModel = { albumArtUri },
                    imageOptions = ImageOptions(
                        contentDescription = "Album Art",
                        contentScale = ContentScale.FillBounds
                    ),
                    modifier = Modifier.fillMaxSize(),
                    failure = {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = "No Album Art",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    loading = {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = "Loading Album Art",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                )
            }
        }
    } else {
        // ---- All other layouts remain unchanged ----
        val baseNonImmersiveAlbumArtSize = when (windowWidthSizeClass) {
            WindowWidthSizeClass.Compact -> 240.dp
            WindowWidthSizeClass.Medium -> 280.dp
            WindowWidthSizeClass.Expanded -> 320.dp
            else -> 240.dp
        }

        val albumArtSizePlaying: Dp = baseNonImmersiveAlbumArtSize
        val albumArtSizePaused: Dp = baseNonImmersiveAlbumArtSize * 0.8f

        val animatedNonImmersiveAlbumArtSize by animateDpAsState(
            targetValue = if (isPlaying) albumArtSizePlaying else albumArtSizePaused,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "animatedNonImmersiveAlbumArtSize"
        )

        val albumArtShadowElevation by animateDpAsState(
            targetValue = if (isPlaying) 32.dp else 16.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
            label = "albumArtShadowElevation"
        )

        BoxWithConstraints(
            modifier = modifier.size(baseNonImmersiveAlbumArtSize)
        ) {
            val currentBoxSize = if (playerLayout == PlayerLayout.IMMERSIVE_CANVAS) {
                minOf(maxWidth, maxHeight)
            } else {
                animatedNonImmersiveAlbumArtSize
            }

            Box(
                modifier = Modifier
                    .then(
                        if (playerLayout != PlayerLayout.IMMERSIVE_CANVAS) {
                            Modifier
                                .size(currentBoxSize)
                                .align(Alignment.Center)
                        } else {
                            Modifier.fillMaxSize()
                        }
                    )
                    .clip(
                        if (playerLayout == PlayerLayout.IMMERSIVE_CANVAS) RoundedCornerShape(0.dp)
                        else RoundedCornerShape(24.dp)
                    )
                    .shadow(
                        elevation = if (playerLayout == PlayerLayout.IMMERSIVE_CANVAS) 0.dp else albumArtShadowElevation,
                        shape = if (playerLayout == PlayerLayout.IMMERSIVE_CANVAS) RoundedCornerShape(0.dp)
                        else RoundedCornerShape(24.dp),
                        ambientColor = Color.Black.copy(alpha = 0.4f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    .background(color = Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                CoilImage(
                    imageModel = { albumArtUri },
                    imageOptions = ImageOptions(
                        contentDescription = "Album Art",
                        contentScale = ContentScale.FillBounds
                    ),
                    modifier = Modifier.fillMaxSize(),
                    failure = {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = "No Album Art",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    loading = {
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
}


