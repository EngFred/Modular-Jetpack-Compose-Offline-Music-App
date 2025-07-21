package com.engfred.musicplayer.feature_equalizer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.core.domain.model.repository.EqualizerController
import com.engfred.musicplayer.core.domain.model.repository.EqualizerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val equalizerController: EqualizerController
) : ViewModel() {

    val equalizerState: StateFlow<EqualizerState> = equalizerController.getEqualizerState()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            initialValue = EqualizerState()
        )

    fun setBandLevel(bandIndex: Short, gain: Short) {
        viewModelScope.launch {
            equalizerController.setBandLevel(bandIndex, gain)
        }
    }

    fun setPreset(presetName: String) {
        viewModelScope.launch {
            equalizerController.setPreset(presetName)
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            equalizerController.setEnabled(enabled)
        }
    }

    /**
     * Sets the gain for the "bass" frequency range.
     * Assumes band 0 is the primary bass band.
     * @param gain The gain in millibels (mB).
     */
    fun setBassGain(gain: Short) {
        viewModelScope.launch {
            // Adjust this logic if you want to control multiple low bands for bass
            val bassBandIndex = 0.toShort()
            if (bassBandIndex < equalizerState.value.numberOfBands) {
                equalizerController.setBandLevel(bassBandIndex, gain)
            }
        }
    }

    /**
     * Sets the gain for the "treble" frequency range.
     * Assumes the highest band is the primary treble band.
     * @param gain The gain in millibels (mB).
     */
    fun setTrebleGain(gain: Short) {
        viewModelScope.launch {
            // Adjust this logic if you want to control multiple high bands for treble
            val trebleBandIndex = (equalizerState.value.numberOfBands - 1).toShort()
            if (trebleBandIndex >= 0 && trebleBandIndex < equalizerState.value.numberOfBands) {
                equalizerController.setBandLevel(trebleBandIndex, gain)
            }
        }
    }

    // IMPORTANT NOTE: getCenterFrequency() is still a placeholder.
    // For a truly professional app, EqualizerState should provide BandInfo objects
    // with actual center frequencies from the native Equalizer instance.
    fun getCenterFrequency(bandIndex: Short): Int {
        return when (bandIndex.toInt()) {
            0 -> 60_000 // 60 Hz
            1 -> 230_000 // 230 Hz
            2 -> 910_000 // 910 Hz (approx 1 kHz)
            3 -> 3_600_000 // 3.6 kHz
            4 -> 14_000_000 // 14 kHz
            else -> 0 // Or handle more bands if your equalizer supports them
        }
    }
}