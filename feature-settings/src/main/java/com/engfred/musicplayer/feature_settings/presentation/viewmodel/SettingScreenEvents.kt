package com.engfred.musicplayer.feature_settings.presentation.viewmodel

import com.engfred.musicplayer.core.ui.theme.AppThemeType
import com.engfred.musicplayer.core.domain.model.PlayerLayout
import com.engfred.musicplayer.core.domain.model.PlaylistLayoutType

/**
 * Sealed class representing user-initiated events on the Settings Screen.
 */
sealed class SettingsEvent {
    data class UpdateTheme(val theme: AppThemeType) : SettingsEvent()
    data class UpdatePlayerLayout(val layout: PlayerLayout) : SettingsEvent()
    data class UpdatePlaylistLayout(val layout: PlaylistLayoutType) : SettingsEvent()
}