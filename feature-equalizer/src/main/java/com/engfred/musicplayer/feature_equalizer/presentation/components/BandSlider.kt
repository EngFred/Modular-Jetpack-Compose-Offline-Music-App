package com.engfred.musicplayer.feature_equalizer.presentation.components

import androidx.compose.animation.core.Spring
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.core.domain.repository.BandInfo // Import BandInfo
import kotlin.math.roundToInt

/**
 * A premium equalizer band sliders Composable with enhanced visuals and interactions.
 * It dynamically displays sliders based on the provided list of BandInfo objects.
 *
 * @param bands List of BandInfo containing index, center frequency, and level range for each band.
 * @param bandLevels Map of band index to current gain level (mB).
 * @param globalBandLevelRange Overall min and max gain levels (mB).
 * @param onBandLevelChange Callback when a band's gain changes.
 * @param isCompact Boolean indicating if the layout is compact (e.g., phone portrait).
 */
@Composable
fun EqualizerBandSliders(
    bands: List<BandInfo>, // CHANGED: Now takes List<BandInfo>
    bandLevels: Map<Short, Short>,
    globalBandLevelRange: Pair<Short, Short>, // CHANGED: Renamed parameter
    onBandLevelChange: (bandIndex: Short, gain: Short) -> Unit,
    isCompact: Boolean
) {
    val (minLevel, maxLevel) = globalBandLevelRange // Use globalBandLevelRange
    val zeroLevel = 0.toShort()
    val colorScheme = MaterialTheme.colorScheme
    val hapticFeedback = LocalHapticFeedback.current

    // Adaptive slider height
    val sliderContainerHeight = if (isCompact) 300.dp else 400.dp

    // Only show the card if there are bands and an adjustable range
    if (bands.isNotEmpty() && minLevel != maxLevel) {
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
                            .height(sliderContainerHeight)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxHeight(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Iterate directly over the 'bands' list
                            bands.forEach { bandInfo -> // CHANGED: Looping over BandInfo objects
                                val bandIndex = bandInfo.index
                                val currentLevel = bandLevels[bandIndex] ?: zeroLevel
                                val bandFrequencyMilliHz = bandInfo.centerFrequencyHz // Use actual frequency from BandInfo
                                val interactionSource = remember { MutableInteractionSource() }
                                val isDragging by interactionSource.collectIsDraggedAsState()

                                val animatedLevel by animateFloatAsState(
                                    targetValue = currentLevel.toFloat(),
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "sliderLevel_$bandIndex"
                                )

                                val scale by animateFloatAsState(
                                    targetValue = if (isDragging) 1.05f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
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

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Vertical Slider
                                    Slider(
                                        value = animatedLevel,
                                        onValueChange = { newValue ->
                                            onBandLevelChange(bandIndex, newValue.roundToInt().toShort())
                                        },
                                        // Use the global range, or individual bandInfo.minLevel/maxLevel if they could differ
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
                                                else -> colorScheme.primary
                                            },
                                            inactiveTrackColor = colorScheme.onSurface.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .rotate(270f)
                                            .wrapContentWidth(align = Alignment.CenterHorizontally)
                                            .padding(vertical = 8.dp),
                                        interactionSource = interactionSource
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Frequency Label (convert mHz to Hz/kHz)
                                    Text(
                                        text = if (bandFrequencyMilliHz >= 1_000_000) { // If >= 1000 Hz
                                            "${bandFrequencyMilliHz / 1_000_000} kHz"
                                        } else {
                                            "${bandFrequencyMilliHz / 1_000} Hz" // Convert mHz to Hz
                                        },
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

                            // Draw subtle markers for ±3 dB (approximate, adjust based on your design system)
                            val gainRange = maxLevel - minLevel
                            if (gainRange > 0) { // Avoid division by zero
                                val markerOffsetPerMilliBel = size.height / gainRange.toFloat()
                                val markerYPositive = lineY - (markerOffsetPerMilliBel * 300) // 300 mB = 3 dB
                                val markerYNegative = lineY + (markerOffsetPerMilliBel * 300) // 300 mB = 3 dB

                                listOf(markerYPositive, markerYNegative).forEach { y ->
                                    // Ensure markers are within bounds
                                    if (y in 0f..size.height) {
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
            }
        }
    } else {
        // Fallback text if no bands or no adjustable range
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Equalizer bands not available or initializing.",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            if (minLevel == maxLevel) {
                Text(
                    "No adjustable gain range detected for the equalizer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}