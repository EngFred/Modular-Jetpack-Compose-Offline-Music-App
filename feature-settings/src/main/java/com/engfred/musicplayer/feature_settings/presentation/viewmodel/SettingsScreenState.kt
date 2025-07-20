package com.engfred.musicplayer.feature_settings.presentation.viewmodel

import com.engfred.musicplayer.core.ui.theme.AppThemeType // Import AppThemeType

/**
 * Data class representing the complete UI state for the Settings Screen.
 */
data class SettingsScreenState(
    val selectedTheme: AppThemeType = AppThemeType.FROSTBYTE, // The currently selected theme
    val isLoading: Boolean = false, // To indicate if settings are being loaded/saved
    val error: String? = null // To display any errors
)