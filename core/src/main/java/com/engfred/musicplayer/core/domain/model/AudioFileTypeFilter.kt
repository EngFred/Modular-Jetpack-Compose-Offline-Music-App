package com.engfred.musicplayer.core.domain.model

/**
 * Enum for filtering audio files by type.
 */
enum class AudioFileTypeFilter {
    ALL,  // All supported audio formats
    MP3_ONLY  // Only MP3 files (MIME: audio/mpeg)
}