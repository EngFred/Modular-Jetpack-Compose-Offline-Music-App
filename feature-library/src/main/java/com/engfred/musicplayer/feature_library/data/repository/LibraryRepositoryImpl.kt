package com.engfred.musicplayer.feature_library.data.repository

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.graphics.scale
import com.engfred.musicplayer.core.common.Resource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.repository.LibraryRepository
import com.engfred.musicplayer.feature_library.data.source.local.ContentResolverDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.CannotWriteException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException
import org.jaudiotagger.audio.exceptions.UnableToCreateFileException
import org.jaudiotagger.tag.FieldDataInvalidException
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.TagOptionSingleton
import org.jaudiotagger.tag.images.Artwork
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

class LibraryRepositoryImpl @Inject constructor(
    private val dataSource: ContentResolverDataSource
) : LibraryRepository {

    private val TAG = "LibraryRepositoryImpl"

    override fun getAllAudioFiles() = dataSource.getAllAudioFilesFlow().map { dtoList ->
        dtoList.map { dto ->
            AudioFile(
                id = dto.id,
                title = dto.title ?: "Unknown Title",
                artist = dto.artist ?: "Unknown Artist",
                album = dto.album ?: "Unknown Album",
                duration = dto.duration,
                uri = dto.uri,
                albumArtUri = dto.albumArtUri,
                dateAdded = dto.dateAdded * 1000L,
                artistId = dto.artistId,
                size = dto.size
            )
        }
    }

    override suspend fun getAudioFileByUri(uri: Uri): Resource<AudioFile> {
        return try {
            val dto = dataSource.getAudioFileByUri(uri)
            if (dto != null) {
                val audioFile = AudioFile(
                    id = dto.id,
                    title = dto.title ?: "Unknown Title",
                    artist = dto.artist ?: "Unknown Artist",
                    album = dto.album ?: "Unknown Album",
                    duration = dto.duration,
                    uri = dto.uri,
                    albumArtUri = dto.albumArtUri,
                    dateAdded = dto.dateAdded * 1000L,
                    artistId = dto.artistId,
                    size = dto.size
                )
                Resource.Success(audioFile)
            } else {
                Resource.Error("Audio file not found for uri: $uri")
            }
        } catch (se: RecoverableSecurityException) {
            Log.w(TAG, "RecoverableSecurityException while getting audio file by uri: ${se.message}")
            throw se
        } catch (e: Exception) {
            Log.e(TAG, "Error getting audio file by uri", e)
            Resource.Error(e.message ?: "Unknown error while fetching audio file")
        }
    }

    /**
     * Scoped-storage-safe metadata editor using JAudioTagger. Supports MP3 and M4A.
     * Updates only provided fields; preserves others. Uses app cache for temp mods.
     */
    override suspend fun editAudioMetadata(
        id: Long,
        newTitle: String?,
        newArtist: String?,
        newAlbumArt: ByteArray?,
        context: Context
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

        // Query MIME type for extension mapping
        val projection = arrayOf(MediaStore.Audio.Media.MIME_TYPE)
        val mimeType = context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: return@withContext Resource.Error("Cannot query MIME type")

        val extension = when (mimeType) {
            "audio/mpeg", "audio/mp3" -> "mp3"
            "audio/mp4", "audio/x-m4a" -> "m4a"
            else -> return@withContext Resource.Error("Unsupported MIME type: $mimeType for metadata editing")
        }

        // Set pending for Q+ to allow writes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pendingValues = ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 1) }
            context.contentResolver.update(uri, pendingValues, null, null)
        }

        var tempFile: File? = null
        try {
            // Step 1: Copy file to app cache temp (scoped-safe)
            tempFile = copyToTempFile(context, uri, extension)
            if (tempFile == null) {
                return@withContext Resource.Error("Failed to copy file for editing")
            }

            // Step 2: Modify tags on temp file
            TagOptionSingleton.getInstance().setAndroid(true)
            val jaudiotaggerAudioFile = AudioFileIO.read(tempFile)
            val tag: Tag = jaudiotaggerAudioFile.getTagOrCreateAndSetDefault()

            // Update only provided fields
            newTitle?.let { tag.setField(FieldKey.TITLE, it) }
            newArtist?.let { tag.setField(FieldKey.ARTIST, it) }

            // Handle album art
            newAlbumArt?.let { artBytes ->
                val processedArt = resizeAndCompressImage(artBytes)
                val artwork: Artwork = ArtworkFactory.getNew().apply {
                    setBinaryData(processedArt)
                    setMimeType("image/jpeg")
                    setPictureType(3) // Front cover
                }
                tag.deleteArtworkField()
                tag.setField(artwork)
            }

            AudioFileIO.write(jaudiotaggerAudioFile)

            // Step 3: Stream temp back to MediaStore URI
            if (!streamTempToMediaStore(tempFile, uri, context)) {
                return@withContext Resource.Error("Failed to write updated file")
            }

            // Step 4: Update MediaStore metadata (text fields for sync)
            val updateValues = ContentValues().apply {
                newTitle?.let { put(MediaStore.Audio.Media.TITLE, it) }
                newArtist?.let { put(MediaStore.Audio.Media.ARTIST, it) }
                put(MediaStore.Audio.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000L)
            }
            if (updateValues.size() > 0) {
                context.contentResolver.update(uri, updateValues, null, null)
            }

            scanFile(context, uri)
            Resource.Success(Unit)
        } catch (re: RecoverableSecurityException) {
            Log.w(TAG, "RecoverableSecurityException while editing metadata: ${re.message}")
            throw re
        } catch (e: CannotReadException) {
            Log.e(TAG, "Cannot read audio file", e)
            Resource.Error("Invalid audio file format")
        } catch (e: InvalidAudioFrameException) {
            Log.e(TAG, "Invalid audio frame", e)
            Resource.Error("Corrupted audio file")
        } catch (e: ReadOnlyFileException) {
            Log.e(TAG, "Read-only file", e)
            Resource.Error("File is read-only")
        } catch (e: CannotWriteException) {
            Log.e(TAG, "Cannot write metadata", e)
            Resource.Error("Failed to write metadata")
        } catch (e: UnableToCreateFileException) {
            Log.e(TAG, "Unable to create temp file for metadata", e)
            Resource.Error("Permission denied for file modification")
        } catch (e: FieldDataInvalidException) {
            Log.e(TAG, "Invalid artwork data", e)
            Resource.Error("Failed to process artwork")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to edit audio metadata", e)
            Resource.Error(e.message ?: "Unknown error occurred while editing metadata.")
        } finally {
            // Cleanup temp and finalize pending
            tempFile?.delete()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val finalizeValues = ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }
                context.contentResolver.update(uri, finalizeValues, null, null)
            }
        }
    }

    /**
     * Copies the audio file from URI to a temp file in app's cacheDir.
     */
    private fun copyToTempFile(context: Context, uri: Uri, extension: String): File? {
        val tempDir = context.cacheDir
        val tempFile = File.createTempFile("edit_audio_", ".$extension", tempDir)
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy to temp: ${e.message}")
            tempFile.delete()
            null
        }
    }

    /**
     * Streams the modified temp file back to MediaStore URI.
     */
    private fun streamTempToMediaStore(tempFile: File, uri: Uri, context: Context): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                tempFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            } != null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stream temp to MediaStore: ${e.message}")
            false
        }
    }

    private fun scanFile(context: Context, uri: Uri) {
        val path = getFilePath(context, uri) ?: run {
            Log.w(TAG, "Cannot get file path for scan, skipping")
            return
        }
        val mimeType = if (path.endsWith(".mp3")) "audio/mpeg" else "audio/mp4"
        MediaScannerConnection.scanFile(context, arrayOf(path), arrayOf(mimeType), null)
    }

    private fun getFilePath(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    private fun resizeAndCompressImage(imageBytes: ByteArray): ByteArray {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
        val originalWidth = options.outWidth
        val originalHeight = options.outHeight
        val maxDimension = maxOf(originalWidth, originalHeight)
        val targetSize = 500
        val scaleFactor = if (maxDimension > targetSize) targetSize.toFloat() / maxDimension else 1f

        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val scaledBitmap = bitmap.scale(
            (originalWidth * scaleFactor).toInt(),
            (originalHeight * scaleFactor).toInt()
        )
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)

        bitmap.recycle()
        scaledBitmap.recycle()

        return outputStream.toByteArray()
    }
}