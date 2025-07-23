package com.engfred.musicplayer.feature_equalizer.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A premium equalizer band sliders Composable with enhanced visuals and interactions.
 *
 * @param numberOfBands Number of frequency bands.
 * @param bandLevels Map of band index to gain level.
 * @param bandLevelRange Min and max gain levels.
 * @param getCenterFrequency Function to get the center frequency for a band.
 * @param onBandLevelChange Callback when a band's gain changes.
 */
@Composable
fun EqualizerBandSliders(
    numberOfBands: Short,
    bandLevels: Map<Short, Short>,
    bandLevelRange: Pair<Short, Short>,
    getCenterFrequency: (Short) -> Int,
    onBandLevelChange: (bandIndex: Short, gain: Short) -> Unit
) {
    val (minLevel, maxLevel) = bandLevelRange
    val zeroLevel = 0.toShort()
    val colorScheme = MaterialTheme.colorScheme
    val hapticFeedback = LocalHapticFeedback.current

    if (numberOfBands > 0 && minLevel != maxLevel) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp,
                pressedElevation = 4.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.surface.copy(alpha = 0.95f)
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                colorScheme.surface.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Equalizer",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp) // Increased height for taller sliders
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxHeight(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 0 until numberOfBands) {
                                val bandIndex = i.toShort()
                                val currentLevel = bandLevels[bandIndex] ?: zeroLevel
                                val bandFrequency = getCenterFrequency(bandIndex)
                                val interactionSource = remember { MutableInteractionSource() }
                                val isDragging by interactionSource.collectIsDraggedAsState()

                                // Animate slider value
                                val animatedLevel by animateFloatAsState(
                                    targetValue = currentLevel.toFloat(),
                                    animationSpec = spring(
                                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                    ),
                                    label = "sliderLevel_$bandIndex"
                                )

                                // Scale effect for interaction
                                val scale by animateFloatAsState(
                                    targetValue = if (isDragging) 1.05f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                                    ),
                                    label = "sliderScale_$bandIndex"
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .width(64.dp)
                                        .fillMaxHeight()
                                        .scale(scale)
                                        .padding(horizontal = 4.dp)
                                ) {
                                    // dB Label
                                    Text(
                                        text = "${String.format("%.1f", currentLevel / 100f)} dB",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = when {
                                            currentLevel > zeroLevel -> Color.Green.copy(alpha = 0.9f)
                                            currentLevel < zeroLevel -> colorScheme.error.copy(alpha = 0.9f)
                                            else -> colorScheme.onSurface
                                        },
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )

                                    // Reduced spacer
                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Vertical Slider
                                    Slider(
                                        value = animatedLevel,
                                        onValueChange = { newValue ->
                                            onBandLevelChange(bandIndex, newValue.roundToInt().toShort())
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                        valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                                        steps = if (maxLevel > minLevel) (maxLevel - minLevel - 1) else 0,
                                        colors = SliderDefaults.colors(
                                            thumbColor = when {
                                                currentLevel > zeroLevel -> Color.Green.copy(alpha = 0.9f)
                                                currentLevel < zeroLevel -> colorScheme.error.copy(alpha = 0.9f)
                                                else -> colorScheme.primary
                                            },
                                            activeTrackColor = when {
                                                currentLevel > zeroLevel -> Color.Green.copy(alpha = 0.7f)
                                                currentLevel < zeroLevel -> colorScheme.error.copy(alpha = 0.7f)
                                                else -> colorScheme.primary.copy(alpha = 0.7f)
                                            },
                                            inactiveTrackColor = colorScheme.onSurface.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f) // Prioritize slider height
                                            .rotate(270f)
                                            .wrapContentWidth(align = Alignment.CenterHorizontally)
                                            .padding(vertical = 8.dp), // Reduced padding
                                        interactionSource = interactionSource
                                    )

                                    // Reduced spacer
                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Frequency Label
                                    Text(
                                        text = if (bandFrequency >= 1000) "${bandFrequency / 1000} kHz" else "${bandFrequency} Hz",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                        }

                        // 0 dB Reference Line with Gradient
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                        ) {
                            val lineY = size.height / 2f
                            drawLine(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        colorScheme.onSurface.copy(alpha = 0.1f),
                                        colorScheme.onSurface.copy(alpha = 0.5f),
                                        colorScheme.onSurface.copy(alpha = 0.1f)
                                    ),
                                    start = Offset(0f, lineY),
                                    end = Offset(size.width, lineY)
                                ),
                                start = Offset(0f, lineY),
                                end = Offset(size.width, lineY),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )

                            // Draw subtle markers for ±3 dB
                            val markerYPositive = lineY - (size.height / (maxLevel - minLevel).toFloat() * 3)
                            val markerYNegative = lineY + (size.height / (maxLevel - minLevel).toFloat() * 3)
                            listOf(markerYPositive, markerYNegative).forEach { y ->
                                drawLine(
                                    color = colorScheme.onSurface.copy(alpha = 0.2f),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
            }
        }
    } else if (numberOfBands.toInt() == 0) {
        Text(
            "Equalizer bands not available or initializing.",
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    } else {
        Text(
            "Equalizer bands have no adjustable range.",
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}