package com.engfred.musicplayer.core.domain.repository

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
 * Represents detailed information about an individual equalizer band.
 * @param index The index of the band.
 * @param centerFrequencyHz The center frequency of the band in millihertz (mHz).
 * (e.g., 60 Hz = 60_000 mHz, 1 kHz = 1_000_000 mHz).
 * @param minLevel The minimum gain level for this band in millibels (mB).
 * @param maxLevel The maximum gain level for this band in millibels (mB).
 */
data class BandInfo(
    val index: Short,
    val centerFrequencyHz: Int,
    val minLevel: Short,
    val maxLevel: Short
)

/**
 * Represents the current state of the equalizer.
 */
data class EqualizerState(
    val isEnabled: Boolean = false,
    val numberOfBands: Short = 0,
    val bands: List<BandInfo> = emptyList(), // Detailed information for each band
    val bandLevels: Map<Short, Short> = emptyMap(), // bandIndex to current gain (mB)
    val globalBandLevelRange: Pair<Short, Short> = 0.toShort() to 0.toShort(), // Overall min mB to max mB
    val presets: List<String> = emptyList(),
    val currentPreset: String? = null,
    val error: String? = null,
    val bassBandInfo: BandInfo? = null, // The identified bass band's info
    val trebleBandInfo: BandInfo? = null // The identified treble band's info
)