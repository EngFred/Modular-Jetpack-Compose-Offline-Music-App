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
                    dateAdded = metadata.recordingDay?.toLong() ?: 0L // Use recordingDay or fallback
                )
            }.also {
                Log.d("AudioFileMapper", "Mapped MediaItem ${mediaItem.mediaId} to AudioFile: ${it.title}")
            }
        } catch (e: Exception) {
            Log.e("AudioFileMapper", "Failed to map MediaItem to AudioFile: ${e.message}")
            // FirebaseCrashlytics.getInstance().recordException(e) // Uncomment if using Crashlytics
            null
        }
    }

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
                    Log.d("AudioFileMapper", "Mapped AudioFile ${audioFile.id} to MediaItem: ${it.mediaId}")
                }
        } catch (e: Exception) {
            Log.e("AudioFileMapper", "Failed to map AudioFile to MediaItem: ${e.message}")
            // FirebaseCrashlytics.getInstance().recordException(e)
            MediaItem.EMPTY // Fallback to empty MediaItem
        }
    }
}