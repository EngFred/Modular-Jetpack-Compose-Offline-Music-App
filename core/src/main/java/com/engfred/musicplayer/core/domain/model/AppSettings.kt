package com.engfred.musicplayer.core.domain.model

import com.engfred.musicplayer.core.ui.theme.AppThemeType

/**
 * Represents the application's user settings.
 * This is a pure domain model.
 */
data class AppSettings(
    val selectedTheme: AppThemeType = AppThemeType.FROSTBYTE,
    val selectedPlayerLayout: PlayerLayout,
    val playlistLayoutType: PlaylistLayoutType
)