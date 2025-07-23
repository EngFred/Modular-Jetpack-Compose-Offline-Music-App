// File: com/engfred/musicplayer/feature_equalizer/presentation/components/BassTrebleControls.kt

package com.engfred.musicplayer.feature_equalizer.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun BassTrebleControls(
    bassLevel: Short,
    trebleLevel: Short,
    bandLevelRange: Pair<Short, Short>,
    onBassChange: (Short) -> Unit,
    onTrebleChange: (Short) -> Unit
) {
    val (minLevel, maxLevel) = bandLevelRange
    val zeroLevel = 0.toShort()

    if (minLevel == maxLevel) {
        return // No adjustable range
    }

    // Helper function to convert actual level (Short) to normalized 0f-1f for knob
    fun levelToNormalized(level: Short): Float {
        return (level - minLevel).toFloat() / (maxLevel - minLevel)
    }

    // Helper function to convert normalized 0f-1f from knob to actual level (Short)
    fun normalizedToLevel(normalizedValue: Float): Short {
        return (normalizedValue * (maxLevel - minLevel) + minLevel).roundToInt().toShort()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Bass & Treble",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bass Control with Rotary Knob
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Bass",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    RotaryKnob(
                        value = levelToNormalized(bassLevel),
                        onValueChange = { normalizedValue ->
                            onBassChange(normalizedToLevel(normalizedValue))
                        },
                        knobSize = 100.dp, // Adjust size as needed
                        indicatorColor = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "${String.format("%.1f", bassLevel / 100f)} dB",
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            bassLevel > zeroLevel -> Color.Green.copy(alpha = 0.8f)
                            bassLevel < zeroLevel -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp)) // Spacing between knobs

                // Treble Control with Rotary Knob
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Treble",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    RotaryKnob(
                        value = levelToNormalized(trebleLevel),
                        onValueChange = { normalizedValue ->
                            onTrebleChange(normalizedToLevel(normalizedValue))
                        },
                        knobSize = 100.dp,
                        indicatorColor = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "${String.format("%.1f", trebleLevel / 100f)} dB",
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            trebleLevel > zeroLevel -> Color.Green.copy(alpha = 0.8f)
                            trebleLevel < zeroLevel -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}