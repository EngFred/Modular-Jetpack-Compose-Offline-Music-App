package com.engfred.musicplayer.feature_library.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.hilt.navigation.compose.hiltViewModel
import com.engfred.musicplayer.feature_library.presentation.components.LibraryContent
import com.engfred.musicplayer.feature_library.presentation.components.PermissionRequestContent
import com.engfred.musicplayer.feature_library.presentation.components.SearchBar
import com.engfred.musicplayer.feature_library.presentation.viewmodel.LibraryEvent
import com.engfred.musicplayer.feature_library.presentation.viewmodel.LibraryViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.collectAsState
import com.engfred.musicplayer.core.ui.CustomSnackbar

/**
 * Composable for the Library screen, displaying a search bar and audio file list or permission request.
 *
 * @param viewModel The ViewModel for managing library state and events.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LibraryScreen(
    onSwipeToNowPlaying: (audioFileUri: String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value
    val permission = viewModel.getRequiredPermission()
    val permissionState = rememberPermissionState(permission)
    var hasRequestedPermission by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(permissionState.status, hasRequestedPermission) {
        if (!permissionState.status.isGranted && !hasRequestedPermission) {
            permissionState.launchPermissionRequest()
            hasRequestedPermission = true
        }
        viewModel.onEvent(
            if (permissionState.status.isGranted) LibraryEvent.PermissionGranted
            else LibraryEvent.CheckPermission
        )
    }

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            ) { data ->
                CustomSnackbar(snackbarData = data)
            }
        },
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
                    permission = permission,
                    shouldShowRationale = permissionState.status.shouldShowRationale,
                    isPermanentlyDenied = !permissionState.status.isGranted && !permissionState.status.shouldShowRationale
                )
            } else {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { query ->
                        viewModel.onEvent(LibraryEvent.OnSearchQueryChanged(query))
                    },
                    placeholder = "Search songs",
                    onFilterSelected = { filterOption ->
                        viewModel.onEvent(LibraryEvent.OnFilterSelected(filterOption))
                    }
                )
                LibraryContent(
                    uiState = uiState,
                    onAudioClick = { audioFile ->
                        viewModel.onEvent(LibraryEvent.OnAudioFileClick(audioFile))
                    },
                    onSwipeToNowPlaying = { audioFile ->
                        viewModel.onEvent(LibraryEvent.OnSwipeToNowPlaying(audioFile))
                        onSwipeToNowPlaying(audioFile.uri.toString())
                    },
                    onMenuOptionSelected = { option, audioFile ->
                        viewModel.onEvent(LibraryEvent.OnMenuOptionSelected(option, audioFile))
                    },
                    onRetry = { viewModel.onEvent(LibraryEvent.LoadAudioFiles) },
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}