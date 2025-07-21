package com.engfred.musicplayer.feature_equalizer.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.engfred.musicplayer.feature_equalizer.presentation.components.BassTrebleControls
import com.engfred.musicplayer.feature_equalizer.presentation.components.EqualizerBandSliders
import com.engfred.musicplayer.feature_equalizer.presentation.components.EqualizerErrorDisplay
import com.engfred.musicplayer.feature_equalizer.presentation.components.EqualizerPresetsDropdown
import com.engfred.musicplayer.feature_equalizer.presentation.components.EqualizerToggle
import com.engfred.musicplayer.feature_equalizer.presentation.viewmodel.EqualizerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    viewModel: EqualizerViewModel = hiltViewModel()
) {
    val equalizerState by viewModel.equalizerState.collectAsState()

    // Wrap the content in a Scaffold
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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