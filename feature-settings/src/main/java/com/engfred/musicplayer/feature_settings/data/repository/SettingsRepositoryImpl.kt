package com.engfred.musicplayer.feature_settings.data.repository

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
import com.engfred.musicplayer.core.domain.model.WidgetBackgroundMode
import com.engfred.musicplayer.core.domain.model.WidgetDisplayInfo
import com.engfred.musicplayer.core.domain.repository.RepeatMode
import com.engfred.musicplayer.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    companion object {
        private val SELECTED_THEME = stringPreferencesKey("selected_theme")
        private val SELECTED_PLAYER_LAYOUT = stringPreferencesKey("selected_player_layout")
        private val PLAYLIST_LAYOUT_TYPE = stringPreferencesKey("playlist_layout_type")
        private val SELECTED_FILTER_OPTION = stringPreferencesKey("selected_filter_option")
        private val REPEAT_MODE = stringPreferencesKey("repeat_mode")
        private val SELECTED_AUDIO_PRESET = stringPreferencesKey("selected_audio_preset")
        private val SELECT_WIDGET_BACKGROUND_MODE = stringPreferencesKey("widget_background_mode")

        private val LAST_PLAYED_AUDIO_ID = longPreferencesKey("last_played_audio_id")
        private val LAST_POSITION_MS = longPreferencesKey("last_position_ms")
        private val LAST_QUEUE_IDS = stringPreferencesKey("last_queue_ids")

//        // New for full widget info cache (for fast load)
//        private val LAST_TITLE = stringPreferencesKey("last_title")
//        private val LAST_ARTIST = stringPreferencesKey("last_artist")
//        private val LAST_DURATION_MS = longPreferencesKey("last_duration_ms")
//        private val LAST_ARTWORK_URI = stringPreferencesKey("last_artwork_uri")
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
                    preferences[SELECTED_THEME] ?: AppThemeType.CLASSIC_BLUE.name
                )
                val selectedPlayerLayout = PlayerLayout.valueOf(
                    preferences[SELECTED_PLAYER_LAYOUT] ?: PlayerLayout.ETHEREAL_FLOW.name
                )
                val playlistLayoutType = PlaylistLayoutType.valueOf(
                    preferences[PLAYLIST_LAYOUT_TYPE] ?: PlaylistLayoutType.LIST.name
                )
                val repeatMode = RepeatMode.valueOf(
                    preferences[REPEAT_MODE] ?: RepeatMode.OFF.name
                )
                val selectedAudioPreset = AudioPreset.valueOf(
                    preferences[SELECTED_AUDIO_PRESET] ?: AudioPreset.NONE.name
                )

                val widgetMode = preferences[SELECT_WIDGET_BACKGROUND_MODE]?.let {
                    try {
                        WidgetBackgroundMode.valueOf(it)
                    } catch (_: Exception) { WidgetBackgroundMode.STATIC }
                } ?: WidgetBackgroundMode.STATIC

                AppSettings(
                    selectedTheme = selectedTheme,
                    selectedPlayerLayout = selectedPlayerLayout,
                    playlistLayoutType = playlistLayoutType,
                    repeatMode = repeatMode,
                    audioPreset = selectedAudioPreset,
                    widgetBackgroundMode = widgetMode
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
                val queueIds = preferences[LAST_QUEUE_IDS]?.takeIf { it.isNotBlank() }?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: null
                LastPlaybackState(
                    audioId = preferences[LAST_PLAYED_AUDIO_ID],
                    positionMs = preferences[LAST_POSITION_MS] ?: 0L,
                    queueIds = queueIds
                )
            }
    }

    override suspend fun saveLastPlaybackState(state: LastPlaybackState) {
        dataStore.edit { preferences ->
            if (state.audioId != null) {
                preferences[LAST_PLAYED_AUDIO_ID] = state.audioId!!
                preferences[LAST_POSITION_MS] = state.positionMs
            } else {
                preferences.remove(LAST_PLAYED_AUDIO_ID)
                preferences.remove(LAST_POSITION_MS)
            }
            val queueStr = state.queueIds?.joinToString(",")
            if (queueStr != null && queueStr.isNotEmpty()) {
                preferences[LAST_QUEUE_IDS] = queueStr
            } else {
                preferences.remove(LAST_QUEUE_IDS)
            }
        }
    }

//    // New: Save full widget info
//    override suspend fun saveLastWidgetInfo(info: WidgetDisplayInfo?) {
//        dataStore.edit { preferences ->
//            if (info != null) {
//                preferences[LAST_TITLE] = info.title
//                preferences[LAST_ARTIST] = info.artist
//                preferences[LAST_DURATION_MS] = info.durationMs
//                preferences[LAST_POSITION_MS] = info.positionMs  // Reuse key, but okay as it's same
//                if (info.artworkUri != null) preferences[LAST_ARTWORK_URI] = info.artworkUri.toString()
//            } else {
//                preferences.remove(LAST_TITLE)
//                preferences.remove(LAST_ARTIST)
//                preferences.remove(LAST_DURATION_MS)
//                preferences.remove(LAST_ARTWORK_URI)
//            }
//        }
//    }
//
//    // New: Get full widget info from prefs (fast)
//    override suspend fun getLastWidgetInfo(): WidgetDisplayInfo? {
//        val preferences = dataStore.data.first()
//        val title = preferences[LAST_TITLE] ?: return null
//        val artist = preferences[LAST_ARTIST] ?: return null
//        val durationMs = preferences[LAST_DURATION_MS] ?: return null
//        val positionMs = preferences[LAST_POSITION_MS] ?: 0L
//        val artworkStr = preferences[LAST_ARTWORK_URI]
//        val artworkUri = artworkStr?.toUri()
//
//        return WidgetDisplayInfo(
//            title = title,
//            artist = artist,
//            durationMs = durationMs,
//            positionMs = positionMs,
//            artworkUri = artworkUri
//        )
//    }

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

    override suspend fun updateAudioPreset(preset: AudioPreset) {
        dataStore.edit { preferences ->
            preferences[SELECTED_AUDIO_PRESET] = preset.name
        }
    }

    override suspend fun updateWidgetBackgroundMode(mode: WidgetBackgroundMode) {
        dataStore.edit { preferences ->
            preferences[SELECT_WIDGET_BACKGROUND_MODE] = mode.name
        }
    }
}