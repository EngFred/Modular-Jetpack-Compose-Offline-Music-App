package com.engfred.musicplayer.feature_player.presentation.components

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun OscillatingAlbumArt(
    albumArtUri: Uri?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    minScale: Float = 0.7f, // Pronounced contraction
    maxScale: Float = 1.3f, // Pronounced expansion
    bassIntensity: Float = 0f, // Beat-driven intensity (0f to 1f)
    estimatedBpm: Float = 120f // Estimated BPM from BeatDetector
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "oscillation")
    val colorScheme = MaterialTheme.colorScheme
    var boxSize by remember { mutableStateOf(Size.Zero) } // Capture Box size

    // Calculate animation duration based on BPM (milliseconds per beat)
    val beatDurationMs = (60000f / estimatedBpm).toInt().coerceIn(200, 1000) // Limit to 60-300 BPM

    // Single oscillator synced to estimated BPM
    val scaleAnimation by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = beatDurationMs,
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ), label = "beatScale"
    )

    // Calculate final scale based on bassIntensity
    val scale = if (isPlaying) {
        if (bassIntensity > 0.1f) { // Threshold to avoid noise in quiet sections
            minScale + (maxScale - minScale) * bassIntensity // Emphasize beat-driven scaling
        } else {
            // Stronger fallback animation during quiet sections
            minScale + (maxScale - minScale) * 0.5f * scaleAnimation
        }
    } else {
        1f
    }

    // Border alpha pulses with beat
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f, // Bolder pulsing for visibility
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = beatDurationMs, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "borderAlpha"
    )

    // Dynamic border width scales with beat
    val borderWidth by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 12f, // Wider for more prominent wave-like effect
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = beatDurationMs, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "borderWidth"
    )

    // Glow effect synced to beat
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f, // Stronger glow for wave-like effect
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = beatDurationMs, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "glowEffect"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = if (isPlaying) 8.dp.toPx() else 0f
                ambientShadowColor = colorScheme.primary.copy(alpha = 0.4f)
                spotShadowColor = colorScheme.primary.copy(alpha = 0.6f)
                shape = CircleShape
                clip = false
            }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .onSizeChanged { size ->
                boxSize = Size(size.width.toFloat(), size.height.toFloat())
            },
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colorScheme.primary.copy(alpha = glowAlpha * bassIntensity.coerceAtLeast(0.4f)),
                            Color.Transparent
                        ),
                        radius = maxOf(1f, 1.2f * boxSize.width) // Ensure positive radius
                    )
                )
                .border(
                    width = (borderWidth * bassIntensity.coerceAtLeast(0.4f)).dp, // Dynamic width
                    color = colorScheme.onBackground.copy(alpha = if (isPlaying) borderAlpha else 0.2f),
                    shape = CircleShape
                )
        )

        // Inner pulsing ring
        if (isPlaying) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val maxRadius = size.width / 2
                drawCircle(
                    color = colorScheme.primary.copy(alpha = 0.6f * bassIntensity.coerceAtLeast(0.4f)), // Stronger opacity
                    center = center,
                    radius = maxRadius * scale, // Sync radius with main scale
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // Album art
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Image(
                painter = painterResource(id = androidx.media3.session.R.drawable.media3_icon_album),
                contentDescription = "Default album art",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (albumArtUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(albumArtUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album Art",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}