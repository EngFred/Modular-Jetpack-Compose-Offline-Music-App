package com.engfred.musicplayer.feature_library.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState
    val permission = viewModel.getRequiredPermission()
    val permissionState = rememberPermissionState(permission)
    var hasRequestedPermission by remember { mutableStateOf(false) }

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

    Column(modifier = Modifier.fillMaxSize()) {
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
                placeholder = "Search songs"
            )
            LibraryContent(
                uiState = uiState,
                viewModel = viewModel
            )
        }
    }
}