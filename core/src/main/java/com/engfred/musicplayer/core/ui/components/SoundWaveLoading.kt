package com.engfred.musicplayer.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/**
 * Premium sound-wave style loading indicator (no shadows).
 *
 * Features:
 * - Sine-based height animation per bar for smooth organic motion
 * - Per-bar speed & phase offsets so the wave looks alive
 * - Animated color shift between two colors (gradient-like feel)
 * - Subtle highlight for depth
 *
 * Usage:
 * SoundWaveLoading(modifier = Modifier.size(140.dp))
 */
@Composable
fun SoundWaveLoading(
    modifier: Modifier = Modifier,
    barCount: Int = 9,
    barWidth: Dp = 6.dp,
    barMaxHeight: Dp = 56.dp,
    minHeightFraction: Float = 0.18f, // smallest bar height as fraction of max
    spacing: Dp = 8.dp,
    baseSpeed: Float = 1.0f, // 1.0 = normal speed, higher = faster
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    cornerRadiusDp: Dp = 6.dp
) {
    val safeBars = max(1, barCount)

    // central animated "mood" that slowly drifts 0..1..0 to drive a color lerp
    val moodTransition = rememberInfiniteTransition()
    val mood by moodTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // per-bar speed + phase offsets
    val barParams = remember(safeBars, baseSpeed) {
        List(safeBars) { index ->
            val speedVariation = 0.85f + ((index % 3) * 0.12f)
            val speed = baseSpeed * speedVariation
            val phase = (index.toFloat() / safeBars.toFloat()) * PI.toFloat()
            Pair(speed, phase)
        }
    }

    val timeTransition = rememberInfiniteTransition()
    val timeProgress by timeTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val semanticsModifier = modifier.semantics {
        contentDescription = "Loading music — please wait"
    }

    Canvas(modifier = semanticsModifier) {
        val bwPx = barWidth.toPx()
        val maxH = barMaxHeight.toPx()
        val spacingPx = spacing.toPx()
        val cornerRadius = cornerRadiusDp.toPx()

        val totalWidth = safeBars * bwPx + (safeBars - 1) * spacingPx
        val startX = (size.width - totalWidth) / 2f
        val centerY = size.height / 2f

        val animValues = barParams.map { (speed, phase) ->
            val t = (timeProgress * 2f * PI.toFloat() * speed) + phase
            val raw = (sin(t) * 0.9f).toFloat()
            val normalized = (raw + 0.9f) / 1.8f
            val eased = normalized * normalized * (3 - 2 * normalized) // smoothstep
            eased.coerceIn(0f, 1f)
        }

        for (i in 0 until safeBars) {
            val progress = animValues[i]
            val barHeight =
                (minHeightFraction * maxH) + progress * (maxH - minHeightFraction * maxH)
            val left = startX + i * (bwPx + spacingPx)
            val top = centerY - barHeight / 2f

            val xPhase =
                (sin(((timeProgress + i * 0.03f) * 2f * PI.toFloat())).toFloat()) * (bwPx * 0.08f)

            val colorMixAmount =
                (mood + (i.toFloat() / safeBars.toFloat()) * 0.12f).coerceIn(0f, 1f)
            val barColor = lerp(primaryColor, secondaryColor, colorMixAmount)

            // Draw main bar
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left + xPhase, top),
                size = Size(bwPx, barHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )

            // Subtle highlight for depth
            rotate(degrees = -5f, pivot = Offset(left + bwPx / 2f, top + barHeight / 4f)) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.05f),
                    topLeft = Offset(left + xPhase + 1f, top + barHeight * 0.04f),
                    size = Size(bwPx * 0.85f, barHeight * 0.12f),
                    cornerRadius = CornerRadius(cornerRadius * 0.6f, cornerRadius * 0.6f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SoundWaveLoadingPreview() {
    SoundWaveLoading(
        modifier = Modifier.size(160.dp),
        barCount = 9,
        barWidth = 6.dp,
        barMaxHeight = 56.dp,
        spacing = 8.dp,
        baseSpeed = 1.0f,
        primaryColor = Color(0xFF26A69A),
        secondaryColor = Color(0xFF7C4DFF),
        cornerRadiusDp = 8.dp
    )
}
