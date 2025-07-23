package com.engfred.musicplayer.feature_playlist.presentation.components.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.ui.AudioFileItem

@Composable
fun PlaylistSongs(
    songs: List<AudioFile>,
    currentPlayingId: Long?,
    onSongClick: (AudioFile) -> Unit,
    onSongDelete: (AudioFile) -> Unit,
    snackbarHostState: SnackbarHostState,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = songs.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        LazyColumn(
            modifier = modifier
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(songs.size, key = { songs[it].id }) { index ->
                val audioFile = songs[index]
                AudioFileItem(
                    audioFile = audioFile,
                    isPlaying = (audioFile.id == currentPlayingId),
                    onClick = onSongClick,
                    onDelete = onSongDelete,
                    modifier = Modifier.animateItem(),
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}