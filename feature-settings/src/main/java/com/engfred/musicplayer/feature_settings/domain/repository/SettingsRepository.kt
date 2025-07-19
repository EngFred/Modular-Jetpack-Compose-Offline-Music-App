package com.engfred.musicplayer.feature_settings.domain.repository

import com.engfred.musicplayer.feature_settings.domain.model.AppSettings
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

    // Add other settings update methods here as needed
}