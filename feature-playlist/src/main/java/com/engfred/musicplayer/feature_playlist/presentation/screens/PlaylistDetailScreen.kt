package com.engfred.musicplayer.feature_playlist.presentation.screens

import android.content.res.Configuration
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.engfred.musicplayer.core.ui.components.ErrorIndicator
import com.engfred.musicplayer.core.ui.components.InfoIndicator
import com.engfred.musicplayer.core.ui.components.LoadingIndicator
import com.engfred.musicplayer.feature_playlist.presentation.components.detail.AddSongsBottomSheet
import com.engfred.musicplayer.feature_playlist.presentation.components.detail.PlaylistActionButtons
import com.engfred.musicplayer.feature_playlist.presentation.components.detail.PlaylistDetailHeaderSection
import com.engfred.musicplayer.feature_playlist.presentation.components.detail.PlaylistEmptyState
import com.engfred.musicplayer.core.ui.components.AudioFileItem
import com.engfred.musicplayer.feature_playlist.presentation.components.detail.PlaylistSongsHeader
import com.engfred.musicplayer.feature_playlist.presentation.components.detail.RenamePlaylistDialog
import com.engfred.musicplayer.feature_playlist.presentation.viewmodel.detail.PlaylistDetailEvent
import com.engfred.musicplayer.feature_playlist.presentation.viewmodel.detail.PlaylistDetailViewModel
import androidx.compose.ui.draw.clip
import com.engfred.musicplayer.feature_playlist.presentation.components.detail.PlaylistSongs
import androidx.compose.ui.platform.LocalDensity
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.ui.components.AddSongToPlaylistDialog
import com.engfred.musicplayer.core.ui.components.ConfirmationDialog
import com.engfred.musicplayer.feature_playlist.presentation.components.detail.PlaylistDetailTopBar
import com.engfred.musicplayer.core.domain.model.AutomaticPlaylistType
import com.engfred.musicplayer.core.ui.components.MiniPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onEditInfo: (AudioFile) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val mainLazyListState = rememberLazyListState() // used for portrait
    val leftListState = rememberLazyListState()     // used for left pane in landscape/tablet
    val rightListState = rememberLazyListState()    // used for songs list in landscape/tablet

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val density = LocalDensity.current

    // responsive breakpoints
    val isTablet = screenWidthDp >= 900

    // left pane fraction: tablet vs landscape phone vs portrait (portrait uses single-column)
    val leftPaneFraction = when {
        isTablet -> 0.35f
        isLandscape -> 0.40f
        else -> 1f
    }
    val rightPaneFraction = 1f - leftPaneFraction

    var moreMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var sortMenuExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    var showAddSongsBottomSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showAddSongsBottomSheet) {
        AddSongsBottomSheet(
            onDismissRequest = { showAddSongsBottomSheet = false },
            sheetState = sheetState,
            allAudioFiles = uiState.allAudioFiles,
            currentPlaylistSongs = uiState.playlist?.songs ?: emptyList(),
            onSongsSelected = {
                it.forEach { song ->
                    viewModel.onEvent(PlaylistDetailEvent.AddSong(song))
                }
            }
        )
    }

    // threshold based on screen height (12% of height) converted to pixels
    val thresholdPx = with(density) { (screenHeightDp * 0.12f).dp.toPx().toInt() }

    // scrolledPastHeader now uses the relevant list state depending on layout
    val scrolledPastHeader by remember {
        derivedStateOf {
            val state = if (!isLandscape) mainLazyListState else rightListState
            state.firstVisibleItemIndex > 0 ||
                    (state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset > thresholdPx)
        }
    }

    Scaffold(
        bottomBar = {
            if (uiState.currentPlayingAudioFile != null) {
                MiniPlayer(
                    onClick = onNavigateToNowPlaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    onPlayPause = {
                        viewModel.onEvent(PlaylistDetailEvent.PlayPause)
                    },
                    onPlayNext = {
                        viewModel.onEvent(PlaylistDetailEvent.PlayNext)
                    },
                    onPlayPrev = {
                        viewModel.onEvent(PlaylistDetailEvent.PlayPrev)
                    },
                    isPlaying = uiState.isPlaying,
                    playingAudioFile = uiState.currentPlayingAudioFile,
                )
            }
        },
    ) { paddingValues ->
        val mainContentModifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                )
            )
            .padding(paddingValues)

        if (!isLandscape) {
            // Portrait / compact phone layout: single column (existing behavior)
            Box(modifier = Modifier.fillMaxSize()) {
                PlaylistDetailTopBar(
                    playlistName = uiState.playlist?.name,
                    playlistArtUri = uiState.playlist?.songs?.firstOrNull()?.albumArtUri,
                    scrolledPastHeader = scrolledPastHeader,
                    onNavigateBack = onNavigateBack,
                    onMoreMenuExpandedChange = { moreMenuExpanded = it },
                    isAutomaticPlaylist = uiState.playlist?.isAutomatic ?: false,
                    onAddSongsClick = { showAddSongsBottomSheet = true },
                    onRenamePlaylistClick = { viewModel.onEvent(PlaylistDetailEvent.ShowRenameDialog) },
                    moreMenuExpanded = moreMenuExpanded,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .zIndex(2f),
                    isEditable = !uiState.playlist?.name.equals("Favorites", ignoreCase = true)
                )

                LazyColumn(
                    modifier = mainContentModifier
                        .padding(horizontal = 8.dp),
                    state = mainLazyListState,
                    contentPadding = PaddingValues(top = 0.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    item {
                        val topBarPadding = 38.dp
                        PlaylistDetailHeaderSection(
                            playlist = uiState.playlist,
                            isCompact = true,
                            modifier = Modifier.padding(top = topBarPadding)
                        )
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    item {
                        when {
                            uiState.isLoading -> LoadingIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                            uiState.error != null -> ErrorIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                message = uiState.error!!
                            )
                            uiState.playlist == null -> InfoIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                message = "Playlist not found or could not be loaded.",
                                icon = Icons.Outlined.LibraryMusic
                            )
                            else -> {
                                Column(Modifier.fillMaxWidth()) {
                                    PlaylistActionButtons(
                                        onPlayAllClick = {
                                            uiState.playlist?.songs?.let { songs ->
                                                if (songs.isNotEmpty()) {
                                                    viewModel.onEvent(PlaylistDetailEvent.PlayAll)
                                                } else {
                                                    Toast.makeText(context, "Playlist is empty, cannot play.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        onShuffleAllClick = {
                                            if (uiState.playlist?.songs?.isNotEmpty() == true) {
                                                viewModel.onEvent(PlaylistDetailEvent.ShuffleAll)
                                            } else {
                                                Toast.makeText(context, "Playlist is empty, cannot shuffle play.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        isCompact = true
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    PlaylistSongsHeader(
                                        songCount = uiState.playlist?.songs?.size ?: 0,
                                        currentSortOrder = uiState.currentSortOrder,
                                        onSortOrderChange = {
                                            viewModel.onEvent(PlaylistDetailEvent.SetSortOrder(it))
                                        },
                                        sortMenuExpanded = sortMenuExpanded,
                                        onSortMenuExpandedChange = { sortMenuExpanded = it },
                                        isTopSongs = uiState.playlist?.type == AutomaticPlaylistType.MOST_PLAYED
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }

                    if (!uiState.isLoading && uiState.error == null && uiState.playlist != null && uiState.playlist?.songs.isNullOrEmpty()) {
                        item {
                            PlaylistEmptyState(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                playlistType = uiState.playlist?.type
                            )
                        }
                    } else if (!uiState.isLoading && uiState.error == null && uiState.playlist != null && !uiState.playlist?.songs.isNullOrEmpty()) {
                        itemsIndexed(
                            items = uiState.sortedSongs,
                            key = { _, audioFile -> audioFile.id }
                        ) { _, audioFile ->
                            AudioFileItem(
                                audioFile = audioFile,
                                isCurrentPlayingAudio = (audioFile.id == uiState.currentPlayingAudioFile?.id),
                                onClick = { clickedAudioFile -> viewModel.onEvent(PlaylistDetailEvent.PlayAudio(clickedAudioFile)) },
                                onRemoveOrDelete = { song -> viewModel.onEvent(PlaylistDetailEvent.ShowRemoveSongConfirmation(song)) },
                                modifier = Modifier.animateItem(),
                                isAudioPlaying = uiState.isPlaying,
                                onAddToPlaylist = {
                                    viewModel.onEvent(PlaylistDetailEvent.ShowPlaylistsDialog(it))
                                },
                                onPlayNext = {
                                    viewModel.onEvent(PlaylistDetailEvent.SetPlayNext(it))
                                },
                                isFromAutomaticPlaylist = uiState.playlist?.isAutomatic ?: false,
                                playCount = uiState.playlist?.playCounts?.get(audioFile.id),
                                onEditInfo = onEditInfo
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        } else {
            // Landscape / wide layout: split into left details and right songs.
            Row(modifier = mainContentModifier.padding(horizontal = if (isTablet) 40.dp else 28.dp)) {
                // Left pane: header + actions. Use LazyColumn so it can scroll independently.
                LazyColumn(
                    state = leftListState,
                    modifier = Modifier
                        .weight(leftPaneFraction)
                        .fillMaxHeight()
                        .padding(end = if (isTablet) 28.dp else 24.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        PlaylistDetailHeaderSection(
                            playlist = uiState.playlist,
                            isCompact = false,
                        )
                    }

                    item {
                        PlaylistActionButtons(
                            onPlayAllClick = {
                                uiState.playlist?.songs?.firstOrNull()?.let { firstSong ->
                                    viewModel.onEvent(PlaylistDetailEvent.PlayAll)
                                }
                            },
                            onShuffleAllClick = {
                                if (uiState.playlist?.songs?.isNotEmpty() == true) {
                                    viewModel.onEvent(PlaylistDetailEvent.ShuffleAll)
                                }
                            },
                            isCompact = false
                        )
                    }

                    // Extra spacing at bottom so content doesn't butt against the songs pane
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // Right pane: songs list and controls
                Column(
                    modifier = Modifier
                        .weight(rightPaneFraction)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.18f))
                        .clip(MaterialTheme.shapes.medium)
                        .padding(start = if (isTablet) 28.dp else 24.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                ) {
                    when {
                        uiState.isLoading || uiState.isCleaningMissingSongs -> LoadingIndicator(modifier = Modifier.fillMaxSize())
                        uiState.error != null -> ErrorIndicator(modifier = Modifier.fillMaxSize(), message = uiState.error!!)
                        uiState.playlist == null -> InfoIndicator(modifier = Modifier.fillMaxSize(), message = "Playlist not found or could not be loaded.", icon = Icons.Outlined.LibraryMusic)
                        else -> {
                            PlaylistSongsHeader(
                                songCount = uiState.playlist?.songs?.size ?: 0,
                                currentSortOrder = uiState.currentSortOrder,
                                onSortOrderChange = {
                                    viewModel.onEvent(PlaylistDetailEvent.SetSortOrder(it))
                                },
                                sortMenuExpanded = sortMenuExpanded,
                                onSortMenuExpandedChange = { sortMenuExpanded = it },
                                isTopSongs = uiState.playlist?.type == AutomaticPlaylistType.MOST_PLAYED
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (uiState.playlist?.songs.isNullOrEmpty()) {
                                PlaylistEmptyState(modifier = Modifier.fillMaxSize(), playlistType = uiState.playlist?.type)
                            } else {
                                PlaylistSongs(
                                    songs = uiState.sortedSongs,
                                    currentPlayingId = uiState.currentPlayingAudioFile?.id,
                                    onSongClick = { clickedAudioFile -> viewModel.onEvent(PlaylistDetailEvent.PlayAudio(clickedAudioFile)) },
                                    onSongRemove = { song -> viewModel.onEvent(PlaylistDetailEvent.ShowRemoveSongConfirmation(song)) },
                                    listState = rightListState,
                                    isAudioPlaying = uiState.isPlaying,
                                    modifier = Modifier.fillMaxSize(),
                                    onAddToPlaylist = {
                                        viewModel.onEvent(PlaylistDetailEvent.ShowPlaylistsDialog(it))
                                    },
                                    onPlayNext = {
                                        viewModel.onEvent(PlaylistDetailEvent.SetPlayNext(it))
                                    },
                                    isFromAutomaticPlaylist = uiState.playlist?.isAutomatic ?: false,
                                    playCountMap = uiState.playlist?.playCounts,
                                    onEditInfo = onEditInfo
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.showRenameDialog && uiState.playlist != null) {
            RenamePlaylistDialog(
                currentName = uiState.playlist?.name!!,
                onConfirm = { newName ->
                    viewModel.onEvent(PlaylistDetailEvent.RenamePlaylist(newName))
                },
                onDismiss = {
                    viewModel.onEvent(PlaylistDetailEvent.HideRenameDialog)
                },
                errorMessage = uiState.error
            )
        }
    }

    if (uiState.showAddToPlaylistDialog) {
        Log.d("PlaylistDetailScreen", "Showing add to playlist dialog")
        AddSongToPlaylistDialog(
            onDismiss = { viewModel.onEvent(PlaylistDetailEvent.DismissAddToPlaylistDialog) },
            playlists = uiState.playlists,
            onAddSongToPlaylist = { playlist ->
                viewModel.onEvent(PlaylistDetailEvent.AddedSongToPlaylist(playlist))
            }
        )
    } else {
        Log.d("PlaylistDetailScreen", "Add to playlist dialog dismissed")
    }

    if (uiState.showRemoveSongConfirmationDialog) {
        uiState.audioFileToRemove?.let { audioFile ->
            ConfirmationDialog(
                title = "Remove '${audioFile.title}'?",
                message = "Are you sure you want to remove this song from '${uiState.playlist?.name}'?",
                confirmButtonText = "Remove",
                dismissButtonText = "Cancel",
                onConfirm = {
                    viewModel.onEvent(PlaylistDetailEvent.ConfirmRemoveSong)
                },
                onDismiss = {
                    viewModel.onEvent(PlaylistDetailEvent.DismissRemoveSongConfirmation)
                }
            )
        } ?: run {
            viewModel.onEvent(PlaylistDetailEvent.DismissRemoveSongConfirmation)
        }
    }
}
