package com.engfred.musicplayer.feature_library.data.repository

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.engfred.musicplayer.core.common.Resource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.repository.LibraryRepository
import com.engfred.musicplayer.core.mapper.AudioFileMapper
import com.engfred.musicplayer.feature_library.data.source.local.ContentResolverDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import androidx.core.graphics.scale

class LibraryRepositoryImpl @Inject constructor(
    private val dataSource: ContentResolverDataSource
) : LibraryRepository {

    private val TAG = "LibraryRepositoryImpl"

    override fun getAllAudioFiles(): Flow<List<AudioFile>> {
        return dataSource.getAllAudioFilesFlow().map { dtoList ->
            dtoList.map { dto ->
                AudioFile(
                    id = dto.id,
                    title = dto.title ?: "Unknown Title",
                    artist = dto.artist ?: "Unknown Artist",
                    album = dto.album ?: "Unknown Album",
                    duration = dto.duration,
                    uri = dto.uri,
                    albumArtUri = dto.albumArtUri,
                    dateAdded = dto.dateAdded * 1000L
                )
            }
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
                    dateAdded = dto.dateAdded * 1000L
                )
                Resource.Success(audioFile)
            } else {
                Resource.Error("Audio file not found for uri: $uri")
            }
        } catch (se: RecoverableSecurityException) {
            Log.w(TAG, "RecoverableSecurityException while getting audio file by uri: ${se.message}")
            // Propagate or wrap as error depending on your app flow. Keeping consistent with editAudioMetadata which throws.
            throw se
        } catch (e: Exception) {
            Log.e(TAG, "Error getting audio file by uri", e)
            Resource.Error(e.message ?: "Unknown error while fetching audio file")
        }

    }

    override suspend fun editAudioMetadata(
        id: Long,
        newTitle: String?,
        newArtist: String?,
        newAlbumArt: ByteArray?,
        context: Context
    ): Resource<Unit> {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

        try {
            // Check MIME type (quick guard)
            val projection = arrayOf(MediaStore.Audio.Media.MIME_TYPE)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            val mimeType = cursor?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
            if (mimeType != null && mimeType !in listOf("audio/mpeg", "audio/mp3")) {
                return Resource.Error("Only MP3 files are supported for metadata editing.")
            }

            // Read original bytes
            val originalBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return Resource.Error("Unable to read audio file.")

            // Prepare album art bytes (resize/compress) if provided
            val processedAlbumArt = newAlbumArt?.let { resizeAndCompressImage(it) }

            // We'll detect if a tag exists; if yes, use its version (3 or 4). If no tag, default to v2.3.
            var tagVersion = 3 // default to ID3v2.3 when creating
            var audioStart = 0
            val existingFrames = mutableListOf<ByteArray>()

            if (originalBytes.size >= 10 &&
                originalBytes[0] == 'I'.toByte() &&
                originalBytes[1] == 'D'.toByte() &&
                originalBytes[2] == '3'.toByte()
            ) {
                // header exists
                val versionByte = originalBytes[3].toInt() and 0xFF
                tagVersion = if (versionByte == 3 || versionByte == 4) versionByte else 3
                val tagSize = calculateSyncSafeInt(originalBytes.copyOfRange(6, 10))
                audioStart = 10 + tagSize

                // Parse frames inside the tag correctly depending on version
                var pos = 10
                while (pos + 10 <= audioStart) {
                    val idBytes = originalBytes.copyOfRange(pos, pos + 4)
                    // stop on padding (four zero bytes)
                    if (idBytes.all { it == 0.toByte() }) break
                    val frameId = String(idBytes)
                    val frameSizeBytes = originalBytes.copyOfRange(pos + 4, pos + 8)
                    val frameSize = if (tagVersion == 4) calculateSyncSafeInt(frameSizeBytes) else bigEndianToInt(frameSizeBytes)
                    val flags = originalBytes.copyOfRange(pos + 8, pos + 10)
                    if (frameSize <= 0 || pos + 10 + frameSize > audioStart) break

                    val frameData = originalBytes.copyOfRange(pos + 10, pos + 10 + frameSize)

                    // Recreate full raw frame in the same header format (so we can preserve it easily)
                    val sizeBytesForHeader = if (tagVersion == 4) encodeSyncSafe(frameSize) else intToBigEndianBytes(frameSize)
                    val header = idBytes + sizeBytesForHeader + flags
                    val fullFrame = header + frameData

                    // keep only frames that are not being edited (we add edited ones later)
                    when (frameId) {
                        "TIT2" -> if (newTitle == null) existingFrames.add(fullFrame)
                        "TPE1" -> if (newArtist == null) existingFrames.add(fullFrame)
                        "APIC" -> if (newAlbumArt == null) existingFrames.add(fullFrame)
                        else -> existingFrames.add(fullFrame)
                    }
                    pos += 10 + frameSize
                }
            } else {
                // No ID3 tag found — we'll create one (default v2.3)
                tagVersion = 3
                audioStart = 0
            }

            // Add/replace edited frames (matching the detected tag version)
            newTitle?.let { existingFrames.add(createTextFrame("TIT2", it, tagVersion)) }
            newArtist?.let { existingFrames.add(createTextFrame("TPE1", it, tagVersion)) }
            processedAlbumArt?.let { existingFrames.add(createApicFrame(it, tagVersion)) }

            // Build new tag bytes (frames concatenated)
            val allFramesData = existingFrames.fold(ByteArray(0)) { acc, frame -> acc + frame }
            val tagFramesSize = allFramesData.size

            if (tagFramesSize > 0) {
                val header = byteArrayOf(
                    'I'.toByte(), 'D'.toByte(), '3'.toByte(),
                    tagVersion.toByte(), 0.toByte(), // version byte and revision=0
                    0.toByte() // flags (no extended flags used here)
                ) + encodeSyncSafe(tagFramesSize) // header length must be syncsafe

                // Audio data is everything after the existing tag header (or whole file if no tag)
                val audioData = originalBytes.copyOfRange(audioStart, originalBytes.size)
                val newFileBytes = header + allFramesData + audioData

                // Write back. This may throw RecoverableSecurityException which we rethrow.
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(newFileBytes)
                    os.flush()
                } ?: return Resource.Error("Unable to open output stream to write audio file.")
            } else {
                // nothing to write
                return Resource.Success(Unit)
            }

            return Resource.Success(Unit)
        } catch (e: RecoverableSecurityException) {
            Log.w(TAG, "RecoverableSecurityException while editing metadata: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to edit audio metadata", e)
            return Resource.Error(e.message ?: "Unknown error occurred while editing metadata.")
        }
    }

    // --- Helpers ---

    private fun resizeAndCompressImage(imageBytes: ByteArray): ByteArray {
        // Basic resize and JPEG compression (keeps it reasonably small)
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

    private fun encodeSyncSafe(size: Int): ByteArray {
        // 4 * 7-bit syncsafe
        return byteArrayOf(
            ((size shr 21) and 0x7F).toByte(),
            ((size shr 14) and 0x7F).toByte(),
            ((size shr 7) and 0x7F).toByte(),
            (size and 0x7F).toByte()
        )
    }

    private fun calculateSyncSafeInt(bytes: ByteArray): Int {
        // ensure signed bytes are normalized; ID3 syncsafe uses 7 bits per byte
        return ((bytes[0].toInt() and 0x7F) shl 21) or
                ((bytes[1].toInt() and 0x7F) shl 14) or
                ((bytes[2].toInt() and 0x7F) shl 7) or
                (bytes[3].toInt() and 0x7F)
    }

    private fun intToBigEndianBytes(i: Int): ByteArray {
        val buffer = ByteBuffer.allocate(4)
        buffer.order(ByteOrder.BIG_ENDIAN)
        buffer.putInt(i)
        return buffer.array()
    }

    private fun bigEndianToInt(bytes: ByteArray): Int {
        val buffer = ByteBuffer.wrap(bytes)
        buffer.order(ByteOrder.BIG_ENDIAN)
        return buffer.int
    }

    /**
     * Create a text frame (TIT2 / TPE1).
     * For ID3v2.4 we can use UTF-8 (encoding=3).
     * For ID3v2.3 we'll use encoding=1 (UTF-16 with BOM) for better unicode support.
     */
    private fun createTextFrame(frameId: String, text: String, tagVersion: Int): ByteArray {
        val (encodingByte, contentBytes) = if (tagVersion == 4) {
            // v2.4: use UTF-8 encoding (encoding byte = 3)
            3.toByte() to text.toByteArray(Charsets.UTF_8)
        } else {
            // v2.3: use UTF-16 with BOM (encoding byte = 1). Add BOM (FE FF) then UTF-16BE bytes.
            val bom = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
            val utf16 = text.toByteArray(Charsets.UTF_16BE)
            1.toByte() to (bom + utf16)
        }

        val frameContent = byteArrayOf(encodingByte) + contentBytes
        val frameSize = frameContent.size
        val sizeBytes = if (tagVersion == 4) encodeSyncSafe(frameSize) else intToBigEndianBytes(frameSize)

        val header = frameId.toByteArray(Charsets.ISO_8859_1) + sizeBytes + byteArrayOf(0, 0)
        return header + frameContent
    }

    /**
     * Create an APIC (album art) frame.
     * We will use text encoding 0 (ISO-8859-1) for the mime string and a blank description.
     */
    private fun createApicFrame(imageData: ByteArray, tagVersion: Int): ByteArray {
        val textEncoding = 0.toByte() // ISO-8859-1
        val mimeType = "image/jpeg".toByteArray(Charsets.ISO_8859_1)
        val pictureType = 3.toByte() // Cover (front)
        val description = byteArrayOf() // empty description

        // APIC content layout: textEncoding (1) + mime + 0 + pictureType + description + 0 + imageData
        val content = byteArrayOf(textEncoding) +
                mimeType + byteArrayOf(0) +
                byteArrayOf(pictureType) +
                description + byteArrayOf(0) +
                imageData

        val frameSize = content.size
        val sizeBytes = if (tagVersion == 4) encodeSyncSafe(frameSize) else intToBigEndianBytes(frameSize)
        val header = byteArrayOf('A'.toByte(), 'P'.toByte(), 'I'.toByte(), 'C'.toByte()) + sizeBytes + byteArrayOf(0, 0)
        return header + content
    }
}
