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
import kotlinx.coroutines.delay
import kotlin.math.min

@Composable
fun OscillatingAlbumArt(
    albumArtUri: Uri?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    minScale: Float = 0.92f,
    maxScale: Float = 1.08f,
    bassIntensity: Float = 0f, // Beat-driven intensity (0f to 1f)
    estimatedBpm: Float = 120f // Estimated BPM from BeatDetector
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var boxSize by remember { mutableStateOf(Size.Zero) } // Capture Box size

    // NEW: State to track the current beat phase for more authentic movement
    var beatPhase by remember { mutableIntStateOf(0) }

    // NEW: Use a more physical spring animation that responds to bass intensity
    val targetScale by remember(bassIntensity, isPlaying) {
        derivedStateOf {
            if (!isPlaying) {
                1f // No animation when not playing
            } else {
                // Map bass intensity to scale with more nuanced response
                // Creates the "small to medium to large" scaling effect you described
                val intensityAdjusted = min(1f, bassIntensity * 1.5f) // Allow slight overshoot for strong beats
                minScale + (maxScale - minScale) * intensityAdjusted
            }
        }
    }

    // NEW: Smoother spring animation that mimics physical speaker movement
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "subwooferScale"
    )

    // NEW: Dynamic effects that pulse with the beat rather than a fixed animation
    val pulseIntensity by remember(bassIntensity) {
        derivedStateOf {
            // Only show effects when there's actual bass detected
            if (bassIntensity > 0.1f) bassIntensity else 0f
        }
    }

    // NEW: Beat-synced effects - only activate when bass is detected
    val borderAlpha by remember(pulseIntensity) {
        derivedStateOf { 0.3f + 0.7f * pulseIntensity }
    }

    val borderWidth by remember(pulseIntensity) {
        derivedStateOf { 4f + 8f * pulseIntensity }
    }

    val glowAlpha by remember(pulseIntensity) {
        derivedStateOf { 0.3f + 0.6f * pulseIntensity }
    }

    // NEW: Use LaunchedEffect to track beat phases for more authentic movement
    LaunchedEffect(isPlaying, estimatedBpm) {
        if (!isPlaying) {
            beatPhase = 0
            return@LaunchedEffect
        }

        // Calculate beat interval based on BPM
        val beatIntervalMs = (60000f / estimatedBpm).toLong().coerceIn(100, 2000)

        while (isPlaying) {
            delay(beatIntervalMs)
            beatPhase = (beatPhase + 1) % 4 // 4-phase cycle for variety
        }
    }

    // NEW: Add subtle secondary oscillation that follows beat phases
    val secondaryOscillation by infiniteRepeatableAnimation(
        initialValue = 0f,
        targetValue = 1f,
        durationMillis = if (estimatedBpm > 60) (60000 / estimatedBpm).toInt() else 1000,
        isPlaying = isPlaying
    )

    // NEW: Combined scale for more organic movement
    val combinedScale = animatedScale * (0.998f + 0.004f * secondaryOscillation)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = combinedScale
                scaleY = combinedScale
                shadowElevation = if (isPlaying) (4f + 8f * pulseIntensity).dp.toPx() else 0f
                ambientShadowColor = colorScheme.primary.copy(alpha = 0.3f * pulseIntensity)
                spotShadowColor = colorScheme.primary.copy(alpha = 0.5f * pulseIntensity)
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
        // Outer pulsing ring - only visible during bass hits
        if (pulseIntensity > 0.1f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colorScheme.primary.copy(alpha = glowAlpha),
                                Color.Transparent
                            ),
                            center = Offset(0.5f, 0.5f),
                            radius = maxOf(1f, boxSize.width * 0.7f)
                        )
                    )
                    .border(
                        width = borderWidth.dp,
                        color = colorScheme.primary.copy(alpha = borderAlpha),
                        shape = CircleShape
                    )
            )
        }

        // Inner pulsing ring - follows the beat more closely
        if (pulseIntensity > 0.2f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val maxRadius = size.width / 2
                drawCircle(
                    color = colorScheme.primary.copy(alpha = 0.4f * pulseIntensity),
                    center = center,
                    radius = maxRadius * 0.9f,
                    style = Stroke(width = (2f + 4f * pulseIntensity).dp.toPx())
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

// NEW: Helper function for secondary oscillations
@Composable
fun infiniteRepeatableAnimation(
    initialValue: Float,
    targetValue: Float,
    durationMillis: Int,
    isPlaying: Boolean
): State<Float> {
    val infiniteTransition = rememberInfiniteTransition(label = "secondaryOscillation")

    return if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = initialValue,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = durationMillis,
                    easing = EaseInOutSine
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "secondaryBeat"
        )
    } else {
        remember { derivedStateOf { initialValue } }
    }
}