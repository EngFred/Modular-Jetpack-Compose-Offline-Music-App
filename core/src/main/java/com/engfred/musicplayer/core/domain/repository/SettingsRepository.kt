package com.engfred.musicplayer.core.domain.repository

import com.engfred.musicplayer.core.domain.model.AppSettings
import com.engfred.musicplayer.core.domain.model.FilterOption
import com.engfred.musicplayer.core.domain.model.PlayerLayout
import com.engfred.musicplayer.core.domain.model.PlaylistLayoutType
import com.engfred.musicplayer.core.ui.theme.AppThemeType
import kotlinx.coroutines.flow.Flow

/**
 * Defines the contract for data operations related to application settings.
 */
interface SettingsRepository {

    /**
     * Observes the current application settings.
     * @return A Flow emitting AppSettings updates.
     */
    fun getAppSettings(): Flow<AppSettings>

    /**
     * Updates the selected theme in the application settings.
     * @param theme The new AppThemeType to set.
     */
    suspend fun updateTheme(theme: AppThemeType)

    /**
     * Updates the selected player layout in the application settings.
     * @param layout The new PlayerLayout to set.
     */
    suspend fun updatePlayerLayout(layout: PlayerLayout)

    /**
     * Updates the playlist layout type in the application settings.
     * @param layout The new PlaylistLayoutType to set.
     */
    suspend fun updatePlaylistLayout(layout: PlaylistLayoutType)

    suspend fun updateFilterOption(filterOption: FilterOption)
    fun getFilterOption(): Flow<FilterOption>
}