package com.engfred.musicplayer.feature_equalizer.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2

@Composable
fun RotaryKnob(
    modifier: Modifier = Modifier,
    value: Float, // Normalized value from 0f to 1f
    onValueChange: (Float) -> Unit,
    knobSize: Dp = 80.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
    knobColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    indicatorColor: Color = MaterialTheme.colorScheme.onPrimary,
    strokeWidth: Dp = 8.dp,
    indicatorLength: Dp = 10.dp,
    initialAngleOffset: Float = 135f, // Start bottom-left
    arcLength: Float = 270f // Sweep 270 degrees
) {
    var cumulativeAngle by remember { mutableFloatStateOf(value * arcLength + initialAngleOffset) }

    Box(
        modifier = modifier
            .size(knobSize)
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val angle = atan2(
                            offset.y - centerY,
                            offset.x - centerX
                        ) * (180f / Math.PI.toFloat())
                        // Initialize dragStartAngle to current knob angle
                        dragStartAngle = angle - cumulativeAngle
                    },
                    onDrag = { change, _ ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val currentTouchAngle = atan2(
                            change.position.y - centerY,
                            change.position.x - centerX
                        ) * (180f / Math.PI.toFloat())

                        // Calculate the angle delta relative to the initial drag angle
                        var angleDelta = currentTouchAngle - dragStartAngle - cumulativeAngle
                        // Handle angle wrap-around
                        if (angleDelta > 180f) angleDelta -= 360f
                        if (angleDelta < -180f) angleDelta += 360f

                        // Update cumulative angle
                        cumulativeAngle += angleDelta
                        // Calculate new value based on the cumulative angle
                        val newValue = ((cumulativeAngle - initialAngleOffset) / arcLength)
                            .coerceIn(0f, 1f)

                        if (newValue != value) {
                            onValueChange(newValue)
                        }

                        // Update dragStartAngle for the next drag event
                        dragStartAngle = currentTouchAngle - cumulativeAngle
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) - strokeWidth.toPx() / 2f - indicatorLength.toPx() / 2f

            // Draw inactive track
            drawArc(
                color = inactiveColor,
                startAngle = initialAngleOffset,
                sweepAngle = arcLength,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )

            // Draw active track
            drawArc(
                color = activeColor,
                startAngle = initialAngleOffset,
                sweepAngle = value * arcLength,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )

            // Draw inner knob circle
            drawCircle(
                color = knobColor,
                radius = radius - strokeWidth.toPx() / 2f,
                center = center
            )

            // Draw indicator line
            rotate(degrees = cumulativeAngle) {
                drawLine(
                    color = indicatorColor,
                    start = Offset(center.x + radius + strokeWidth.toPx() / 2f - indicatorLength.toPx(), center.y),
                    end = Offset(center.x + radius + strokeWidth.toPx() / 2f + indicatorLength.toPx(), center.y),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

// Maintain dragStartAngle as a global variable to persist across drag events
private var dragStartAngle by mutableFloatStateOf(0f)