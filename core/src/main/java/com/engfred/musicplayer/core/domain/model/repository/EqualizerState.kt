//package com.engfred.musicplayer.core.domain.model.repository
//
//data class EqualizerState(
//    val isEnabled: Boolean = false,
//    val numberOfBands: Short = 0,
//    val bands: List<EqBand> = emptyList(),
//    val currentPreset: String = "Normal",
//    val presets: List<String> = emptyList(),
//    val bassBoostStrength: Short = 0,
//    val virtualizerStrength: Short = 0,
//    val bassBoostSupported: Boolean = false,
//    val virtualizerSupported: Boolean = false,
//    val error: String? = null,
//    val isInitializing: Boolean = true,
//    val audioSessionId: Int? = null
//)
//
//data class EqBand(
//    val index: Short,
//    val centerFreqHz: Int,
//    val minLevelMilliBel: Short,
//    val maxLevelMilliBel: Short,
//    val currentLevelMilliBel: Short
//)