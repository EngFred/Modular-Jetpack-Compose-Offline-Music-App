package com.engfred.musicplayer.feature_settings.domain.model

import com.engfred.musicplayer.core.ui.theme.AppThemeType

/**
 * Represents the application's user settings.
 * This is a pure domain model.
 */
data class AppSettings(
    val selectedTheme: AppThemeType = AppThemeType.FROSTBYTE
)