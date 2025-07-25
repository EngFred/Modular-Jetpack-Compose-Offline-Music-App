package com.engfred.musicplayer.feature_equalizer.data

import android.media.audiofx.Equalizer
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import com.engfred.musicplayer.core.data.session.AudioSessionIdPublisher
import com.engfred.musicplayer.core.domain.repository.BandInfo
import com.engfred.musicplayer.core.domain.repository.EqualizerController
import com.engfred.musicplayer.core.domain.repository.EqualizerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "EqualizerControllerImpl"

@Singleton
@OptIn(UnstableApi::class)
class EqualizerControllerImpl @Inject constructor(
    private val audioSessionIdPublisher: AudioSessionIdPublisher
) : EqualizerController {

    private var equalizer: Equalizer? = null
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val equalizerMutex = Mutex()

    private val _equalizerState = MutableStateFlow(EqualizerState())
    override fun getEqualizerState(): StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    init {
        Log.d(TAG, "Initializing EqualizerControllerImpl")
        controllerScope.launch {
            audioSessionIdPublisher.currentAudioSessionId.collectLatest { sessionId ->
                Log.d(TAG, "Received audio session ID: $sessionId")
                equalizerMutex.withLock {
                    if (sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId != 0) {
                        try {
                            equalizer?.release()
                            equalizer = null

                            equalizer = Equalizer(0, sessionId).apply {
                                enabled = true
                            }
                            Log.d(TAG, "Equalizer created with session ID: $sessionId, enabled: ${equalizer?.enabled}")
                            updateEqualizerStateInternal()
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to create Equalizer with session ID $sessionId: ${e.message}", e)
                            equalizer?.release()
                            equalizer = null
                            _equalizerState.value = EqualizerState(error = "Failed to initialize Equalizer: ${e.message}")
                        }
                    } else {
                        Log.d(TAG, "Audio Session ID is unset (0). Releasing equalizer.")
                        equalizer?.release()
                        equalizer = null
                        _equalizerState.value = EqualizerState()
                    }
                }
            }
        }
    }

    /**
     * Internal helper to read the current state from the Equalizer and publish it.
     * This method assumes it's called from within a `equalizerMutex.withLock` block.
     */
    private fun updateEqualizerStateInternal() {
        equalizer?.let { eq ->
            val numberOfBands = eq.numberOfBands
            val (minLevel, maxLevel) = eq.bandLevelRange
            val globalBandLevelRange = minLevel to maxLevel

            val bandLevels = mutableMapOf<Short, Short>()
            val bandsInfo = mutableListOf<BandInfo>()

            for (i in 0 until numberOfBands) {
                val bandIndex = i.toShort()
                val centerFreq = eq.getCenterFreq(bandIndex)
                val currentGain = eq.getBandLevel(bandIndex)
                bandLevels[bandIndex] = currentGain
                bandsInfo.add(BandInfo(bandIndex, centerFreq, minLevel, maxLevel)) // Using global min/max for bandInfo too
            }

            val presets = (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }
            val currentPresetIndex = eq.currentPreset

            val currentPresetName = if (currentPresetIndex >= 0 && currentPresetIndex < eq.numberOfPresets) {
                try {
                    eq.getPresetName(currentPresetIndex)
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "Error getting preset name for index $currentPresetIndex: ${e.message}")
                    null
                }
            } else {
                null
            }

            // Identify Bass and Treble bands based on frequency
            val bassFrequencyUpperLimitMilliHz = 250_000 // 250 Hz in mHz
            val trebleFrequencyLowerLimitMilliHz = 4_000_000 // 4 kHz in mHz

            val identifiedBassBand = bandsInfo
                .filter { it.centerFrequencyHz <= bassFrequencyUpperLimitMilliHz }
                .maxByOrNull { it.centerFrequencyHz } // Get the highest frequency band within the bass range

            val identifiedTrebleBand = bandsInfo
                .filter { it.centerFrequencyHz >= trebleFrequencyLowerLimitMilliHz }
                .minByOrNull { it.centerFrequencyHz } // Get the lowest frequency band within the treble range

            _equalizerState.value = EqualizerState(
                isEnabled = eq.enabled,
                numberOfBands = numberOfBands,
                bands = bandsInfo,
                bandLevels = bandLevels,
                globalBandLevelRange = globalBandLevelRange,
                presets = presets,
                currentPreset = currentPresetName,
                error = null,
                bassBandInfo = identifiedBassBand, // Set the identified bass band info
                trebleBandInfo = identifiedTrebleBand // Set the identified treble band info
            )
            Log.d(TAG, "Equalizer state updated: ${_equalizerState.value}")
        } ?: run {
            _equalizerState.value = EqualizerState()
            Log.d(TAG, "Equalizer is null, state reset to default.")
        }
    }

    override suspend fun setBandLevel(bandIndex: Short, gain: Short) {
        equalizerMutex.withLock {
            equalizer?.let { eq ->
                try {
                    eq.setBandLevel(bandIndex, gain)
                    Log.d(TAG, "Set band $bandIndex to $gain mB")
                    val currentBandLevels = _equalizerState.value.bandLevels.toMutableMap()
                    currentBandLevels[bandIndex] = gain
                    _equalizerState.value = _equalizerState.value.copy(
                        bandLevels = currentBandLevels,
                        currentPreset = null // Manual adjustment means no preset is active
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to set band level for band $bandIndex: ${e.message}", e)
                    _equalizerState.value = _equalizerState.value.copy(error = "Failed to set band level: ${e.message}")
                }
            } ?: Log.w(TAG, "Equalizer not initialized, cannot set band level.")
        }
    }

    override suspend fun setPreset(presetName: String) {
        equalizerMutex.withLock {
            equalizer?.let { eq ->
                try {
                    val presetIndex = (0 until eq.numberOfPresets).firstOrNull { eq.getPresetName(it.toShort()) == presetName }?.toShort()
                    if (presetIndex != null) {
                        eq.usePreset(presetIndex)
                        Log.d(TAG, "Set preset to: $presetName")
                        // After applying a preset, re-read all band levels
                        val newBandLevels = mutableMapOf<Short, Short>()
                        for (i in 0 until eq.numberOfBands) {
                            newBandLevels[i.toShort()] = eq.getBandLevel(i.toShort())
                        }
                        _equalizerState.value = _equalizerState.value.copy(
                            bandLevels = newBandLevels,
                            currentPreset = presetName
                        )
                    } else {
                        Log.w(TAG, "Preset '$presetName' not found.")
                        _equalizerState.value = _equalizerState.value.copy(error = "Preset '$presetName' not found.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to set preset '$presetName': ${e.message}", e)
                    _equalizerState.value = _equalizerState.value.copy(error = "Failed to set preset: ${e.message}")
                }
            } ?: Log.w(TAG, "Equalizer not initialized, cannot set preset.")
        }
    }

    override suspend fun setEnabled(enabled: Boolean) {
        equalizerMutex.withLock {
            equalizer?.let { eq ->
                try {
                    if (eq.enabled != enabled) {
                        eq.enabled = enabled
                        Log.d(TAG, "Equalizer enabled set to $enabled")
                        // If enabling, immediately update the state to reflect current band levels
                        // If disabling, the levels remain but isEnabled changes
                        _equalizerState.value = _equalizerState.value.copy(isEnabled = enabled)
                        // If enabling, we should ideally re-read the full state to ensure UI is accurate
                        if (enabled) {
                            updateEqualizerStateInternal()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to set equalizer enabled state to $enabled: ${e.message}", e)
                    _equalizerState.value = _equalizerState.value.copy(error = "Failed to set enabled state: ${e.message}")
                }
            } ?: Log.w(TAG, "Equalizer not initialized, cannot set enabled state.")
        }
    }

    override fun release() {
        Log.d(TAG, "Releasing EqualizerControllerImpl")
        controllerScope.cancel()
        controllerScope.launch {
            equalizerMutex.withLock {
                equalizer?.release()
                equalizer = null
                _equalizerState.value = EqualizerState() // Reset state on release
                Log.d(TAG, "Equalizer released and state reset.")
            }
        }
    }
}