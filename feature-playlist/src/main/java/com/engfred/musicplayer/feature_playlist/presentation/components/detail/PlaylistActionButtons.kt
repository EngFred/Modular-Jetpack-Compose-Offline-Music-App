package com.engfred.musicplayer.feature_playlist.presentation.components.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.widthIn
@Composable
fun PlaylistActionButtons(
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    isCompact: Boolean, // New parameter
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isCompact) 24.dp else 0.dp), // Adjust horizontal padding
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val buttonMinSize = if (isCompact) 120.dp else 160.dp // Minimum button width
        Button(
            onClick = onPlayClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .weight(1f)
                .height(if (isCompact) 56.dp else 64.dp) // Taller buttons
                .widthIn(min = buttonMinSize) // Ensure a minimum width
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play Playlist", tint = MaterialTheme.colorScheme.onPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Play", color = MaterialTheme.colorScheme.onPrimary, style = if (isCompact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium) // Dynamic text size
        }
        Spacer(modifier = Modifier.width(if (isCompact) 16.dp else 24.dp)) // More space between buttons
        Button(
            onClick = onShuffleClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier
                .weight(1f)
                .height(if (isCompact) 56.dp else 64.dp)
                .widthIn(min = buttonMinSize)
        ) {
            Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle Playlist", tint = MaterialTheme.colorScheme.onSecondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Shuffle", color = MaterialTheme.colorScheme.onSecondary, style = if (isCompact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium) // Dynamic text size
        }
    }
}