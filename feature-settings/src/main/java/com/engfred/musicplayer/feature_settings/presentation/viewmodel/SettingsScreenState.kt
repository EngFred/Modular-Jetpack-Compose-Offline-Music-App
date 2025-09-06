package com.engfred.musicplayer.feature_settings.presentation.viewmodel

import com.engfred.musicplayer.core.ui.theme.AppThemeType
import com.engfred.musicplayer.core.domain.model.PlayerLayout
import com.engfred.musicplayer.core.domain.model.PlaylistLayoutType
import com.engfred.musicplayer.core.domain.model.FilterOption

/**
 * Data class representing the complete UI state for the Settings Screen.
 */
data class SettingsScreenState(
    val selectedTheme: AppThemeType = AppThemeType.FROSTBYTE, // The currently selected theme
    // ---------------------------------------------
    // NEW: Add fields for additional settings, excluding repeatMode and shuffleMode
    // ---------------------------------------------
    val selectedPlayerLayout: PlayerLayout = PlayerLayout.ETHEREAL_FLOW,
    val playlistLayoutType: PlaylistLayoutType = PlaylistLayoutType.LIST,
    val selectedFilterOption: FilterOption = FilterOption.DATE_ADDED_DESC,
    val isLoading: Boolean = false, // To indicate if settings are being loaded/saved
    val error: String? = null // To display any errors
)