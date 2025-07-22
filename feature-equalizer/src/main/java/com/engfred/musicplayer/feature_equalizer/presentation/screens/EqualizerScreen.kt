package com.engfred.musicplayer.feature_equalizer.presentation.screens

import androidx.compose.foundation.background // Import for background gradient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api // Keep if still using ExperimentalMaterial3Api for something else
import androidx.compose.material3.MaterialTheme // Import for colors
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush // Import for background gradient
import androidx.compose.ui.graphics.Color // Import for Color.Transparent
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.engfred.musicplayer.core.ui.CustomTopBar
import com.engfred.musicplayer.feature_equalizer.presentation.components.BassTrebleControls
import com.engfred.musicplayer.feature_equalizer.presentation.components.EqualizerBandSliders
import com.engfred.musicplayer.feature_equalizer.presentation.components.EqualizerErrorDisplay
import com.engfred.musicplayer.feature_equalizer.presentation.components.EqualizerPresetsDropdown
import com.engfred.musicplayer.feature_equalizer.presentation.components.EqualizerToggle
import com.engfred.musicplayer.feature_equalizer.presentation.viewmodel.EqualizerViewModel

@Composable
fun EqualizerScreen(
    viewModel: EqualizerViewModel = hiltViewModel()
) {
    val equalizerState by viewModel.equalizerState.collectAsState()

    // Wrap the content in a Scaffold
    Scaffold(
        // Remove topBar parameter from Scaffold as it's now managed by the parent
        containerColor = Color.Transparent // Ensures parent's background gradient is visible
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from the Scaffold
                .background( // Add background gradient to the column
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EqualizerErrorDisplay(errorMessage = equalizerState.error)

            // 1. Equalizer Enabled/Disabled Toggle
            EqualizerToggle(
                isEnabled = equalizerState.isEnabled,
                onEnabledChange = viewModel::setEnabled
            )

            // 2. Presets Dropdown
            EqualizerPresetsDropdown(
                presets = equalizerState.presets,
                currentPreset = equalizerState.currentPreset,
                onPresetSelected = viewModel::setPreset
            )

            // 3. Bass & Treble Controls
            if (equalizerState.numberOfBands > 0 && equalizerState.bandLevelRange.first != equalizerState.bandLevelRange.second) {
                val bassLevel = equalizerState.bandLevels[0.toShort()] ?: 0.toShort()
                val trebleLevel = equalizerState.bandLevels[(equalizerState.numberOfBands - 1).toShort()] ?: 0.toShort()

                BassTrebleControls(
                    bassLevel = bassLevel,
                    trebleLevel = trebleLevel,
                    bandLevelRange = equalizerState.bandLevelRange,
                    onBassChange = viewModel::setBassGain,
                    onTrebleChange = viewModel::setTrebleGain
                )
            }

            // 4. Frequency Band Sliders
            EqualizerBandSliders(
                numberOfBands = equalizerState.numberOfBands,
                bandLevels = equalizerState.bandLevels,
                bandLevelRange = equalizerState.bandLevelRange,
                getCenterFrequency = viewModel::getCenterFrequency,
                onBandLevelChange = viewModel::setBandLevel
            )
        }
    }
}