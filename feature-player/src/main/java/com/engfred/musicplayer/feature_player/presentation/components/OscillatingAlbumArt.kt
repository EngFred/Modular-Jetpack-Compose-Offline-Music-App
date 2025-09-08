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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.engfred.musicplayer.feature_player.R
import kotlin.random.Random

@Composable
fun OscillatingAlbumArt(
    albumArtUri: Uri?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    minScale: Float = 0.85f, // Reduced range for less aggressive contraction
    maxScale: Float = 1.15f, // Reduced range for less aggressive expansion
    bassIntensity: Float = 1f, // Simulated bass intensity (0f to 1f)
    oscillationDuration: Int = 600 // Base duration for rhythmic pulse
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "oscillation")
    val colorScheme = MaterialTheme.colorScheme

    // Create multiple independent oscillators with randomized timing for organic effect
    val oscillator1 by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (oscillationDuration * (0.8f + Random.nextFloat() * 0.4f)).toInt(),
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ), label = "oscillator1"
    )

    val oscillator2 by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (oscillationDuration * (0.7f + Random.nextFloat() * 0.6f)).toInt(),
                easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
            ),
            repeatMode = RepeatMode.Reverse
        ), label = "oscillator2"
    )

    val oscillator3 by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (oscillationDuration * (0.9f + Random.nextFloat() * 0.2f)).toInt(),
                easing = CubicBezierEasing(0.2f, 0.0f, 0.4f, 1.0f)
            ),
            repeatMode = RepeatMode.Reverse
        ), label = "oscillator3"
    )

    // Calculate final scale based on oscillators and bass intensity
    val scale = if (isPlaying) {
        minScale + (maxScale - minScale) * bassIntensity *
                ((oscillator1 + oscillator2 + oscillator3) / 3f - minScale) / (maxScale - minScale)
    } else {
        1f
    }

    // Shadow scale for enhanced depth
    val shadowScale by infiniteTransition.animateFloat(
        initialValue = 0.8f, // Adjusted to match reduced main scale
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = oscillationDuration, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "shadowOscillation"
    )

    // Animate border alpha for pulsing ring
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = oscillationDuration, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "borderAlpha"
    )

    // Glow effect for the background
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = oscillationDuration * 2, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "glowEffect"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = if (isPlaying) shadowScale else 1f
                scaleY = if (isPlaying) shadowScale else 1f
                shadowElevation = if (isPlaying) 24.dp.toPx() else 0f // Reduced slightly for balance
                ambientShadowColor = colorScheme.primary.copy(alpha = 0.4f)
                spotShadowColor = colorScheme.primary.copy(alpha = 0.6f)
                shape = CircleShape
                clip = false
            }
            .scale(if (isPlaying) scale else 1f)
            .drawWithCache {
                onDrawWithContent {
                    if (isPlaying) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    colorScheme.primary.copy(alpha = glowAlpha * bassIntensity),
                                    Color.Transparent
                                ),
                                center = Offset(size.width / 2, size.height / 2),
                                radius = size.width / 2 * 1.3f
                            ),
                            radius = size.width / 2 * 1.3f,
                            center = Offset(size.width / 2, size.height / 2)
                        )
                    }
                    drawContent()
                }
            }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring with gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colorScheme.primary.copy(alpha = 0.15f * bassIntensity),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 4.dp,
                    color = colorScheme.onBackground.copy(alpha = if (isPlaying) borderAlpha else 0.1f),
                    shape = CircleShape
                )
        )

        // Concentric pulsing rings
        if (isPlaying) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val maxRadius = size.width / 2

                drawCircle(
                    color = colorScheme.primary.copy(alpha = 0.2f * bassIntensity),
                    center = center,
                    radius = maxRadius * 0.95f * (0.9f + 0.1f * oscillator1),
                    style = Stroke(width = 3.dp.toPx())
                )

                drawCircle(
                    color = colorScheme.primary.copy(alpha = 0.15f * bassIntensity),
                    center = center,
                    radius = maxRadius * 0.85f * (0.92f + 0.08f * oscillator2),
                    style = Stroke(width = 2.dp.toPx())
                )

                drawCircle(
                    color = colorScheme.primary.copy(alpha = 0.1f * bassIntensity),
                    center = center,
                    radius = maxRadius * 0.75f * (0.94f + 0.06f * oscillator3),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        // Album art
        Box(
            modifier = Modifier
                .fillMaxSize(0.9f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Image(
                painter = painterResource(id = androidx.media3.session.R.drawable.media3_icon_album), // Use your app's default
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

        // Central "subwoofer cone" element
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.4f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                colorScheme.primary.copy(alpha = 0.9f),
                                colorScheme.primary.copy(alpha = 0.4f)
                            )
                        )
                    )
                    .scale(0.8f + 0.2f * oscillator1) // Reduced for less aggressive motion
            )
        }
    }
}