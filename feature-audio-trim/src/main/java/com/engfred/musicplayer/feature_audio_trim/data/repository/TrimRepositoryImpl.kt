package com.engfred.musicplayer.feature_audio_trim.data.repository

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.*
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.feature_audio_trim.domain.model.TrimResult
import com.engfred.musicplayer.feature_audio_trim.domain.repository.TrimRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

private const val TAG = "TrimRepository"

@UnstableApi
class TrimRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TrimRepository {

    override suspend fun trimAudio(
        audioFile: AudioFile,
        startMs: Long,
        endMs: Long
    ): Flow<TrimResult> = callbackFlow {
        // Launch the core trimming logic in a coroutine scope
        coroutineScope {
            launch(Dispatchers.IO) {
                performTrim(this@callbackFlow, audioFile, startMs, endMs)
            }
        }
        // No explicit cleanup needed in awaitClose, as cancellation is handled in performTrim
        awaitClose { /* No-op */ }
    }

    /**
     * Executes the actual trimming process using Media3 Transformer and handles file saving.
     */
    private suspend fun performTrim(
        channel: kotlinx.coroutines.channels.SendChannel<TrimResult>,
        audioFile: AudioFile,
        startMs: Long,
        endMs: Long
    ) {
        var transformer: Transformer? = null
        val done = CompletableDeferred<Unit>()

        try {
            if (!coroutineContext.isActive) return

            // 1. Setup Input and Output Files
            val inputUri = audioFile.uri
            val outputDir = context.getExternalFilesDir(null)
            val outputFileName = "trimmed_${audioFile.id}_${System.currentTimeMillis()}.m4a"
            val outputFile = File(outputDir, outputFileName)

            // 2. Configure Media Item for Trimming (Clipping)
            val mediaItem = MediaItem.Builder()
                .setUri(inputUri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startMs)
                        .setEndPositionMs(endMs)
                        .build()
                )
                .build()

            // 3. Configure Edited Media Item (Remove video, ensure audio processing)
            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .build()

            // 4. Initialize Transformer (Must be done on Main thread due to Media3 requirements)
            transformer = withContext(Dispatchers.Main) {
                Transformer.Builder(context)
                    .experimentalSetTrimOptimizationEnabled(false) // Disable for better progress reporting
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            done.complete(Unit)
                        }

                        override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                            done.completeExceptionally(exportException)
                        }

                        override fun onFallbackApplied(
                            composition: Composition,
                            originalTransformationRequest: TransformationRequest,
                            fallbackTransformationRequest: TransformationRequest
                        ) {
                            // Optional: Handle fallback if needed
                        }
                    })
                    .build()
            }

            // 5. Start Transformation
            withContext(Dispatchers.Main) {
                transformer.start(editedMediaItem, outputFile.absolutePath)
            }

            // 6. Await Completion (no polling needed)
            done.await() // Wait for completion; throws on error

            // 7. MediaStore Save Logic
            val newDuration = endMs - startMs
            if (newDuration <= 0) {
                channel.trySend(TrimResult.Error("Trim duration too short"))
                return
            }

            val trimmedTitle = "${audioFile.title} (trimmed)"
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, "$trimmedTitle.m4a")
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                put(MediaStore.Audio.Media.TITLE, trimmedTitle)
                put(MediaStore.Audio.Media.ARTIST, audioFile.artist ?: "Unknown Artist")
                put(MediaStore.Audio.Media.ALBUM, audioFile.album ?: "Unknown Album")
                put(MediaStore.Audio.Media.DURATION, newDuration)
                put(MediaStore.Audio.Media.DATE_ADDED, System.currentTimeMillis() / 1000L)
                put(MediaStore.Audio.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000L)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
            }

            val newUri = context.contentResolver.insert(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw IOException("Failed to insert into MediaStore")

            // Copy file content from temp output to MediaStore URI
            context.contentResolver.openOutputStream(newUri)?.use { outStream ->
                outputFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outStream)
                }
            } ?: throw IOException("Cannot write to new MediaStore URI")

            // Finalize MediaStore entry for Android Q+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updateValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }
                context.contentResolver.update(newUri, updateValues, null, null)
            }

            // 8. Media Scan and Cleanup
            val newPath = getFilePath(context, newUri)
            newPath?.let {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(it),
                    arrayOf("audio/mp4"),
                    null
                )
            }

            outputFile.delete()

            Log.d(TAG, "Trim process completed successfully")
            channel.trySend(TrimResult.Success)

        } catch (e: IOException) {
            Log.e(TAG, "IO error during trim", e)
            channel.trySend(TrimResult.Error(e.message ?: "Trim failed: IO Error"))
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied during trim", e)
            channel.trySend(TrimResult.PermissionDenied)
        } catch (e: ExportException) {
            Log.e(TAG, "Export error during trim", e)
            channel.trySend(TrimResult.Error(e.message ?: "Trim failed: Export Error"))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during trim", e)
            channel.trySend(TrimResult.Error(e.message ?: "Trim failed: Unexpected Error"))
        } finally {
            // Cleanup: transformer
            transformer?.let { t ->
                withContext(Dispatchers.Main) {
                    t.cancel()
                }
            }
        }
    }

    /**
     * Utility to get the file path from a MediaStore URI for use with MediaScannerConnection.
     */
    private fun getFilePath(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)
            } else null
        }
    }
}