package com.engfred.musicplayer.feature_equalizer.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Composable for dedicated Bass and Treble rotary knob controls.
 *
 * @param bassLevel The current gain level for the identified bass band (mB).
 * @param trebleLevel The current gain level for the identified treble band (mB).
 * @param globalBandLevelRange The overall min and max gain levels (mB) for the equalizer.
 * @param onBassChange Callback when the bass gain changes.
 * @param onTrebleChange Callback when the treble gain changes.
 * @param isCompact Boolean indicating if the layout is compact.
 */
@Composable
fun BassTrebleControls(
    bassLevel: Short,
    trebleLevel: Short,
    globalBandLevelRange: Pair<Short, Short>, // CHANGED: Renamed parameter
    onBassChange: (Short) -> Unit,
    onTrebleChange: (Short) -> Unit,
    isCompact: Boolean
) {
    val (minLevel, maxLevel) = globalBandLevelRange // Use globalBandLevelRange
    val zeroLevel = 0.toShort()
    val colorScheme = MaterialTheme.colorScheme

    if (minLevel == maxLevel) {
        // If there's no adjustable range, don't show the controls.
        // EqualizerScreen already has a check for this, but this adds robustness.
        return
    }

    // Helper function to convert actual level (Short) to normalized 0f-1f for knob
    fun levelToNormalized(level: Short): Float {
        // Ensure that division by zero doesn't occur if range is somehow 0
        if ((maxLevel - minLevel).toShort() == 0.toShort()) return 0f
        return (level - minLevel).toFloat() / (maxLevel - minLevel)
    }

    // Helper function to convert normalized 0f-1f from knob to actual level (Short)
    fun normalizedToLevel(normalizedValue: Float): Short {
        return (normalizedValue * (maxLevel - minLevel) + minLevel).roundToInt().toShort()
    }

    // Adaptive knob size
    val knobSize = if (isCompact) 100.dp else 120.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface.copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Bass & Treble",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.onSurface,
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
                        color = colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    RotaryKnob(
                        value = levelToNormalized(bassLevel),
                        onValueChange = { normalizedValue ->
                            onBassChange(normalizedToLevel(normalizedValue))
                        },
                        knobSize = knobSize,
                        indicatorColor = colorScheme.primary
                    )
                    Text(
                        text = "${String.format("%.1f", bassLevel / 100f)} dB",
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            bassLevel > zeroLevel -> Color.Green.copy(alpha = 0.8f)
                            bassLevel < zeroLevel -> colorScheme.error.copy(alpha = 0.8f)
                            else -> colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(if (isCompact) 16.dp else 24.dp))

                // Treble Control with Rotary Knob
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Treble",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    RotaryKnob(
                        value = levelToNormalized(trebleLevel),
                        onValueChange = { normalizedValue ->
                            onTrebleChange(normalizedToLevel(normalizedValue))
                        },
                        knobSize = knobSize,
                        indicatorColor = colorScheme.primary
                    )
                    Text(
                        text = "${String.format("%.1f", trebleLevel / 100f)} dB",
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            trebleLevel > zeroLevel -> Color.Green.copy(alpha = 0.8f)
                            trebleLevel < zeroLevel -> colorScheme.error.copy(alpha = 0.8f)
                            else -> colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}