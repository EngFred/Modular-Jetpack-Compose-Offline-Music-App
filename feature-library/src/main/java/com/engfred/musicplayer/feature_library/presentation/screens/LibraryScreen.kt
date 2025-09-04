package com.engfred.musicplayer.feature_library.presentation.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
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
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value
    val permission = viewModel.getRequiredPermission()
    val permissionState = rememberPermissionState(permission)
    var hasRequestedPermission by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val deleteMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val deletedAudioFile = uiState.audioFileToDelete

        if (result.resultCode == Activity.RESULT_OK) {
            deletedAudioFile?.let {
                viewModel.onEvent(LibraryEvent.DeletionResult(it, true, null)) // Signal success
            }
        } else {
            deletedAudioFile?.let {
                viewModel.onEvent(LibraryEvent.DeletionResult(it, false, "Deletion cancelled or failed."))
            } ?: run {
                viewModel.onEvent(LibraryEvent.DismissDeleteConfirmationDialog)
            }
        }
    }

    LaunchedEffect(permissionState.status, hasRequestedPermission, uiState.hasStoragePermission) {
        if (!permissionState.status.isGranted && !hasRequestedPermission) {
            permissionState.launchPermissionRequest()
            hasRequestedPermission = true
        }

        if (permissionState.status.isGranted) {
            viewModel.onEvent(LibraryEvent.PermissionGranted)
        } else {
            viewModel.onEvent(LibraryEvent.CheckPermission)
        }
    }

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(viewModel.deleteRequest) {
        viewModel.deleteRequest.collect { intentSenderRequest ->
            deleteMediaLauncher.launch(intentSenderRequest)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                )
            )
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!uiState.hasStoragePermission) {
                PermissionRequestContent(
                    permissionState = permissionState,
                    shouldShowRationale = permissionState.status.shouldShowRationale,
                    isPermanentlyDenied = !permissionState.status.isGranted && !permissionState.status.shouldShowRationale
                )
            } else {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { query ->
                        viewModel.onEvent(LibraryEvent.SearchQueryChanged(query))
                    },
                    placeholder = "Search songs",
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
                    }
                )
            }
        }
    }

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
            // Use the generic ConfirmationDialog from core.ui
            ConfirmationDialog(
                title = "Delete '${audioFile.title}'?",
                message = "Are you sure you want to permanently delete '${audioFile.title}' from your device? This action cannot be undone.", // Clear message
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
            // If dialog is somehow shown without an audioFileToDelete, dismiss it.
            viewModel.onEvent(LibraryEvent.DismissDeleteConfirmationDialog)
        }
    }
}