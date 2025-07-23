package com.engfred.musicplayer.feature_equalizer.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView // Import for haptics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    val view = LocalView.current // For haptic feedback

    val isPlaying by viewModel.isPlaying.collectAsState()

    Scaffold(
        // topBar = { /* Removed as per your request */ },
        containerColor = Color.Transparent
    ) { paddingValues ->
        val bottomPadding = if(isPlaying) 142.dp else 75.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error Display (now with AnimatedVisibility for smoother appearance)
            AnimatedVisibility(
                visible = equalizerState.error != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                equalizerState.error?.let { EqualizerErrorDisplay(errorMessage = it) }
            }

            Spacer(modifier = Modifier.height(8.dp)) // Added some space below top bar/error

            // 1. Equalizer Enabled/Disabled Toggle
            EqualizerToggle(
                isEnabled = equalizerState.isEnabled,
                onEnabledChange = {
                    viewModel.setEnabled(it)
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                }
            )

            // 2. Presets Dropdown
            EqualizerPresetsDropdown(
                presets = equalizerState.presets,
                currentPreset = equalizerState.currentPreset,
                onPresetSelected = {
                    viewModel.setPreset(it)
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                }
            )

            // Dynamic visibility for Bass & Treble (only if adjustable bands exist)
            if (equalizerState.numberOfBands > 0 && equalizerState.bandLevelRange.first != equalizerState.bandLevelRange.second) {
                val bassLevel = equalizerState.bandLevels[0.toShort()] ?: 0.toShort()
                val trebleLevel = equalizerState.bandLevels[(equalizerState.numberOfBands - 1).toShort()] ?: 0.toShort()

                BassTrebleControls(
                    bassLevel = bassLevel,
                    trebleLevel = trebleLevel,
                    bandLevelRange = equalizerState.bandLevelRange,
                    onBassChange = {
                        viewModel.setBassGain(it)
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    },
                    onTrebleChange = {
                        viewModel.setTrebleGain(it)
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                )
            }

            // Frequency Band Sliders
            EqualizerBandSliders(
                numberOfBands = equalizerState.numberOfBands,
                bandLevels = equalizerState.bandLevels,
                bandLevelRange = equalizerState.bandLevelRange,
                getCenterFrequency = viewModel::getCenterFrequency,
                onBandLevelChange = { bandIndex, gain ->
                    viewModel.setBandLevel(bandIndex, gain)
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                }
            )
        }
    }
}