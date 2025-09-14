package com.engfred.musicplayer.feature_library.presentation.screens

import LibraryEvent
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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
    // reset isPermissionDialogShowing whenever a response is observed (granted or denied)
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

    // --- helper states for showing scroll buttons ---

    // Keep a coroutine scope for scroll animations (animateScrollToItem)
    val coroutineScope = rememberCoroutineScope()

    // Determine the current list (filtered or full)
    val currentListCount by remember(uiState) {
        derivedStateOf {
            val audios = uiState.filteredAudioFiles.ifEmpty { uiState.audioFiles }
            audios.size
        }
    }

    // Derived state for whether we are at the top
    val isAtTop by remember(lazyListState) {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
        }
    }

    // Derived state for whether we are at the bottom.
    // We consider "at bottom" when the last visible item index equals totalItemsCount - 1.
    val isAtBottom by remember(lazyListState) {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val total = layoutInfo.totalItemsCount
            if (total == 0) {
                true // treat empty list as both top & bottom
            } else {
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
                lastVisible?.index == total - 1
            }
        }
    }

    // We want the buttons to show when the user *starts scrolling* (user-initiated),
    // and hide shortly after scrolling stops to avoid flicker.
    var userIsScrolling by remember { mutableStateOf(false) }

    // Start a flow watching lazyListState.isScrollInProgress to set userIsScrolling.
    LaunchedEffect(lazyListState) {
        // snapshotFlow gives us changes of isScrollInProgress
        snapshotFlow { lazyListState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { inProgress ->
                if (inProgress) {
                    // user started interacting; show the FABs (if other conditions are met)
                    userIsScrolling = true
                } else {
                    // user stopped scrolling — keep visible for a short grace duration, then hide
                    // This prevents immediate flicker between quick drags/flings.
                    // Adjust delayMillis if you want a longer/shorter persistence.
                    val delayMillis = 1200L
                    // Launch small delay; if a new scroll begins, this coroutine is cancelled automatically.
                    try {
                        delay(delayMillis)
                        userIsScrolling = false
                    } catch (_: Exception) {
                        // coroutine cancelled because user scrolled again; ignore
                    }
                }
            }
    }

    // Compute visibility for each button:
    val showScrollToTop by remember(userIsScrolling, isAtTop) { derivedStateOf { userIsScrolling && !isAtTop } }
    val showScrollToBottom by remember(userIsScrolling, isAtBottom, currentListCount) {
        derivedStateOf {
            userIsScrolling && !isAtBottom && currentListCount > 0
        }
    }

    // Scroll to top when a sort/filter option is applied or changes ---
    // Do not scroll if the list is already at the top.
    LaunchedEffect(key1 = uiState.currentFilterOption) {
        if (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0) {
            // animate for smooth UX; replace with scrollToItem(0) if you prefer an instant jump
            lazyListState.scrollToItem(index = 0)
        }
    }

    // --- UI ---
    Box(
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
        // Keep the existing Column layout for the main content
        Column(modifier = Modifier.fillMaxSize()) {
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

        // --- Overlayed Floating Buttons ---
        // Position them bottom-end stacked vertically with spacing.
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Scroll to Top FAB
            AnimatedVisibility(
                visible = showScrollToTop,
                enter = slideInVertically(animationSpec = tween(180)) + fadeIn(animationSpec = tween(180)),
                exit = slideOutVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(180))
            ) {
                FloatingActionButton(
                    onClick = {
                        // Scroll to top safely
                        coroutineScope.launch {
                            // animate to first item (index 0)
                            lazyListState.scrollToItem(index = 0)
                        }
                    },
                    // keep default styling - you can customise in theme
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Scroll to top"
                    )
                }
            }

            // Scroll to Bottom FAB
            AnimatedVisibility(
                visible = showScrollToBottom,
                enter = slideInVertically(animationSpec = tween(180)) + fadeIn(animationSpec = tween(180)),
                exit = slideOutVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(180))
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            // Scroll to last item (index = count - 1). Clamp to 0 if list changes to empty.
                            val lastIndex = (currentListCount - 1).coerceAtLeast(0)
                            lazyListState.scrollToItem(index = lastIndex)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Scroll to bottom"
                    )
                }
            }
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
