package com.engfred.musicplayer.feature_library.data.source.local

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import com.engfred.musicplayer.feature_library.data.model.AudioFileDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Data source for accessing device audio files using ContentResolver and MediaStore.
 * Provides a reactive flow that emits a new list of audio files whenever changes are detected
 * in the MediaStore.
 */
class ContentResolverDataSource @Inject constructor(
    private val context: Context
) {

    private val AUDIO_PROJECTION = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DATE_ADDED
    )

    private val AUDIO_SELECTION = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

    private val ALBUM_ART_BASE_URI = "content://media/external/audio/albumart".toUri()

    /**
     * Emits a list of audio files stored on the device using MediaStore.
     * This flow reacts to changes in the MediaStore by re-querying and emitting updated lists.
     */
    fun getAllAudioFilesFlow(): Flow<List<AudioFileDto>> = callbackFlow {
        val fetchAndSendAudioFiles = {
            val audioFiles = mutableListOf<AudioFileDto>()
            var cursor: Cursor? = null
            try {
                cursor = context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    AUDIO_PROJECTION,
                    AUDIO_SELECTION,
                    null,
                    null
                )

                cursor?.use {
                    val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                    while (it.moveToNext()) {
                        val id = it.getLong(idColumn)
                        val title = it.getString(titleColumn)
                        val artist = it.getString(artistColumn)
                        val album = it.getString(albumColumn)
                        val duration = it.getLong(durationColumn)
                        val albumId = it.getLong(albumIdColumn)
                        val dateAdded = it.getLong(dateAddedColumn)

                        val contentUri: Uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        )

                        val albumArtUri: Uri? = if (albumId != 0L) {
                            ContentUris.withAppendedId(
                                ALBUM_ART_BASE_URI,
                                albumId
                            )
                        } else null

                        audioFiles.add(
                            AudioFileDto(
                                id = id,
                                title = title,
                                artist = artist,
                                album = album,
                                duration = duration,
                                // The 'data' field (file path) is removed as Uri is the modern way to access media.
                                // If you absolutely need the file path for specific legacy operations,
                                // consider alternatives or specific permissions for Android 10+.
                                data = contentUri.toString(), // Using URI string as a placeholder for 'data'
                                uri = contentUri,
                                albumId = albumId,
                                albumArtUri = albumArtUri,
                                dateAdded = dateAdded
                            )
                        )
                    }
                }
                trySend(audioFiles)
            } catch (e: Exception) {
                Log.e("TAG", "Error fetching audio files", e)
                trySend(emptyList())
            } finally {
                cursor?.close()
            }
        }

        val audioObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                fetchAndSendAudioFiles()
            }
        }

        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            audioObserver
        )

        fetchAndSendAudioFiles()

        awaitClose {
            context.contentResolver.unregisterContentObserver(audioObserver)
        }
    }.flowOn(Dispatchers.IO)
}