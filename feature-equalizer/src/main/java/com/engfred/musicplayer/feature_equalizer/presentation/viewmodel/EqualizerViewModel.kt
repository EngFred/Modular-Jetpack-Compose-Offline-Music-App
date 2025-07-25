package com.engfred.musicplayer.feature_equalizer.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.core.domain.repository.EqualizerController
import com.engfred.musicplayer.core.domain.repository.EqualizerState
import com.engfred.musicplayer.core.domain.repository.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val equalizerController: EqualizerController,
    playerController: PlayerController
) : ViewModel() {

    val equalizerState: StateFlow<EqualizerState> = equalizerController.getEqualizerState()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            initialValue = EqualizerState()
        )

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()


    init {
        //to apply some bottom padding when displaying a mini player
        playerController.getPlaybackState().onEach { state ->
            if (state.currentAudioFile != null) {
                Log.d("PlaylistViewModel", "Is playing...")
                _isPlaying.update { true }
            } else {
                Log.d("PlaylistViewModel", "Is not playing!!")
                _isPlaying.update { false }
            }
        }.launchIn(viewModelScope)

        // Disable equalizer at start to prevent unexpected audio changes
        // Users can enable it explicitly from the UI if desired.
        setEnabled(false)
    }

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
     * It dynamically identifies the bass band based on its center frequency.
     * @param gain The gain in millibels (mB).
     */
    fun setBassGain(gain: Short) {
        viewModelScope.launch {
            // Define a typical upper limit for bass frequencies (e.g., 250 Hz)
            val bassFrequencyUpperLimitMilliHz = 250_000 // 250 Hz in mHz

            // Find the band that is most likely the "bass" band.
            // This prioritizes bands with center frequencies within the typical bass range,
            // favoring higher frequencies within that range to potentially affect more of the
            // perceived "punch" or "warmth" in the bass.
            val bassBand = equalizerState.value.bands
                .filter { it.centerFrequencyHz <= bassFrequencyUpperLimitMilliHz }
                .maxByOrNull { it.centerFrequencyHz } // Get the highest frequency band within the bass range

            bassBand?.let {
                equalizerController.setBandLevel(it.index, gain)
            } ?: Log.w("EqualizerViewModel", "No suitable bass band found.")
        }
    }

    /**
     * Sets the gain for the "treble" frequency range.
     * It dynamically identifies the treble band based on its center frequency.
     * @param gain The gain in millibels (mB).
     */
    fun setTrebleGain(gain: Short) {
        viewModelScope.launch {
            // Define a typical lower limit for treble frequencies (e.g., 4 kHz)
            val trebleFrequencyLowerLimitMilliHz = 4_000_000 // 4 kHz in mHz

            // Find the band that is most likely the "treble" band.
            // This prioritizes bands with center frequencies within the typical treble range,
            // favoring lower frequencies within that range to affect the start of the treble.
            val trebleBand = equalizerState.value.bands
                .filter { it.centerFrequencyHz >= trebleFrequencyLowerLimitMilliHz }
                .minByOrNull { it.centerFrequencyHz } // Get the lowest frequency band within the treble range

            trebleBand?.let {
                equalizerController.setBandLevel(it.index, gain)
            } ?: Log.w("EqualizerViewModel", "No suitable treble band found.")
        }
    }
}