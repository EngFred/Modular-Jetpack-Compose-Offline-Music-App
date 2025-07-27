package com.engfred.musicplayer.feature_settings.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.engfred.musicplayer.core.ui.theme.AppThemeType
import com.engfred.musicplayer.core.domain.model.AppSettings
import com.engfred.musicplayer.core.domain.model.FilterOption
import com.engfred.musicplayer.core.domain.model.PlayerLayout
import com.engfred.musicplayer.core.domain.model.PlaylistLayoutType
import com.engfred.musicplayer.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of SettingsRepository using DataStore Preferences.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    // Define preference keys
    private object PreferencesKeys {
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
        val SELECTED_PLAYER_LAYOUT = stringPreferencesKey("selected_player_layout")
        val PLAYLIST_LAYOUT_TYPE = stringPreferencesKey("playlist_layout_type")
        val SELECTED_FILTER_OPTION = stringPreferencesKey("selected_filter_option")
    }

    override fun getAppSettings(): Flow<AppSettings> {
        return dataStore.data.map { preferences ->
            val selectedThemeString = preferences[PreferencesKeys.SELECTED_THEME] ?: AppThemeType.DEEP_BLUE.name
            val selectedPlayerLayoutString = preferences[PreferencesKeys.SELECTED_PLAYER_LAYOUT] ?: PlayerLayout.ETHEREAL_FLOW.name
            val playlistLayoutTypeString = preferences[PreferencesKeys.PLAYLIST_LAYOUT_TYPE] ?: PlaylistLayoutType.LIST.name
            AppSettings(
                selectedTheme = AppThemeType.valueOf(selectedThemeString),
                selectedPlayerLayout = PlayerLayout.valueOf(selectedPlayerLayoutString),
                playlistLayoutType = PlaylistLayoutType.valueOf(playlistLayoutTypeString)
            )
        }
    }

    /**
     * Retrieves the saved filter option from DataStore.
     */
    override fun getFilterOption(): Flow<FilterOption> {
        return dataStore.data.map { preferences ->
            val filterOptionString = preferences[PreferencesKeys.SELECTED_FILTER_OPTION] ?: FilterOption.DATE_ADDED_DESC.name
            FilterOption.valueOf(filterOptionString)
        }
    }

    override suspend fun updateTheme(theme: AppThemeType) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_THEME] = theme.name
        }
    }

    override suspend fun updatePlayerLayout(layout: PlayerLayout) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_PLAYER_LAYOUT] = layout.name
        }
    }

    override suspend fun updatePlaylistLayout(layout: PlaylistLayoutType) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PLAYLIST_LAYOUT_TYPE] = layout.name
        }
    }

    /**
     * Saves the selected filter option to DataStore.
     */
    override suspend fun updateFilterOption(filterOption: FilterOption) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_FILTER_OPTION] = filterOption.name
        }
    }
}