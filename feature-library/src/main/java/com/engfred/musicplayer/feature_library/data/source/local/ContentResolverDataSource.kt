package com.engfred.musicplayer.feature_library.data.source.local

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.engfred.musicplayer.feature_library.data.model.AudioFileDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import androidx.core.net.toUri

/**
 * Data source for fetching audio files using Android's ContentResolver and MediaStore.
 * This class is responsible for interacting directly with the Android framework.
 */
class ContentResolverDataSource @Inject constructor(
    private val context: Context
) {

    /**
     * Fetches all audio files from the device's MediaStore.
     * @return A list of AudioFileDto objects.
     */
    suspend fun getAllAudioFiles(): List<AudioFileDto> = withContext(Dispatchers.IO) {
        val audioFiles = mutableListOf<AudioFileDto>()

        // Define the columns we want to retrieve
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA, // Path to the file (often deprecated for Uri)
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED
        )

        // Define the selection criteria (only music files)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        // Query the MediaStore
        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null // No specific sort order for now, can be added later
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn)
                val artist = it.getString(artistColumn)
                val album = it.getString(albumColumn)
                val duration = it.getLong(durationColumn)
                val data = it.getString(dataColumn)
                val albumId = it.getLong(albumIdColumn)
                val dateAdded = it.getLong(dateAddedColumn)

                // Construct the content URI for the audio file
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                // Construct the album art URI
                val albumArtUri: Uri? = if (albumId != 0L) { // Check for non-zero albumId
                    ContentUris.withAppendedId(
                        "content://media/external/audio/albumart".toUri(),
                        albumId
                    )
                } else {
                    null
                }

                audioFiles.add(
                    AudioFileDto(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        data = data,
                        uri = contentUri,
                        albumId = albumId,
                        albumArtUri = albumArtUri,
                        dateAdded = dateAdded
                    )
                )
            }
        }
        audioFiles
    }

    /**
     * Searches for audio files based on a query string.
     * This method performs a basic search on title, artist, and album.
     * @param query The search query.
     * @return A list of matching AudioFileDto objects.
     */
    suspend fun searchAudioFiles(query: String): List<AudioFileDto> = withContext(Dispatchers.IO) {
        val audioFiles = mutableListOf<AudioFileDto>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND (" +
                "${MediaStore.Audio.Media.TITLE} LIKE ? OR " +
                "${MediaStore.Audio.Media.ARTIST} LIKE ? OR " +
                "${MediaStore.Audio.Media.ALBUM} LIKE ?)"

        val searchPattern = "%$query%"
        val selectionArgs = arrayOf(searchPattern, searchPattern, searchPattern)

        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn)
                val artist = it.getString(artistColumn)
                val album = it.getString(albumColumn)
                val duration = it.getLong(durationColumn)
                val data = it.getString(dataColumn)
                val albumId = it.getLong(albumIdColumn)
                val dateAdded = it.getLong(dateAddedColumn)

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val albumArtUri: Uri? = if (albumId != 0L) { // Check for non-zero albumId
                    ContentUris.withAppendedId(
                        "content://media/external/audio/albumart".toUri(),
                        albumId
                    )
                } else {
                    null
                }

                audioFiles.add(
                    AudioFileDto(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        data = data,
                        uri = contentUri,
                        albumId = albumId,
                        albumArtUri = albumArtUri, // ADDED: Pass albumArtUri to DTO
                        dateAdded = dateAdded
                    )
                )
            }
        }
        audioFiles
    }
}