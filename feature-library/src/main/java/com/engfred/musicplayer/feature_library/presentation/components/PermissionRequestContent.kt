package com.engfred.musicplayer.feature_library.presentation.components

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
import androidx.compose.material3.Button
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

/**
 * PermissionRequestContent
 *
 * A user-driven permission UI that only triggers the system permission dialog when the user taps
 * the primary action. If the user permanently denied permissions, we show a clear action to open
 * the App Settings page so they can enable permissions manually.
 *
 * - shouldShowRationale: whether the OS recommends showing a rationale.
 * - isPermanentlyDenied: a higher-level computed boolean (caller decides when to treat as permanent).
 * - onRequestPermission: callback to call when user asks to request permission (caller should call launchPermissionRequest()).
 * - onOpenAppSettings: open application's settings page.
 * - onContinueWithout: allow user to continue in limited mode without permission.
 */
@Composable
fun PermissionRequestContent(
    shouldShowRationale: Boolean,
    isPermanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onContinueWithout: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Permission information",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Music needs access to your audio files",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = when {
                isPermanentlyDenied -> {
                    // User has previously denied permanently (caller computed this)
                    "You have permanently denied access to the music library. To enable full functionality, open the app settings and grant storage permission to Music."
                }
                shouldShowRationale -> {
                    // OS recommends showing a rationale
                    "Music needs permission to access your audio files so you can discover, browse and play your songs. Please grant access to continue."
                }
                else -> {
                    // First-time or undetermined state
                    "To discover and play your songs, Music requires permission to access audio files on your device. Tap Grant Access to allow."
                }
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Main action: either request permission or go to settings
        if (isPermanentlyDenied) {
            Button(
                onClick = { onOpenAppSettings() },
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Text(text = "Open App Settings", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            FilledTonalButton(
                onClick = { onRequestPermission() },
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Text(text = "Grant Access", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Secondary action: allow user to continue without permission (limited experience)
        TextButton(
            onClick = { onContinueWithout() },
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Text(text = "Continue without access", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
