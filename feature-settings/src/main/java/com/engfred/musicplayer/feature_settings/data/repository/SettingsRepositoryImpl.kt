package com.engfred.musicplayer.feature_settings.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.engfred.musicplayer.core.ui.theme.AppThemeType
import com.engfred.musicplayer.core.domain.model.AppSettings
import com.engfred.musicplayer.core.domain.model.AudioPreset
import com.engfred.musicplayer.core.domain.model.FilterOption
import com.engfred.musicplayer.core.domain.model.LastPlaybackState
import com.engfred.musicplayer.core.domain.model.PlayerLayout
import com.engfred.musicplayer.core.domain.model.PlaylistLayoutType
import com.engfred.musicplayer.core.domain.repository.RepeatMode
import com.engfred.musicplayer.core.domain.repository.SettingsRepository
import com.engfred.musicplayer.core.domain.repository.ShuffleMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SettingsRepository using DataStore.
 * Handles defaults and error recovery for production robustness.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    companion object {
        private val SELECTED_THEME = stringPreferencesKey("selected_theme")
        private val SELECTED_PLAYER_LAYOUT = stringPreferencesKey("selected_player_layout")
        private val PLAYLIST_LAYOUT_TYPE = stringPreferencesKey("playlist_layout_type")
        private val CROSSFADE_ENABLED = booleanPreferencesKey("crossfade_enabled")
        private val SELECTED_FILTER_OPTION = stringPreferencesKey("selected_filter_option")
        private val REPEAT_MODE = stringPreferencesKey("repeat_mode")
        private val SHUFFLE_MODE = stringPreferencesKey("shuffle_mode")
        private val SELECTED_AUDIO_PRESET = stringPreferencesKey("selected_audio_preset")

        /**
         * NEW: Keys for transient last playback state (audio ID and position).
         * Stored as Long for efficiency; null ID clears the state.
         */
        private val LAST_PLAYED_AUDIO_ID = longPreferencesKey("last_played_audio_id")
        private val LAST_POSITION_MS = longPreferencesKey("last_position_ms")
    }

    override fun getAppSettings(): Flow<AppSettings> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val selectedTheme = AppThemeType.valueOf(
                    preferences[SELECTED_THEME] ?: AppThemeType.BLUE.name
                )
                val selectedPlayerLayout = PlayerLayout.valueOf(
                    preferences[SELECTED_PLAYER_LAYOUT] ?: PlayerLayout.ETHEREAL_FLOW.name
                )
                val playlistLayoutType = PlaylistLayoutType.valueOf(
                    preferences[PLAYLIST_LAYOUT_TYPE] ?: PlaylistLayoutType.LIST.name
                )
                val crossfadeEnabled = preferences[CROSSFADE_ENABLED] ?: false
                val repeatMode = RepeatMode.valueOf(
                    preferences[REPEAT_MODE] ?: RepeatMode.OFF.name
                )
                val shuffleMode = ShuffleMode.valueOf(
                    preferences[SHUFFLE_MODE] ?: ShuffleMode.OFF.name
                )
                val selectedAudioPreset = AudioPreset.valueOf(
                    preferences[SELECTED_AUDIO_PRESET] ?: AudioPreset.NONE.name
                )
                AppSettings(
                    selectedTheme = selectedTheme,
                    selectedPlayerLayout = selectedPlayerLayout,
                    playlistLayoutType = playlistLayoutType,
                    crossfadeEnabled = crossfadeEnabled,
                    repeatMode = repeatMode,
                    shuffleMode = shuffleMode,
                    audioPreset = selectedAudioPreset
                )
            }
    }

    override fun getFilterOption(): Flow<FilterOption> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                FilterOption.valueOf(
                    preferences[SELECTED_FILTER_OPTION] ?: FilterOption.DATE_ADDED_DESC.name
                )
            }
    }

    /**
     * NEW: Flow for last playback state, with error handling (emits default null state on IO error).
     * This enables resumption: if audioId is non-null, rebuild queue and seek to positionMs.
     */
    override fun getLastPlaybackState(): Flow<LastPlaybackState> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                LastPlaybackState(
                    audioId = preferences[LAST_PLAYED_AUDIO_ID],
                    positionMs = preferences[LAST_POSITION_MS] ?: 0L
                )
            }
    }

    /**
     * NEW: Suspend func to save last state (or clear if audioId null).
     * Async IO via DataStore; called from service onDestroy for best-effort persistence.
     */
    override suspend fun saveLastPlaybackState(state: LastPlaybackState) {
        dataStore.edit { preferences ->
            if (state.audioId != null) {
                preferences[LAST_PLAYED_AUDIO_ID] = state.audioId!!
                preferences[LAST_POSITION_MS] = state.positionMs
            } else {
                // Clear stale state (e.g., file deleted)
                preferences.remove(LAST_PLAYED_AUDIO_ID)
                preferences.remove(LAST_POSITION_MS)
            }
        }
    }

    override suspend fun updateTheme(theme: AppThemeType) {
        dataStore.edit { preferences ->
            preferences[SELECTED_THEME] = theme.name
        }
    }

    override suspend fun updatePlayerLayout(layout: PlayerLayout) {
        dataStore.edit { preferences ->
            preferences[SELECTED_PLAYER_LAYOUT] = layout.name
        }
    }

    override suspend fun updatePlaylistLayout(layout: PlaylistLayoutType) {
        dataStore.edit { preferences ->
            preferences[PLAYLIST_LAYOUT_TYPE] = layout.name
        }
    }

    override suspend fun updateCrossfadeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CROSSFADE_ENABLED] = enabled
        }
    }

    override suspend fun updateFilterOption(filterOption: FilterOption) {
        dataStore.edit { preferences ->
            preferences[SELECTED_FILTER_OPTION] = filterOption.name
        }
    }

    override suspend fun updateRepeatMode(repeatMode: RepeatMode) {
        dataStore.edit { preferences ->
            preferences[REPEAT_MODE] = repeatMode.name
        }
    }

    override suspend fun updateShuffleMode(shuffleMode: ShuffleMode) {
        dataStore.edit { preferences ->
            preferences[SHUFFLE_MODE] = shuffleMode.name
        }
    }

    override suspend fun updateAudioPreset(preset: AudioPreset) {
        dataStore.edit { preferences ->
            preferences[SELECTED_AUDIO_PRESET] = preset.name
        }
    }
}