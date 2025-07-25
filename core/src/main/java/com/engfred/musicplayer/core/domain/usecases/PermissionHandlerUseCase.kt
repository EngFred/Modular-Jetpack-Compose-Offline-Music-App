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
     * Checks if the necessary audio permissions are granted.
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
     * Returns the appropriate permission string based on the Android version.
     */
    fun getRequiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
}