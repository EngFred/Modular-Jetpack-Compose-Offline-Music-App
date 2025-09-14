package com.engfred.musicplayer.feature_settings.presentation.viewmodel

import com.engfred.musicplayer.core.domain.model.AudioPreset
import com.engfred.musicplayer.core.ui.theme.AppThemeType
import com.engfred.musicplayer.core.domain.model.PlayerLayout
import com.engfred.musicplayer.core.domain.model.PlaylistLayoutType
import com.engfred.musicplayer.core.domain.model.FilterOption

/**
 * Data class representing the complete UI state for the Settings Screen.
 */
data class SettingsScreenState(
    val selectedTheme: AppThemeType = AppThemeType.CLASSIC_BLUE,
    val selectedPlayerLayout: PlayerLayout = PlayerLayout.ETHEREAL_FLOW,
    val playlistLayoutType: PlaylistLayoutType = PlaylistLayoutType.LIST,
    val audioPreset: AudioPreset = AudioPreset.NONE,
    val selectedFilterOption: FilterOption = FilterOption.DATE_ADDED_DESC,
    val isLoading: Boolean = false, // To indicate if settings are being loaded/saved
    val error: String? = null // To display any errors
)