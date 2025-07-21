package com.engfred.musicplayer.feature_equalizer.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    if (minLevel == maxLevel) {
        // If there's no adjustable range, these controls won't be meaningful
        return
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Bass & Treble", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))

            // Bass Control
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Bass", modifier = Modifier.weight(0.3f), style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = bassLevel.toFloat(),
                    onValueChange = { newValue -> onBassChange(newValue.roundToInt().toShort()) },
                    valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                    steps = if (maxLevel > minLevel) (maxLevel - minLevel) - 1 else 0,
                    modifier = Modifier.weight(0.7f)
                )
                Text(
                    text = "${String.format("%.1f", bassLevel / 100f)} dB",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp)) // Spacing between sliders

            // Treble Control
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Treble", modifier = Modifier.weight(0.3f), style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = trebleLevel.toFloat(),
                    onValueChange = { newValue -> onTrebleChange(newValue.roundToInt().toShort()) },
                    valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                    steps = if (maxLevel > minLevel) (maxLevel - minLevel) - 1 else 0,
                    modifier = Modifier.weight(0.7f)
                )
                Text(
                    text = "${String.format("%.1f", trebleLevel / 100f)} dB",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}