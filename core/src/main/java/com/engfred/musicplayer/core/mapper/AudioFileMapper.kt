package com.engfred.musicplayer.core.mapper

import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.engfred.musicplayer.core.domain.model.AudioFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioFileMapper @Inject constructor() {
    private val TAG = "AudioFileMapper"

    /**
     * Maps a MediaItem from Media3 player to an AudioFile domain model.
     * Returns null if mapping fails.
     */
    fun mapMediaItemToAudioFile(mediaItem: MediaItem): AudioFile? {
        return try {
            mediaItem.mediaMetadata.let { metadata ->
                AudioFile(
                    id = mediaItem.mediaId.toLongOrNull() ?: 0L,
                    title = metadata.title?.toString() ?: "Unknown Title",
                    artist = metadata.artist?.toString() ?: "Unknown Artist",
                    album = metadata.albumTitle?.toString() ?: "Unknown Album",
                    duration = metadata.durationMs ?: 0L,
                    uri = mediaItem.localConfiguration?.uri ?: Uri.EMPTY,
                    albumArtUri = metadata.artworkUri,
                    dateAdded = metadata.recordingDay?.toLong() ?: 0L
                )
            }.also {
                Log.d(TAG, "Mapped MediaItem ${mediaItem.mediaId} to AudioFile: ${it.title}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to map MediaItem to AudioFile", e)
            null
        }
    }

    /**
     * Maps an AudioFile domain model to a MediaItem for Media3 player.
     * Returns MediaItem.EMPTY if mapping fails.
     */
    fun mapAudioFileToMediaItem(audioFile: AudioFile): MediaItem {
        return try {
            MediaItem.Builder()
                .setMediaId(audioFile.id.toString())
                .setUri(audioFile.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(audioFile.title)
                        .setArtist(audioFile.artist)
                        .setAlbumTitle(audioFile.album)
                        .setArtworkUri(audioFile.albumArtUri)
                        .setDurationMs(audioFile.duration)
                        .build()
                )
                .build()
                .also {
                    Log.d(TAG, "Mapped AudioFile ${audioFile.id} to MediaItem: ${it.mediaId}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to map AudioFile to MediaItem", e)
            MediaItem.EMPTY
        }
    }
}
