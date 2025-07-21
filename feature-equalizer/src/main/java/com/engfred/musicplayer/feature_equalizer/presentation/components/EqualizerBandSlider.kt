package com.engfred.musicplayer.feature_equalizer.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun EqualizerBandSliders(
    numberOfBands: Short,
    bandLevels: Map<Short, Short>,
    bandLevelRange: Pair<Short, Short>,
    getCenterFrequency: (Short) -> Int, // Pass this from ViewModel
    onBandLevelChange: (bandIndex: Short, gain: Short) -> Unit
) {
    val (minLevel, maxLevel) = bandLevelRange
    if (numberOfBands > 0 && minLevel != maxLevel) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Frequency Bands", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))

                // Scrollable row for vertical band sliders
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp) // Fixed height for vertical sliders
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically // Vertically center the rotated sliders
                ) {
                    for (i in 0 until numberOfBands) {
                        val bandIndex = i.toShort()
                        val currentLevel = bandLevels[bandIndex] ?: 0.toShort()
                        val bandFrequency = getCenterFrequency(bandIndex)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .width(60.dp) // Width for each band column
                                .fillMaxHeight()
                                .padding(horizontal = 4.dp)
                        ) {
                            // Top label (dB value)
                            Text(
                                text = "${String.format("%.1f", currentLevel / 100f)} dB",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            // Vertical Slider
                            Slider(
                                value = currentLevel.toFloat(),
                                onValueChange = { newValue ->
                                    onBandLevelChange(bandIndex, newValue.roundToInt().toShort())
                                },
                                valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                                steps = if (maxLevel > minLevel) (maxLevel - minLevel) - 1 else 0,
                                modifier = Modifier
                                    .weight(3f) // Occupy most of the vertical space
                                    .rotate(270f) // Rotate 270 degrees to make it vertical
                                    .wrapContentWidth() // Wrap content to prevent extra width from rotation
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            // Bottom label (Frequency)
                            Text(
                                text = if (bandFrequency >= 1000) "${bandFrequency / 1000} kHz" else "${bandFrequency} Hz",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    } else if (numberOfBands.toInt() == 0) {
        Text("Equalizer bands not available or initializing.")
    } else { // minLevel == maxLevel
        Text("Equalizer bands have no adjustable range.")
    }
}