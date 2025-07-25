package com.engfred.musicplayer.feature_playlist.presentation.screens

import MiniPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.engfred.musicplayer.core.ui.ErrorIndicator
import com.engfred.musicplayer.core.ui.InfoIndicator
import com.engfred.musicplayer.core.ui.LoadingIndicator
import com.engfred.musicplayer.feature_playlist.domain.model.PlaylistSortOrder
import com.engfred.musicplayer.feature_playlist.presentation.components.detail.AddSongsBottomSheet
import com.engfred.musicplayer.feature_playlist.presentation.components.detail.PlaylistActionButtons
import com.engfred.musicplayer.feature_playlist.presentation.components.detail.PlaylistDetailHeaderSection
import com.engfred.musicplayer.feature_playlist.presentation.components.detail.PlaylistEmptyState
import com.engfred.musicplayer.core.ui.AudioFileItem
import com.engfred.musicplayer.feature_playlist.presentation.components.detail.PlaylistSongsHeader
import com.engfred.musicplayer.feature_playlist.presentation.components.detail.RenamePlaylistDialog
import com.engfred.musicplayer.feature_playlist.presentation.viewmodel.detail.PlaylistDetailEvent
import com.engfred.musicplayer.feature_playlist.presentation.viewmodel.detail.PlaylistDetailViewModel
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.draw.clip
import com.engfred.musicplayer.feature_playlist.presentation.components.detail.PlaylistSongs
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    windowWidthSizeClass: WindowWidthSizeClass
) {
    val uiState by viewModel.uiState.collectAsState()
    val mainLazyListState = rememberLazyListState()

    var moreMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var currentSortOrder by remember { mutableStateOf(PlaylistSortOrder.DATE_ADDED) }

    val sortedSongs = remember(uiState.playlist?.songs, currentSortOrder) {
        uiState.playlist?.songs?.let { songs ->
            when (currentSortOrder) {
                PlaylistSortOrder.DATE_ADDED -> songs
                PlaylistSortOrder.ALPHABETICAL -> songs.sortedBy { it.title.lowercase() }
            }
        } ?: emptyList()
    }

    var showAddSongsBottomSheet by remember { mutableStateOf(false) }
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

    val isCompactWidth = windowWidthSizeClass == WindowWidthSizeClass.Compact

    Scaffold(
        bottomBar = {
            if (uiState.currentPlayingAudioFile != null) {
                MiniPlayer(
                    onClick = onNavigateToNowPlaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f),
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
                    windowWidthSizeClass = windowWidthSizeClass
                )
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        val mainContentModifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            )

        if (isCompactWidth) {
            // --- Compact Layout (Phones - Portrait) ---
            LazyColumn(
                modifier = mainContentModifier.padding(horizontal = 16.dp),
                state = mainLazyListState,
                contentPadding = PaddingValues(top = 0.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    PlaylistDetailHeaderSection(
                        playlist = uiState.playlist,
                        onNavigateBack = onNavigateBack,
                        onAddSongsClick = { showAddSongsBottomSheet = true },
                        onRenamePlaylistClick = { viewModel.onEvent(PlaylistDetailEvent.ShowRenameDialog) },
                        moreMenuExpanded = moreMenuExpanded,
                        onMoreMenuExpandedChange = { moreMenuExpanded = it },
                        isCompact = true
                    )
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                item {
                    when {
                        uiState.isLoading -> LoadingIndicator(modifier = Modifier.fillMaxWidth().height(200.dp))
                        uiState.error != null -> ErrorIndicator(modifier = Modifier.fillMaxWidth().height(200.dp), message = uiState.error!!)
                        uiState.playlist == null -> InfoIndicator(modifier = Modifier.fillMaxWidth().height(200.dp), message = "Playlist not found or could not be loaded.", icon = Icons.Outlined.LibraryMusic)
                        else -> {
                            Column(Modifier.fillMaxWidth()) {
                                PlaylistActionButtons(
                                    onPlayClick = {
                                        uiState.playlist?.songs?.firstOrNull()?.let { firstSong ->
                                            viewModel.onEvent(PlaylistDetailEvent.PlaySong(firstSong))
                                        }
                                    },
                                    onShuffleClick = {
                                        if (uiState.playlist?.songs?.isNotEmpty() == true) {
                                            viewModel.onEvent(PlaylistDetailEvent.ShufflePlay)
                                        }
                                    },
                                    isCompact = true
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                PlaylistSongsHeader(
                                    songCount = uiState.playlist?.songs?.size ?: 0,
                                    currentSortOrder = currentSortOrder,
                                    onSortOrderChange = { currentSortOrder = it },
                                    sortMenuExpanded = sortMenuExpanded,
                                    onSortMenuExpandedChange = { sortMenuExpanded = it }
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }

                if (!uiState.isLoading && uiState.error == null && uiState.playlist != null && uiState.playlist?.songs.isNullOrEmpty()) {
                    item {
                        PlaylistEmptyState(modifier = Modifier.fillMaxWidth().height(200.dp))
                    }
                } else if (!uiState.isLoading && uiState.error == null && uiState.playlist != null && !uiState.playlist?.songs.isNullOrEmpty()) {
                    itemsIndexed(
                        items = sortedSongs,
                        key = { _, audioFile -> audioFile.id }
                    ) { _, audioFile ->
                        AudioFileItem(
                            audioFile = audioFile,
                            isCurrentPlayingAudio = (audioFile.id == uiState.currentPlayingAudioFile?.id),
                            onClick = { clickedAudioFile -> viewModel.onEvent(PlaylistDetailEvent.PlaySong(clickedAudioFile)) },
                            onDelete = { song -> viewModel.onEvent(PlaylistDetailEvent.RemoveSong(song.id)) },
                            modifier = Modifier.animateItem(),
                            onSwipeLeft = { audioFile ->
                                if (uiState.currentPlayingAudioFile?.id != audioFile.id) { viewModel.onEvent(PlaylistDetailEvent.SwipedLeft(audioFile)) } else { onNavigateToNowPlaying() }
                            },
                            onSwipeRight = { viewModel.onEvent(PlaylistDetailEvent.SwipedRight(it)) },
                            isAudioPlaying = uiState.isPlaying
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        } else {
            // --- Expanded Layout (Tablets / Phones - Landscape) ---
            Row(modifier = mainContentModifier.padding(horizontal = 48.dp)) {
                Column(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .background(Color.Transparent)
                        .padding(end = 24.dp)
                        .verticalScroll(rememberScrollState()), // FIXED: Changed to rememberScrollState()
                    horizontalAlignment = Alignment.CenterHorizontally // FIXED: Changed to horizontalAlignment
                ) {
                    PlaylistDetailHeaderSection(
                        playlist = uiState.playlist,
                        onNavigateBack = onNavigateBack,
                        onAddSongsClick = { showAddSongsBottomSheet = true },
                        onRenamePlaylistClick = { viewModel.onEvent(PlaylistDetailEvent.ShowRenameDialog) },
                        moreMenuExpanded = moreMenuExpanded,
                        onMoreMenuExpandedChange = { moreMenuExpanded = it },
                        isCompact = false
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    PlaylistActionButtons(
                        onPlayClick = {
                            uiState.playlist?.songs?.firstOrNull()?.let { firstSong ->
                                viewModel.onEvent(PlaylistDetailEvent.PlaySong(firstSong))
                            }
                        },
                        onShuffleClick = {
                            if (uiState.playlist?.songs?.isNotEmpty() == true) {
                                viewModel.onEvent(PlaylistDetailEvent.ShufflePlay)
                            }
                        },
                        isCompact = false
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                        .clip(MaterialTheme.shapes.medium)
                        .padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                ) {
                    when {
                        uiState.isLoading -> LoadingIndicator(modifier = Modifier.fillMaxSize())
                        uiState.error != null -> ErrorIndicator(modifier = Modifier.fillMaxSize(), message = uiState.error!!)
                        uiState.playlist == null -> InfoIndicator(modifier = Modifier.fillMaxSize(), message = "Playlist not found or could not be loaded.", icon = Icons.Outlined.LibraryMusic)
                        else -> {
                            PlaylistSongsHeader(
                                songCount = uiState.playlist?.songs?.size ?: 0,
                                currentSortOrder = currentSortOrder,
                                onSortOrderChange = { currentSortOrder = it },
                                sortMenuExpanded = sortMenuExpanded,
                                onSortMenuExpandedChange = { sortMenuExpanded = it }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (uiState.playlist?.songs.isNullOrEmpty()) {
                                PlaylistEmptyState(modifier = Modifier.fillMaxSize())
                            } else {
                                PlaylistSongs(
                                    songs = sortedSongs,
                                    currentPlayingId = uiState.currentPlayingAudioFile?.id,
                                    onSongClick = { clickedAudioFile -> viewModel.onEvent(PlaylistDetailEvent.PlaySong(clickedAudioFile)) },
                                    onSongDelete = { song -> viewModel.onEvent(PlaylistDetailEvent.RemoveSong(song.id)) },
                                    listState = rememberLazyListState(),
                                    isAudioPlaying = uiState.isPlaying,
                                    onSwipeRight = { viewModel.onEvent(PlaylistDetailEvent.SwipedRight(it)) },
                                    onSwipeLeft = { audioFile ->
                                        if (uiState.currentPlayingAudioFile?.id != audioFile.id) { viewModel.onEvent(PlaylistDetailEvent.SwipedLeft(audioFile)) } else { onNavigateToNowPlaying() }
                                    },
                                    modifier = Modifier.fillMaxSize()
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
}