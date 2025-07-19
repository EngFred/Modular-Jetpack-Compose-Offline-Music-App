package com.engfred.musicplayer.feature_settings.presentation.viewmodel

import com.engfred.musicplayer.core.ui.theme.AppThemeType

/**
 * Sealed class representing all possible events that can occur on the Settings Screen.
 */
sealed class SettingsEvent {
    data class UpdateTheme(val theme: AppThemeType) : SettingsEvent()
    // Add other settings events here as needed
}