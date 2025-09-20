package com.engfred.musicplayer.core.util

import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.model.FilterOption

fun sortAudioFiles(audioFiles: List<AudioFile>, filter: FilterOption): List<AudioFile> {
    return when (filter) {
        FilterOption.DATE_ADDED_ASC -> audioFiles.sortedBy { it.dateAdded }
        FilterOption.DATE_ADDED_DESC -> audioFiles.sortedByDescending { it.dateAdded }
        FilterOption.LENGTH_ASC -> audioFiles.sortedBy { it.duration }
        FilterOption.LENGTH_DESC -> audioFiles.sortedByDescending { it.duration }
        FilterOption.ALPHABETICAL_ASC -> audioFiles.sortedBy { it.title.lowercase() }
        FilterOption.ALPHABETICAL_DESC -> audioFiles.sortedByDescending { it.title.lowercase() }
    }
}