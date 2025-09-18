package com.engfred.helpers

import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.model.FilterOption
import com.engfred.musicplayer.core.domain.repository.PlaybackController
import com.engfred.musicplayer.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

suspend fun preparePlayback(
    audioFiles: List<AudioFile>,
    settingsRepository: SettingsRepository,
    playbackController: PlaybackController
): List<AudioFile> {
    // Consolidate settings fetch to avoid repeated retrievals
    val appSettings = settingsRepository.getAppSettings().first()
    val filter = settingsRepository.getFilterOption().first()

    val sortedAudios = when (filter) {
        FilterOption.DATE_ADDED_ASC -> audioFiles.sortedBy { it.dateAdded }
        FilterOption.DATE_ADDED_DESC -> audioFiles.sortedByDescending { it.dateAdded }
        FilterOption.LENGTH_ASC -> audioFiles.sortedBy { it.duration }
        FilterOption.LENGTH_DESC -> audioFiles.sortedByDescending { it.duration }
        FilterOption.ALPHABETICAL_ASC -> audioFiles.sortedBy { it.title.lowercase() }
        FilterOption.ALPHABETICAL_DESC -> audioFiles.sortedByDescending { it.title.lowercase() }
    }

    // Ensure controller reflects settings
    playbackController.setRepeatMode(appSettings.repeatMode)
    playbackController.setShuffleMode(appSettings.shuffleMode)
    return sortedAudios
}