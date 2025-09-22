package com.engfred.musicplayer.feature_library.data.repository

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
                artistId = dto.artistId
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
                    artistId = dto.artistId
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
     * Streaming-safe metadata editor. Does NOT load entire audio file into memory.
     *
     * - Reads only the ID3 tag header + tag bytes to parse existing frames.
     * - Builds new tag frames in memory (small).
     * - Streams remaining audio bytes into a temp file (buffered).
     * - Writes temp file back to the media store via ContentResolver output stream.
     */
    override suspend fun editAudioMetadata(
        id: Long,
        newTitle: String?,
        newArtist: String?,
        newAlbumArt: ByteArray?,
        context: Context
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

        try {
            // Quick MIME check (optional)
            val projection = arrayOf(MediaStore.Audio.Media.MIME_TYPE)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            val mimeType = cursor?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
            if (mimeType != null && mimeType !in listOf("audio/mpeg", "audio/mp3")) {
                return@withContext Resource.Error("Only MP3 files are supported for metadata editing.")
            }

            // Resize/compress album art if provided
            val processedAlbumArt = newAlbumArt?.let { resizeAndCompressImage(it) }

            // Step 1: Open input stream and read header + tag (if present)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // read first 10 bytes (ID3 header) if present
                val headerBuf = ByteArray(10)
                val headerRead = readFully(inputStream, headerBuf, 0, 10)
                var tagVersion = 4
                val existingFrames = mutableListOf<ByteArray>()

                if (headerRead == 10 &&
                    headerBuf[0] == 'I'.toByte() &&
                    headerBuf[1] == 'D'.toByte() &&
                    headerBuf[2] == '3'.toByte()
                ) {
                    val versionByte = headerBuf[3].toInt() and 0xFF
                    tagVersion = if (versionByte == 4) 4 else 3
                    val tagSize = calculateSyncSafeInt(headerBuf.copyOfRange(6, 10))

                    // read the tag block (small)
                    val tagData = ByteArray(tagSize)
                    val tagRead = readFully(inputStream, tagData, 0, tagSize)
                    if (tagRead != tagSize) {
                        Log.w(TAG, "Could not fully read tag: expected $tagSize got $tagRead")
                    }

                    // parse frames from tagData
                    var pos = 0
                    while (pos + 10 <= tagData.size) {
                        val idBytes = tagData.copyOfRange(pos, pos + 4)
                        if (idBytes.all { it == 0.toByte() }) break // padding
                        val frameId = String(idBytes, Charsets.ISO_8859_1)
                        val frameSizeBytes = tagData.copyOfRange(pos + 4, pos + 8)
                        val frameSize = if (tagVersion == 4) calculateSyncSafeInt(frameSizeBytes) else bigEndianToInt(frameSizeBytes)
                        val flags = tagData.copyOfRange(pos + 8, pos + 10)

                        if (frameSize <= 0 || pos + 10 + frameSize > tagData.size) break

                        val frameData = tagData.copyOfRange(pos + 10, pos + 10 + frameSize)

                        // Recreate raw frame bytes (header + data) to preserve untouched frames
                        val sizeForHeader = if (tagVersion == 4) encodeSyncSafe(frameSize) else intToBigEndianBytes(frameSize)
                        val headerForFrame = idBytes + sizeForHeader + flags
                        val fullFrame = headerForFrame + frameData

                        when (frameId) {
                            "TIT2" -> if (newTitle == null) existingFrames.add(fullFrame)
                            "TPE1" -> if (newArtist == null) existingFrames.add(fullFrame)
                            "APIC" -> if (newAlbumArt == null) existingFrames.add(fullFrame)
                            else -> existingFrames.add(fullFrame)
                        }
                        pos += 10 + frameSize
                    }

                    // inputStream is already positioned after header+tag because we read them.
                } else {
                    // no ID3 tag; reopen stream to position 0
                    inputStream.close()
                    context.contentResolver.openInputStream(uri)?.use { ins2 ->
                        // We'll stream the entire audio payload from ins2 (we haven't consumed tag)
                        performStreamingWrite(
                            ins2 = ins2,
                            preservedFrames = existingFrames,
                            newTitle = newTitle,
                            newArtist = newArtist,
                            processedAlbumArt = processedAlbumArt,
                            tagVersion = tagVersion,
                            context = context,
                            uri = uri
                        )
                    } ?: return@withContext Resource.Error("Unable to re-open input stream.")
                    scanFile(context, uri)
                    return@withContext Resource.Success(Unit)
                }

                // At this point inputStream is positioned right after the tag (or at 0 if no tag).
                // We will write new tag + copy rest of audio from current inputStream into temp file, then stream temp file to content resolver.

                // Build new frames (in-memory; small)
                newTitle?.let { existingFrames.add(createTextFrame("TIT2", it, tagVersion)) }
                newArtist?.let { existingFrames.add(createTextFrame("TPE1", it, tagVersion)) }
                processedAlbumArt?.let { existingFrames.add(createApicFrame(it, tagVersion)) }

                val totalFramesSize = existingFrames.fold(0) { acc, b -> acc + b.size }
                if (totalFramesSize <= 0) {
                    // nothing to write
                    return@withContext Resource.Success(Unit)
                }

                val headerBytes = byteArrayOf(
                    'I'.toByte(), 'D'.toByte(), '3'.toByte(),
                    tagVersion.toByte(), 0.toByte(), // revision=0
                    0.toByte() // flags = 0
                ) + encodeSyncSafe(totalFramesSize)

                // Temp file to hold new content
                val tempFile = File.createTempFile("edit_audio_", ".tmp", context.cacheDir)
                try {
                    FileOutputStream(tempFile).use { fos ->
                        // write ID3 header + frames
                        fos.write(headerBytes)
                        for (frame in existingFrames) fos.write(frame)

                        // copy the remainder of inputStream (audio payload) to temp - streaming copy
                        val buffer = ByteArray(8 * 1024)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } > 0) {
                            fos.write(buffer, 0, read)
                        }
                        fos.flush()
                    }

                    // Now write tempFile back to media store (this may throw RecoverableSecurityException)
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(tempFile).use { fis ->
                            val buf = ByteArray(8 * 1024)
                            var r: Int
                            while (fis.read(buf).also { r = it } > 0) {
                                out.write(buf, 0, r)
                            }
                            out.flush()
                        }
                    } ?: return@withContext Resource.Error("Unable to open output stream to write audio file.")

                    scanFile(context, uri)
                    return@withContext Resource.Success(Unit)
                } finally {
                    try { tempFile.delete() } catch (_: Exception) { /* ignore */ }
                }
            } ?: return@withContext Resource.Error("Unable to open audio file input stream.")
        } catch (re: RecoverableSecurityException) {
            Log.w(TAG, "RecoverableSecurityException while editing metadata: ${re.message}")
            throw re
        } catch (e: Exception) {
            Log.e(TAG, "Failed to edit audio metadata", e)
            return@withContext Resource.Error(e.message ?: "Unknown error occurred while editing metadata.")
        }
    }

    // Helper that was split out for the case we need to re-open the input stream and stream everything:
    private fun performStreamingWrite(
        ins2: InputStream,
        preservedFrames: MutableList<ByteArray>,
        newTitle: String?,
        newArtist: String?,
        processedAlbumArt: ByteArray?,
        tagVersion: Int,
        context: Context,
        uri: Uri
    ) {
        // This helper mirrors logic above but accepts an input stream positioned at 0.
        // For brevity inlining a minimal streaming process here (called only in the edge-case where
        // the first header read wasn't ID3 and we consumed bytes) — in most runs above is used.
        val localTagVersion = tagVersion
        val existingFrames = preservedFrames
        newTitle?.let { existingFrames.add(createTextFrame("TIT2", it, localTagVersion)) }
        newArtist?.let { existingFrames.add(createTextFrame("TPE1", it, localTagVersion)) }
        processedAlbumArt?.let { existingFrames.add(createApicFrame(it, localTagVersion)) }

        val totalFramesSize = existingFrames.fold(0) { acc, b -> acc + b.size }
        if (totalFramesSize <= 0) return

        val headerBytes = byteArrayOf(
            'I'.toByte(), 'D'.toByte(), '3'.toByte(),
            localTagVersion.toByte(), 0.toByte(),
            0.toByte()
        ) + encodeSyncSafe(totalFramesSize)

        val tempFile = File.createTempFile("edit_audio_", ".tmp", context.cacheDir)
        try {
            FileOutputStream(tempFile).use { fos ->
                fos.write(headerBytes)
                for (frame in existingFrames) fos.write(frame)
                val buffer = ByteArray(8 * 1024)
                var r: Int
                while (ins2.read(buffer).also { r = it } > 0) {
                    fos.write(buffer, 0, r)
                }
                fos.flush()
            }

            context.contentResolver.openOutputStream(uri)?.use { out ->
                FileInputStream(tempFile).use { fis ->
                    val buf = ByteArray(8 * 1024)
                    var r: Int
                    while (fis.read(buf).also { r = it } > 0) {
                        out.write(buf, 0, r)
                    }
                    out.flush()
                }
            }
            scanFile(context, uri)
        } finally {
            try { tempFile.delete() } catch (_: Exception) { }
        }
    }

    private fun scanFile(context: Context, uri: Uri) {
        val path = getFilePath(context, uri) ?: return
        MediaScannerConnection.scanFile(
            context,
            arrayOf(path),
            arrayOf("audio/mpeg"),
            null
        )
    }

    private fun getFilePath(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return null
    }

    // --- Utility helpers ---

    private fun readFully(stream: InputStream, buf: ByteArray, offset: Int, length: Int): Int {
        var readTotal = 0
        var pos = offset
        var remaining = length
        while (remaining > 0) {
            val r = stream.read(buf, pos, remaining)
            if (r == -1) break
            readTotal += r
            pos += r
            remaining -= r
        }
        return readTotal
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

    private fun encodeSyncSafe(size: Int): ByteArray {
        return byteArrayOf(
            ((size shr 21) and 0x7F).toByte(),
            ((size shr 14) and 0x7F).toByte(),
            ((size shr 7) and 0x7F).toByte(),
            (size and 0x7F).toByte()
        )
    }

    private fun calculateSyncSafeInt(bytes: ByteArray): Int {
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

    private fun createTextFrame(frameId: String, text: String, tagVersion: Int): ByteArray {
        val (encodingByte, contentBytes) = if (tagVersion == 4) {
            3.toByte() to text.toByteArray(Charsets.UTF_8)
        } else {
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

    private fun createApicFrame(imageData: ByteArray, tagVersion: Int): ByteArray {
        val textEncoding = 0.toByte()
        val mimeType = "image/jpeg".toByteArray(Charsets.ISO_8859_1)
        val pictureType = 3.toByte()
        val description = byteArrayOf()

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