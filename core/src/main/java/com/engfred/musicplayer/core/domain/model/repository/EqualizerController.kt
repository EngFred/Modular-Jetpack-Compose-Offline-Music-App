package com.engfred.musicplayer.core.domain.model.repository

import kotlinx.coroutines.flow.Flow

interface EqualizerController {
    /**
     * Provides the current state of the equalizer.
     */
    fun getEqualizerState(): Flow<EqualizerState>

    /**
     * Sets the gain for a specific equalizer band.
     * @param bandIndex The index of the band to adjust.
     * @param gain The gain in millibels (mB).
     */
    suspend fun setBandLevel(bandIndex: Short, gain: Short)

    /**
     * Sets the equalizer preset.
     * @param presetName The name of the preset to apply.
     */
    suspend fun setPreset(presetName: String)

    /**
     * Enables or disables the equalizer.
     * @param enabled True to enable, false to disable.
     */
    suspend fun setEnabled(enabled: Boolean)

    /**
     * Releases any resources held by the equalizer.
     */
    fun release()
}

/**
 * Represents the current state of the equalizer.
 */
data class EqualizerState(
    val isEnabled: Boolean = false,
    val numberOfBands: Short = 0,
    val bandLevels: Map<Short, Short> = emptyMap(), // bandIndex to gain (mB)
    val bandLevelRange: Pair<Short, Short> = 0.toShort() to 0.toShort(), // min mB to max mB
    val presets: List<String> = emptyList(),
    val currentPreset: String? = null,
    val error: String? = null
)