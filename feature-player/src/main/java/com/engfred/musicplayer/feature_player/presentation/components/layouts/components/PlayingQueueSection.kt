package com.engfred.musicplayer.feature_player.presentation.components.layouts.components

import com.engfred.musicplayer.core.domain.model.AudioFile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun PlayingQueueSection(
    queue: List<AudioFile>,
    currentPlayingIndex: Int,
    onPlayItem: (AudioFile) -> Unit,
    onRemoveItem: (AudioFile) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp)) // Add some rounded corners to the queue pane
            .background(LocalContentColor.current.copy(alpha = 0.05f)) // Subtle background
            .padding(16.dp),
        horizontalAlignment = Alignment.Start // Align text to start
    ) {
        Text(
            text = "Playing Queue",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = LocalContentColor.current,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        if (queue.isEmpty()) {
            Text(
                text = "Queue is empty.",
                style = MaterialTheme.typography.bodyLarge,
                color = LocalContentColor.current.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 24.dp, start = 8.dp)
            )
        } else {
            LazyColumn {
                itemsIndexed(queue) { index, audioFile ->
                    QueueItem(
                        audioFile = audioFile,
                        isCurrent = index == currentPlayingIndex,
                        onPlayClick = { onPlayItem(audioFile) },
                        onRemoveClick = { onRemoveItem(audioFile) }
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
    isCurrent: Boolean,
    onPlayClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    val backgroundColor = if (isCurrent) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) // Highlight current song
    } else {
        Color.Transparent
    }
    val contentColor = if (isCurrent) {
        MaterialTheme.colorScheme.primary
    } else {
        LocalContentColor.current
    }
    val artistColor = if (isCurrent) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    } else {
        LocalContentColor.current.copy(alpha = 0.7f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onPlayClick) // Play song on click
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AsyncImage(
            model = audioFile.albumArtUri,
            contentDescription = "Album Art",
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = audioFile.title ?: "Unknown Title",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
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
                Icons.Default.Close, // Using Close for remove from queue
                contentDescription = "Remove from queue",
                tint = LocalContentColor.current.copy(alpha = 0.6f)
            )
        }
    }
}