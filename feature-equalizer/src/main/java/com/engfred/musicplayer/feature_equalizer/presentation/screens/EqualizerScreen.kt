package com.engfred.musicplayer.feature_equalizer.presentation.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.engfred.musicplayer.feature_equalizer.presentation.components.BassTrebleControls
import com.engfred.musicplayer.feature_equalizer.presentation.components.EqualizerBandSliders
import com.engfred.musicplayer.feature_equalizer.presentation.components.EqualizerErrorDisplay
import com.engfred.musicplayer.feature_equalizer.presentation.components.EqualizerPresetsDropdown
import com.engfred.musicplayer.feature_equalizer.presentation.components.EqualizerToggle
import com.engfred.musicplayer.feature_equalizer.presentation.viewmodel.EqualizerViewModel
import android.util.Log // Import Log for warnings

@Composable
fun EqualizerScreen(
    viewModel: EqualizerViewModel = hiltViewModel(),
    windowWidthSizeClass: WindowWidthSizeClass
) {
    val equalizerState by viewModel.equalizerState.collectAsState()
    val view = LocalView.current
    val isPlaying by viewModel.isPlaying.collectAsState() // Still used if MiniPlayer's height depends on this

    val isCompactWidth = windowWidthSizeClass == WindowWidthSizeClass.Compact

    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        val baseModifier = Modifier
            .fillMaxSize()
            .padding(paddingValues) // Use Scaffold's calculated padding
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            )

        // Error Display: Always show at the top if there's an error.
        Column(modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(horizontal = 16.dp)) { // Moved outside the main content column to always be on top
            AnimatedVisibility(
                visible = equalizerState.error != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                equalizerState.error?.let { EqualizerErrorDisplay(errorMessage = it) }
            }
        }


        if (isCompactWidth) {
            // --- Compact Layout (Phones - Portrait) ---
            Column(
                modifier = baseModifier
                    .verticalScroll(rememberScrollState()) // Allow overall screen scrolling
                    .padding(horizontal = 16.dp, vertical = 8.dp), // Overall screen padding
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp)) // Initial spacer

                // 1. Equalizer Enabled/Disabled Toggle
                EqualizerToggle(
                    isEnabled = equalizerState.isEnabled,
                    onEnabledChange = {
                        viewModel.setEnabled(it)
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                )

                // 2. Presets Dropdown
                EqualizerPresetsDropdown(
                    presets = equalizerState.presets,
                    currentPreset = equalizerState.currentPreset,
                    onPresetSelected = {
                        viewModel.setPreset(it)
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                )

                // 3. Bass & Treble Controls (Conditional based on identified bands)
                // IMPORTANT: This now relies on `bassBandInfo` and `trebleBandInfo` from EqualizerState
                if (equalizerState.bassBandInfo != null && equalizerState.trebleBandInfo != null && equalizerState.globalBandLevelRange.first != equalizerState.globalBandLevelRange.second) {
                    val bassLevel = equalizerState.bandLevels[equalizerState.bassBandInfo!!.index] ?: 0.toShort()
                    val trebleLevel = equalizerState.bandLevels[equalizerState.trebleBandInfo!!.index] ?: 0.toShort()

                    BassTrebleControls(
                        bassLevel = bassLevel,
                        trebleLevel = trebleLevel,
                        globalBandLevelRange = equalizerState.globalBandLevelRange, // Renamed
                        onBassChange = {
                            viewModel.setBassGain(it)
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        },
                        onTrebleChange = {
                            viewModel.setTrebleGain(it)
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        },
                        isCompact = true
                    )
                } else {
                    Log.w("EqualizerScreen", "Bass or Treble band not identified, BassTrebleControls hidden.")
                    // Optionally show a placeholder or message if bass/treble bands are not available
                }

                // 4. Frequency Band Sliders (Now uses equalizerState.bands for information)
                // IMPORTANT: Pass equalizerState.bands and remove getCenterFrequency parameter
                if (equalizerState.bands.isNotEmpty() && equalizerState.globalBandLevelRange.first != equalizerState.globalBandLevelRange.second) {
                    EqualizerBandSliders(
                        bands = equalizerState.bands, // NEW: Pass the list of BandInfo
                        bandLevels = equalizerState.bandLevels,
                        globalBandLevelRange = equalizerState.globalBandLevelRange, // Renamed
                        onBandLevelChange = { bandIndex, gain ->
                            viewModel.setBandLevel(bandIndex, gain)
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        },
                        isCompact = true
                    )
                } else {
                    Log.w("EqualizerScreen", "No equalizer bands available or range is zero, EqualizerBandSliders hidden.")
                    // Optionally show a placeholder or message
                }
            }
        } else {
            // --- Expanded Layout (Phones - Landscape, Tablets, Desktops) ---
            Row(
                modifier = baseModifier
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Controls (EqualizerToggle, Presets, Bass/Treble)
                Column(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    EqualizerToggle(
                        isEnabled = equalizerState.isEnabled,
                        onEnabledChange = {
                            viewModel.setEnabled(it)
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                    )
                    EqualizerPresetsDropdown(
                        presets = equalizerState.presets,
                        currentPreset = equalizerState.currentPreset,
                        onPresetSelected = {
                            viewModel.setPreset(it)
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                    )
                    // IMPORTANT: This now relies on `bassBandInfo` and `trebleBandInfo`
                    if (equalizerState.bassBandInfo != null && equalizerState.trebleBandInfo != null && equalizerState.globalBandLevelRange.first != equalizerState.globalBandLevelRange.second) {
                        val bassLevel = equalizerState.bandLevels[equalizerState.bassBandInfo!!.index] ?: 0.toShort()
                        val trebleLevel = equalizerState.bandLevels[equalizerState.trebleBandInfo!!.index] ?: 0.toShort()
                        BassTrebleControls(
                            bassLevel = bassLevel,
                            trebleLevel = trebleLevel,
                            globalBandLevelRange = equalizerState.globalBandLevelRange, // Renamed
                            onBassChange = {
                                viewModel.setBassGain(it)
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            },
                            onTrebleChange = {
                                viewModel.setTrebleGain(it)
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            },
                            isCompact = false
                        )
                    } else {
                        Log.w("EqualizerScreen", "Bass or Treble band not identified in expanded layout, BassTrebleControls hidden.")
                    }
                }

                // Right Column: Band Sliders (main visual element)
                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // IMPORTANT: Pass equalizerState.bands and remove getCenterFrequency parameter
                    if (equalizerState.bands.isNotEmpty() && equalizerState.globalBandLevelRange.first != equalizerState.globalBandLevelRange.second) {
                        EqualizerBandSliders(
                            bands = equalizerState.bands, // NEW: Pass the list of BandInfo
                            bandLevels = equalizerState.bandLevels,
                            globalBandLevelRange = equalizerState.globalBandLevelRange, // Renamed
                            onBandLevelChange = { bandIndex, gain ->
                                viewModel.setBandLevel(bandIndex, gain)
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            },
                            isCompact = false
                        )
                    } else {
                        Log.w("EqualizerScreen", "No equalizer bands available or range is zero in expanded layout, EqualizerBandSliders hidden.")
                    }
                }
            }
        }
    }
}