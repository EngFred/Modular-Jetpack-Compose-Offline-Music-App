package com.engfred.musicplayer.feature_settings.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.engfred.musicplayer.core.ui.theme.AppThemeType
import com.engfred.musicplayer.feature_settings.domain.model.AppSettings
import com.engfred.musicplayer.feature_settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of SettingsRepository using DataStore Preferences.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences> // Inject DataStore directly
) : SettingsRepository {

    // Define preference keys
    private object PreferencesKeys {
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
        // Add other preference keys here
    }

    override fun getAppSettings(): Flow<AppSettings> {
        return dataStore.data.map { preferences -> // Use injected dataStore
            val selectedThemeString = preferences[PreferencesKeys.SELECTED_THEME] ?: AppThemeType.FROSTBYTE.name
            AppSettings(
                selectedTheme = AppThemeType.valueOf(selectedThemeString)
            )
        }
    }

    override suspend fun updateTheme(theme: AppThemeType) {
        dataStore.edit { preferences -> // Use injected dataStore
            preferences[PreferencesKeys.SELECTED_THEME] = theme.name
        }
    }
}