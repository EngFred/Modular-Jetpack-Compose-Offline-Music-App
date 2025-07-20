package com.engfred.musicplayer.feature_library.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.engfred.musicplayer.core.domain.model.AudioFile // Import AudioFile
import com.engfred.musicplayer.feature_library.presentation.components.AudioFileItem
import com.engfred.musicplayer.feature_library.presentation.viewmodel.LibraryEvent
import com.engfred.musicplayer.feature_library.presentation.viewmodel.LibraryScreenState
import com.engfred.musicplayer.feature_library.presentation.viewmodel.LibraryViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.PermissionState

/**
 * Composable screen for displaying the music library.
 * Handles permission requests and displays audio files or error/loading states.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onAudioFileClick: (String) -> Unit // Callback for when an audio file is clicked (pass URI string)
) {
    val uiState = viewModel.uiState
    val permission = viewModel.getRequiredPermission()
    val permissionState = rememberPermissionState(permission)
    var hasRequestedPermission by remember { mutableStateOf(false) } // Track if we've requested permission

    // Automatically request permission on first launch
    LaunchedEffect(permissionState.status, hasRequestedPermission) {
        if (!permissionState.status.isGranted && !hasRequestedPermission) {
            // First launch or after denial without "Don't ask again"
            permissionState.launchPermissionRequest()
            hasRequestedPermission = true // Prevent repeated requests
        }
        // Update ViewModel with permission status
        viewModel.onEvent(
            if (permissionState.status.isGranted) LibraryEvent.PermissionGranted
            else LibraryEvent.CheckPermission
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!uiState.hasStoragePermission) {
            PermissionRequestContent(
                permissionState = permissionState,
                permission = permission,
                shouldShowRationale = permissionState.status.shouldShowRationale,
                isPermanentlyDenied = !permissionState.status.isGranted && !permissionState.status.shouldShowRationale
            )
        } else {
            LibraryContent(
                uiState = uiState,
                onAudioFileClick = { audioFile ->
                    viewModel.onEvent(LibraryEvent.OnAudioFileClick(audioFile))
                    onAudioFileClick(audioFile.uri.toString())
                }
            )
        }
    }
}

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

@Composable
fun LibraryContent(
    uiState: LibraryScreenState, // Receive the full UI state
    onAudioFileClick: (AudioFile) -> Unit // Callback now takes AudioFile object
) {
    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        uiState.error != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Error: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        uiState.audioFiles.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No audio files found on your device.",
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        else -> {
            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.audioFiles) { audioFile ->
                    AudioFileItem(
                        audioFile = audioFile,
                        onClick = { clickedAudioFile ->
                            onAudioFileClick(clickedAudioFile) // Pass AudioFile directly
                        }
                    )
                }
            }
        }
    }
}