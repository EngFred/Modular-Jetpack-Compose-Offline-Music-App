package com.engfred.musicplayer.feature_library.presentation.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestContent(
    permissionState: PermissionState,
    shouldShowRationale: Boolean,
    isPermanentlyDenied: Boolean
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp), // Increased padding for more breathing room
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- Icon for visual emphasis ---
        Icon(
            imageVector = Icons.Default.Info, // Placeholder, consider a music-related icon
            contentDescription = "Permission icon",
            modifier = Modifier.size(64.dp), // Larger icon
            tint = MaterialTheme.colorScheme.primary // Use primary color for prominence
        )
        Spacer(modifier = Modifier.height(24.dp))

        // --- Title ---
        Text(
            text = "Music Library Access Needed",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), // Bolder title
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        // --- Descriptive Message ---
        Text(
            text = when {
                shouldShowRationale -> {
                    "To play your favorite songs, Music Player needs access to your device's audio files. Granting this permission allows us to discover and organize your music library."
                }
                isPermanentlyDenied -> {
                    "It looks like you've permanently denied music library access. To enable this feature, please go to App Settings and grant the necessary permission manually."
                }
                else -> {
                    "Please grant access to your music library to enable Music Player to discover and play your audio files."
                }
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant // Slightly less prominent than onBackground
        )
        Spacer(modifier = Modifier.height(32.dp)) // More space before buttons

        // --- Primary Action Button ---
        // Using FilledTonalButton for a slightly less prominent but still clear action
        FilledTonalButton(
            onClick = {
                if (shouldShowRationale || !isPermanentlyDenied) {
                    permissionState.launchPermissionRequest()
                } else {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f) // Wider button
        ) {
            Text(
                text = if (isPermanentlyDenied) "Go to App Settings" else "Grant Access",
                style = MaterialTheme.typography.titleMedium // Slightly larger text
            )
        }

        // --- Optional Secondary Action (e.g., "Learn More" or "Not Now") ---
        if (shouldShowRationale) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    // Handle "Not Now" or "Learn More" action
                    // For example, navigate to a limited mode or show more info
                }
            ) {
                Text(text = "Not Now", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}