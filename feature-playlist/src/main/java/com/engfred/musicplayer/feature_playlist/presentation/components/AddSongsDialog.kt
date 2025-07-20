package com.engfred.musicplayer.feature_playlist.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.core.domain.model.AudioFile

@Composable
fun AddSongsDialog(
    allAudioFiles: List<AudioFile>,
    currentPlaylistSongs: List<AudioFile>,
    onAddSongs: (List<AudioFile>) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedSongs = remember { mutableStateListOf<AudioFile>() }
    val currentSongIds = currentPlaylistSongs.map { it.id }.toSet()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Songs to Playlist") },
        text = {
            Column {
                if (allAudioFiles.isEmpty()) {
                    Text(
                        text = "No songs available on device.",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(allAudioFiles, key = { it.id }) { audioFile ->
                            val isSelected = selectedSongs.contains(audioFile)
                            val isInPlaylist = currentSongIds.contains(audioFile.id)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isInPlaylist) {
                                        if (isSelected) {
                                            selectedSongs.remove(audioFile)
                                        } else {
                                            selectedSongs.add(audioFile)
                                        }
                                    }
                                    .padding(horizontal = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = MaterialTheme.shapes.medium,
                                elevation = CardDefaults.cardElevation(1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = audioFile.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isInPlaylist) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = audioFile.artist ?: "Unknown Artist",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isInPlaylist) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isInPlaylist) {
                                        Text(
                                            text = "Added",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAddSongs(selectedSongs.toList())
                    onDismiss()
                },
                enabled = selectedSongs.isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}