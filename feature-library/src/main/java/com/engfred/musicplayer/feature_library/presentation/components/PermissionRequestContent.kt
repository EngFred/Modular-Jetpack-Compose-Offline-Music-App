package com.engfred.musicplayer.feature_library.presentation.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestContent(
    permissionState: PermissionState,
    permission: String,
    shouldShowRationale: Boolean,
    isPermanentlyDenied: Boolean
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Permission Required",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = when {
                shouldShowRationale -> {
                    "This app needs access to your audio files to play music. Please grant the permission to continue."
                }
                isPermanentlyDenied -> {
                    "Audio file access was permanently denied. Please enable it in App Settings to use the app."
                }
                else -> {
                    "Please grant access to audio files to use the music player."
                }
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            if (shouldShowRationale || !isPermanentlyDenied) {
                // Request permission for first launch or rationale case
                permissionState.launchPermissionRequest()
            } else {
                // Direct to app settings for permanently denied case
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        }) {
            Text(
                text = if (isPermanentlyDenied) "Open App Settings" else "Grant Permission"
            )
        }
    }
}