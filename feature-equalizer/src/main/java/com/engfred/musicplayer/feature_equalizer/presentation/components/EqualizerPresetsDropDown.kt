package com.engfred.musicplayer.feature_equalizer.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerPresetsDropdown(
    presets: List<String>,
    currentPreset: String?,
    onPresetSelected: (String) -> Unit
) {
    if (presets.isNotEmpty()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Presets", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentPreset ?: "Custom",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Preset") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        presets.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset) },
                                onClick = {
                                    onPresetSelected(preset)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Only show this message if there are bands but no presets available
        // This check would ideally be handled in the parent composable or by a richer EqualizerState
        // for now, it's just a simple text
        // Text("No presets available on this device.")
    }
}