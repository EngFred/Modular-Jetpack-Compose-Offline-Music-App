package com.engfred.musicplayer.feature_player.presentation.components

import com.engfred.musicplayer.core.domain.model.AudioFile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage

@Composable
fun PlayingQueueSection(
    playingQueue: List<AudioFile>,
    playingAudio: AudioFile?,
    onPlayItem: (AudioFile) -> Unit,
    onRemoveItem: (AudioFile) -> Unit,
    isCompact: Boolean
) {

    val currentSongIndex = remember(playingAudio, playingQueue) {
        if (playingAudio == null || playingQueue.isEmpty()) {
            -1
        } else {
            playingQueue.indexOfFirst { it.id == playingAudio.id }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(LocalContentColor.current.copy(alpha = 0.05f)),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Playing Queue",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = LocalContentColor.current,
            modifier = Modifier.padding(bottom = 10.dp, top = 10.dp , start = 10.dp, end= 10.dp)
        )
        if (playingQueue.isEmpty()) {
            Text(
                text = "Queue is empty.",
                style = MaterialTheme.typography.bodyLarge,
                color = LocalContentColor.current.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 24.dp, start = 8.dp)
            )
        } else {
            LazyColumn {
                itemsIndexed(playingQueue) { index, audioFile ->
                    QueueItem(
                        audioFile = audioFile,
                        isCurrentlyPlaying = index == currentSongIndex,
                        onPlayClick = { onPlayItem(audioFile) },
                        onRemoveClick = { onRemoveItem(audioFile) },
                        showAlbumArt = isCompact,
                        isLastItem = index == playingQueue.lastIndex
                    )
                }
            }
        }
    }
}

// Reusable Queue Item Composable (used by both bottom sheet and embedded queue)
@Composable
fun QueueItem(
    audioFile: AudioFile,
    isCurrentlyPlaying: Boolean,
    onPlayClick: () -> Unit,
    onRemoveClick: () -> Unit,
    showAlbumArt: Boolean = true,
    isLastItem: Boolean = false
) {
    val backgroundColor = if (isCurrentlyPlaying) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }
    val contentColor = if (isCurrentlyPlaying) {
        MaterialTheme.colorScheme.primary
    } else {
        LocalContentColor.current
    }
    val artistColor = if (isCurrentlyPlaying) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    } else {
        LocalContentColor.current.copy(alpha = 0.7f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onPlayClick)
            .padding(start = 15.dp),
    ) {

        if (showAlbumArt) Spacer(Modifier.size(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (showAlbumArt)  {

                CoilImage(
                    imageModel = { audioFile.albumArtUri },
                    imageOptions = ImageOptions(
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Crop
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    failure = {
                        Icon(
                            imageVector = Icons.Rounded.Album,
                            contentDescription = "No Album Art",
                        )
                    },
                    loading = {
                        Icon(
                            imageVector = Icons.Rounded.Album,
                            contentDescription = "No Album Art",
                        )
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = audioFile.title ?: "Unknown Title",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCurrentlyPlaying) FontWeight.Bold else FontWeight.Normal,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = audioFile.artist ?: "Unknown Artist",
                    style = MaterialTheme.typography.bodyMedium,
                    color = artistColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Option to remove item from queue
            IconButton(onClick = onRemoveClick) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Remove from queue",
                    tint = LocalContentColor.current.copy(alpha = 0.6f)
                )
            }
        }
        if (showAlbumArt) Spacer(Modifier.size(6.dp))
        if (!isLastItem) {
            HorizontalDivider()
        }
    }
}