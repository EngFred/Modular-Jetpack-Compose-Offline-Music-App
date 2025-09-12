package com.engfred.musicplayer.core.domain.repository

import com.engfred.musicplayer.core.domain.model.AppSettings
import com.engfred.musicplayer.core.domain.model.AudioPreset
import com.engfred.musicplayer.core.domain.model.FilterOption
import com.engfred.musicplayer.core.domain.model.PlayerLayout
import com.engfred.musicplayer.core.domain.model.PlaylistLayoutType
import com.engfred.musicplayer.core.ui.theme.AppThemeType
import kotlinx.coroutines.flow.Flow

/**
 * Defines the contract for data operations related to application settings.
 */
interface SettingsRepository {

    fun getAppSettings(): Flow<AppSettings>

    suspend fun updateTheme(theme: AppThemeType)

    suspend fun updatePlayerLayout(layout: PlayerLayout)

    suspend fun updatePlaylistLayout(layout: PlaylistLayoutType)

    suspend fun updateFilterOption(filterOption: FilterOption)
    fun getFilterOption(): Flow<FilterOption>

    suspend fun updateRepeatMode(repeatMode: RepeatMode)

    suspend fun updateShuffleMode(shuffleMode: ShuffleMode)

    suspend fun updateCrossfadeEnabled(enabled: Boolean)

    suspend fun updateAudioPreset(preset: AudioPreset)
}