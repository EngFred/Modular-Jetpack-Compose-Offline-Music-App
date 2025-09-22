package com.engfred.musicplayer.core.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.graphics.scale
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.usecases.PermissionHandlerUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Locale

object MediaUtils {

    private const val TAG = "MediaUtils"

    /**
     * Checks if an audio file represented by a Uri is accessible (exists and permissions are granted).
     * This function should be called before attempting to play a local audio file.
     *
     * @param context The application context.
     * @param audioFileUri The Uri of the audio file to check.
     * @param permissionHandlerUseCase An instance of PermissionHandlerUseCase to check storage permissions.
     * @return True if the file exists and is accessible, false otherwise.
     */
    suspend fun isAudioFileAccessible(
        context: Context,
        audioFileUri: Uri,
        permissionHandlerUseCase: PermissionHandlerUseCase
    ): Boolean {
        return withContext(Dispatchers.IO) { // Perform I/O operations on the IO dispatcher
            // 1. Check if storage permissions are granted
            if (!permissionHandlerUseCase.hasAudioPermission()) {
                Log.w(TAG, "Storage read permission not granted. Cannot access file: $audioFileUri")
                return@withContext false
            }

            // 2. Check if the file exists and is readable via ContentResolver
            val contentResolver: ContentResolver = context.contentResolver
            var isAccessible = false
            try {
                // Attempt to open an InputStream to check for existence and readability
                // Using use{} ensures the InputStream is closed automatically
                contentResolver.openInputStream(audioFileUri)?.use {
                    isAccessible = true
                    Log.d(TAG, "Audio file found and accessible: $audioFileUri")
                } ?: run {
                    Log.w(TAG, "Audio file not found at URI: $audioFileUri")
                    isAccessible = false
                }
            } catch (e: FileNotFoundException) {
                Log.w(TAG, "Audio file not found at URI (FileNotFoundException): $audioFileUri, ${e.message}")
                isAccessible = false
            } catch (e: IOException) {
                Log.e(TAG, "I/O error accessing audio file: $audioFileUri, ${e.message}", e)
                isAccessible = false
            } catch (e: SecurityException) {
                Log.e(TAG, "Security error accessing audio file (permission issue?): $audioFileUri, ${e.message}", e)
                isAccessible = false
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error checking audio file accessibility: $audioFileUri, ${e.message}", e)
                isAccessible = false
            }
            return@withContext isAccessible
        }
    }

    /**
     * Deletes an audio file from the device's storage using MediaStore API.
     * For Android 10 (API 29) and above, this will trigger a system dialog
     * asking the user for confirmation to delete the file.
     *
     * @param context The application context.
     * @param audioFile The AudioFile object to be deleted.
     * @param onPreQDeletionResult Callback for Android versions < Q where deletion is synchronous.
     * True if deleted, false if not.
     * @return [IntentSender] if Android 10 (API 29) or higher, which needs to be launched via
     * [startIntentSenderForResult] from an Activity. Null for older Android versions
     * or if an error occurs immediately.
     */
    fun deleteAudioFile(
        context: Context,
        audioFile: AudioFile,
        onPreQDeletionResult: (Boolean, String?) -> Unit
    ): IntentSender? {
        val contentUri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            audioFile.id
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10 (API 29) and above
                val urisToDelete = arrayListOf(contentUri)
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, urisToDelete)
                return pendingIntent.intentSender // Return the IntentSender
            } else { // Android 9 (API 28) and below
                val rowsDeleted = context.contentResolver.delete(contentUri, null, null)
                if (rowsDeleted > 0) {
                    Log.d(TAG, "Successfully deleted file: ${audioFile.title} (Rows deleted: $rowsDeleted)")
                    onPreQDeletionResult(true, null)
                } else {
                    val message = "Failed to delete file directly: ${audioFile.title}. Rows deleted: $rowsDeleted. Might not exist or permission denied."
                    Log.w(TAG, message)
                    onPreQDeletionResult(false, message)
                }
                return null // No IntentSender needed for pre-Q
            }
        } catch (e: Exception) {
            val message = "Error deleting file ${audioFile.title} (ID: ${audioFile.id}): ${e.message}"
            Log.e(TAG, message, e)
            onPreQDeletionResult(false, message) // Report failure immediately
            return null
        }
    }
    fun formatDuration(milliseconds: Long): String {
        if (milliseconds <= 0) return "00:00"
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    fun compressImage(imageBytes: ByteArray): ByteArray {
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
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

        bitmap.recycle()
        scaledBitmap.recycle()

        return outputStream.toByteArray()
    }

    // Existing shareAudioFile function
    fun shareAudioFile(context: Context, audioFile: AudioFile) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, audioFile.uri)
                putExtra(Intent.EXTRA_SUBJECT, "Sharing song: ${audioFile.title}")
                putExtra(Intent.EXTRA_TEXT, "Listen to '${audioFile.title}' by ${audioFile.artist}!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant temp read permission to the receiving app
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share song via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share song.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Error sharing audio file: ${e.message}", e)
        }
    }
}