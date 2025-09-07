package com.engfred.musicplayer.feature_playlist.presentation.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PlaylistActionButtons(
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isCompact) 15.dp else 0.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tileHeight = if (isCompact) 56.dp else 64.dp
        val tileMinWidth = if (isCompact) 120.dp else 160.dp

        // Play Tile
        Box(
            modifier = Modifier
                .weight(1f)
                .height(tileHeight)
                .widthIn(min = tileMinWidth)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onPlayClick() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = "Play Playlist",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    "Play",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // Shuffle Tile
        Box(
            modifier = Modifier
                .weight(1f)
                .height(tileHeight)
                .widthIn(min = tileMinWidth)
                .background(MaterialTheme.colorScheme.secondary)
                .clickable { onShuffleClick() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle Playlist",
                    tint = MaterialTheme.colorScheme.onSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Shuffle",
                    color = MaterialTheme.colorScheme.onSecondary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}