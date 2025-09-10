package com.engfred.musicplayer.feature_library.presentation.screens

import LibraryEvent
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.ui.AddSongToPlaylistDialog
import com.engfred.musicplayer.core.ui.ConfirmationDialog
import com.engfred.musicplayer.feature_library.presentation.components.LibraryContent
import com.engfred.musicplayer.feature_library.presentation.components.PermissionRequestContent
import com.engfred.musicplayer.feature_library.presentation.components.SearchBar
import com.engfred.musicplayer.feature_library.presentation.viewmodel.LibraryViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    onEditSong: (AudioFile) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value
    val permission = viewModel.getRequiredPermission()
    val permissionState = rememberPermissionState(permission)
    // Persist across config changes so "permanently denied" detection is accurate after user requests.
    var hasRequestedPermission by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()

    val deleteMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val deletedAudioFile = uiState.audioFileToDelete

        if (result.resultCode == Activity.RESULT_OK) {
            deletedAudioFile?.let {
                viewModel.onEvent(LibraryEvent.DeletionResult(it, true, null))
            }
        } else {
            deletedAudioFile?.let {
                viewModel.onEvent(LibraryEvent.DeletionResult(it, false, "Deletion cancelled or failed."))
            } ?: run {
                viewModel.onEvent(LibraryEvent.DismissDeleteConfirmationDialog)
            }
        }
    }

    // React to permission state changes (granted or revoked) so ViewModel can update and UI follows
    LaunchedEffect(permissionState.status) {
        if (permissionState.status.isGranted) {
            // Permission granted — inform ViewModel so it can load library
            viewModel.onEvent(LibraryEvent.PermissionGranted)
        } else {
            // Not granted — keep ViewModel informed (it may show limited UI)
            viewModel.onEvent(LibraryEvent.CheckPermission)
        }
    }

    // Collect UI messages
    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Collect delete requests
    LaunchedEffect(viewModel.deleteRequest) {
        viewModel.deleteRequest.collect { intentSenderRequest ->
            deleteMediaLauncher.launch(intentSenderRequest)
        }
    }

    // Use the provided modifier so NavHost padding (innerPadding) can be applied by the parent.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                )
            )
    ) {
        // Permission flow: show PermissionRequestContent if permission not granted.
        // Important: we DO NOT auto-launch permission requests; we only show the UI and wait for user action.
        if (!uiState.hasStoragePermission) {
            PermissionRequestContent(
                shouldShowRationale = permissionState.status.shouldShowRationale,
                isPermanentlyDenied = (!permissionState.status.isGranted && !permissionState.status.shouldShowRationale && hasRequestedPermission),
                onRequestPermission = {
                    // User tapped "Grant Access" -> launch the system permission dialog
                    permissionState.launchPermissionRequest()
                    hasRequestedPermission = true
                },
                onOpenAppSettings = {
                    // Open the App settings page to allow user to manually grant permission
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                onContinueWithout = {
                    // User chooses to continue without granting permission (limited experience).
                    // Notify ViewModel so it can adjust UI/behavior accordingly.
                    viewModel.onEvent(LibraryEvent.CheckPermission)
                }
            )
        } else {
            // Permission granted → normal library UI
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { query ->
                    viewModel.onEvent(LibraryEvent.SearchQueryChanged(query))
                },
                placeholder = "Search songs",
                currentFilter = uiState.currentFilterOption,
                onFilterSelected = { filterOption ->
                    viewModel.onEvent(LibraryEvent.FilterSelected(filterOption))
                }
            )
            LibraryContent(
                uiState = uiState,
                onAudioClick = { audioFile ->
                    viewModel.onEvent(LibraryEvent.PlayAudio(audioFile))
                },
                isAudioPlaying = uiState.isPlaying,
                onRetry = { viewModel.onEvent(LibraryEvent.Retry) },
                onRemoveOrDelete = { audioFileToDelete ->
                    viewModel.onEvent(LibraryEvent.ShowDeleteConfirmation(audioFileToDelete))
                },
                onAddToPlaylist = {
                    viewModel.onEvent(LibraryEvent.AddedToPlaylist(it))
                },
                onPlayNext = {
                    viewModel.onEvent(LibraryEvent.PlayedNext(it))
                },
                lazyListState = lazyListState,
                onEditSong = onEditSong
            )
        }
    }

    // Dialogs & Floating flows (unchanged behavior)
    if (uiState.showAddToPlaylistDialog) {
        AddSongToPlaylistDialog(
            onDismiss = { viewModel.onEvent(LibraryEvent.DismissAddToPlaylistDialog) },
            playlists = uiState.playlists,
            onAddSongToPlaylist = { playlist ->
                viewModel.onEvent(LibraryEvent.AddedSongToPlaylist(playlist))
            }
        )
    }

    if (uiState.showDeleteConfirmationDialog) {
        uiState.audioFileToDelete?.let { audioFile ->
            ConfirmationDialog(
                title = "Delete '${audioFile.title}'?",
                message = "Are you sure you want to permanently delete '${audioFile.title}' from your device? This action cannot be undone.",
                confirmButtonText = "Delete",
                dismissButtonText = "Cancel",
                onConfirm = {
                    viewModel.onEvent(LibraryEvent.ConfirmDeleteAudioFile)
                },
                onDismiss = {
                    viewModel.onEvent(LibraryEvent.DismissDeleteConfirmationDialog)
                }
            )
        } ?: run {
            viewModel.onEvent(LibraryEvent.DismissDeleteConfirmationDialog)
        }
    }
}
