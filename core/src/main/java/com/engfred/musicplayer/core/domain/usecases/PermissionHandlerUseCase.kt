package com.engfred.musicplayer.core.domain.usecases

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Use case to handle audio file access permissions.
 * Provides methods to check if permission is granted and to determine if rationale is needed.
 */
class PermissionHandlerUseCase (
    private val context: Context
) {

    /**
     * Checks if the necessary audio READ permissions are granted.
     * For Android 13 (API 33) and above, READ_MEDIA_AUDIO is required.
     * For Android 12 (API 32) and below, READ_EXTERNAL_STORAGE is required.
     * @return True if permissions are granted, false otherwise.
     */
    fun hasAudioPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks if WRITE_EXTERNAL_STORAGE permission is granted.
     * This permission is primarily relevant for direct file operations on Android 9 (API 28) and below.
     * For Android 10 (API 29) and above, MediaStore.createDeleteRequest handles deletion consent
     * via a system dialog, making this permission less critical for media file deletion.
     *
     * @return True if WRITE_EXTERNAL_STORAGE is granted (on relevant versions), or always true for API 29+.
     */
    fun hasWriteStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { // Android 9 (API 28) and below
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // For Android 10 (Q) and above, direct WRITE_EXTERNAL_STORAGE is largely deprecated
            // for media files in shared storage. MediaStore APIs with user consent are used instead.
            true // Assume MediaStore APIs will handle permission via user consent dialog.
        }
    }

    /**
     * Returns the appropriate permission string for reading audio based on the Android version.
     */
    fun getRequiredReadPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    /**
     * Returns the appropriate permission string for writing to external storage if applicable.
     */
    fun getRequiredWritePermission(): String? {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        } else {
            null // Not directly needed for Q+ for media deletion via MediaStore
        }
    }
}