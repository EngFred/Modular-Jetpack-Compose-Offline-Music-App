package com.engfred.musicplayer.helpers

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

private const val TAG = "IntentPermissionHelper"

/**
 * Centralizes the incoming ACTION_VIEW handling and permission flow used by MainActivity.
 *
 * callbacks:
 *  - getRequiredPermission() -> String
 *  - onUriReady(uri)
 *  - onPendingUri(pending)
 *  - tryOpenUriStream(uri): Boolean
 *  - setLastHandledUri(str)
 *  - isLastHandledUri(str): Boolean
 */
object IntentPermissionHelper {

    fun handleIncomingIntent(
        activity: android.app.Activity,
        intent: Intent?,
        getRequiredPermission: () -> String,
        onUriReady: (Uri) -> Unit,
        onPendingUri: (Uri?) -> Unit,
        permissionLauncher: ActivityResultLauncher<String>,
        tryOpenUriStream: (Uri) -> Boolean,
        setLastHandledUri: (String) -> Unit,
        isLastHandledUri: (String) -> Boolean
    ) {
        try {
            if (intent == null) return

            if (intent.action == Intent.ACTION_VIEW) {
                val uri = intent.data ?: return
                val uriString = uri.toString()

                if (isLastHandledUri(uriString)) {
                    Log.d(TAG, "Intent URI already handled: $uriString")
                    return
                }

                setLastHandledUri(uriString)
                Log.d(TAG, "Incoming ACTION_VIEW with URI: $uriString")

                if (intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0) {
                    try {
                        activity.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        Log.d(TAG, "Persistable URI permission taken.")
                    } catch (e: SecurityException) {
                        Log.w(TAG, "Could not take persistable permission: ${e.message}")
                    } catch (e: Exception) {
                        Log.w(TAG, "takePersistableUriPermission failed: ${e.message}")
                    }
                }

                val canOpenNow = tryOpenUriStream(uri)
                if (canOpenNow) {
                    onUriReady(uri)
                    return
                }

                val requiredPerm = getRequiredPermission()
                if (ContextCompat.checkSelfPermission(activity, requiredPerm) == PackageManager.PERMISSION_GRANTED) {
                    onUriReady(uri)
                } else {
                    onPendingUri(uri)
                    permissionLauncher.launch(requiredPerm)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleIncomingIntent: ${e.message}", e)
        }
    }
}
