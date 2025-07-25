package com.engfred.musicplayer.core.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import com.engfred.musicplayer.core.domain.usecases.PermissionHandlerUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException

private const val TAG = "FileAccessUtils"

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
            Log.w(TAG, "Storage permission not granted. Cannot access file: $audioFileUri")
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
            // This can happen if permissions are revoked while the app is running,
            // or if the URI is for a file the app doesn't have direct access to,
            // even if general storage permission is granted (e.g., if the file
            // was deleted or moved).
            Log.e(TAG, "Security error accessing audio file (permission issue?): $audioFileUri, ${e.message}", e)
            isAccessible = false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error checking audio file accessibility: $audioFileUri, ${e.message}", e)
            isAccessible = false
        }
        return@withContext isAccessible
    }
}