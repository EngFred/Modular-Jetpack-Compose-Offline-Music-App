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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    val owner = LocalLifecycleOwner.current

    // Track if permission has been requested at least once
    var hasRequestedPermission by rememberSaveable { mutableStateOf(false) }
    // Track if we're currently showing the permission dialog
    var isPermissionDialogShowing by rememberSaveable { mutableStateOf(false) }

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

    // React to permission state changes:
    // IMPORTANT: reset isPermissionDialogShowing whenever a response is observed (granted or denied)
    LaunchedEffect(permissionState.status) {
        // user responded to permission dialog — hide the "dialog showing" UI
        isPermissionDialogShowing = false

        if (permissionState.status.isGranted) {
            // Permission granted
            viewModel.onEvent(LibraryEvent.PermissionGranted)
        } else {
            // Permission not granted - update ViewModel state (and optionally surface a message)
            viewModel.onEvent(LibraryEvent.CheckPermission)
        }
    }

    DisposableEffect(key1 = Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Re-evaluate permission state after returning from Settings
                viewModel.onEvent(LibraryEvent.CheckPermission)
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
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
        if (!uiState.hasStoragePermission) {
            PermissionRequestContent(
                shouldShowRationale = permissionState.status.shouldShowRationale,
                isPermanentlyDenied = (!permissionState.status.isGranted &&
                        !permissionState.status.shouldShowRationale &&
                        hasRequestedPermission),
                isPermissionDialogShowing = isPermissionDialogShowing,
                onRequestPermission = {
                    // User tapped "Grant Access" -> launch the system permission dialog
                    permissionState.launchPermissionRequest()
                    hasRequestedPermission = true
                    isPermissionDialogShowing = true
                },
                onOpenAppSettings = {
                    // Open the App settings page to allow user to manually grant permission
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
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

    // Dialogs & Floating flows (unchanged)...
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
