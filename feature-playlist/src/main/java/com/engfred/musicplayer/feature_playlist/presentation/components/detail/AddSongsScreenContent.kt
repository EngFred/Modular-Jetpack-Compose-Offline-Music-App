package com.engfred.musicplayer.feature_playlist.presentation.components.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.core.domain.model.AudioFile

@Composable
fun AddSongsScreenContent(
    allAudioFiles: List<AudioFile>,
    currentPlaylistSongs: List<AudioFile>,
    onSongsSelected: (List<AudioFile>) -> Unit, // Callback when "Add" is pressed
    onNavigateBack: () -> Unit // Callback to go back to VIEW_PLAYLIST mode
) {
    // Keep track of selected songs by their String IDs
    val initialSelectedSongIdsSet = remember(currentPlaylistSongs) {
        currentPlaylistSongs.map { it.id.toString() }.toMutableSet()
    }
    val selectedSongIds = remember { mutableStateListOf<String>().apply { addAll(initialSelectedSongIdsSet) } }

    var searchQuery by remember { mutableStateOf("") }

    val filteredSongs = remember(allAudioFiles, searchQuery) {
        if (searchQuery.isBlank()) {
            allAudioFiles
        } else {
            allAudioFiles.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        (it.artist?.contains(searchQuery, ignoreCase = true) ?: false) ||
                        (it.album?.contains(searchQuery, ignoreCase = true) ?: false)
            }
        }
    }

    // Filter out songs that are already in the current playlist
    val availableSongsForSelection = remember(filteredSongs, currentPlaylistSongs) {
        val currentPlaylistSongStringIds = currentPlaylistSongs.map { it.id.toString() }.toSet()
        filteredSongs.filter { it.id.toString() !in currentPlaylistSongStringIds }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar for Add Songs Screen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(MaterialTheme.colorScheme.surface) // Solid background for top bar
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Playlist",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Add Songs",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Button(
                onClick = {
                    val songsToAdd = allAudioFiles.filter {
                        it.id.toString() in selectedSongIds &&
                                it.id.toString() !in initialSelectedSongIdsSet
                    }
                    onSongsSelected(songsToAdd)
                },
                enabled = (selectedSongIds.size - initialSelectedSongIdsSet.size) > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                val newSongsCount = selectedSongIds.size - initialSelectedSongIdsSet.size
                Text("Add ($newSongsCount)")
            }
        }

        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search songs...") },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = MaterialTheme.shapes.small
        )

        AnimatedVisibility(
            visible = availableSongsForSelection.isEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "No matching songs found." else "All songs are already in this playlist!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = availableSongsForSelection.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Take up remaining vertical space
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(availableSongsForSelection, key = { it.id }) { audioFile ->
                    val isSelected = selectedSongIds.contains(audioFile.id.toString())

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val idAsString = audioFile.id.toString()
                                if (isSelected) {
                                    selectedSongIds.remove(idAsString)
                                } else {
                                    selectedSongIds.add(idAsString)
                                }
                            }
                            .background(MaterialTheme.colorScheme.surface), // Item background
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Song Info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Placeholder for album art or a simple icon
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), MaterialTheme.shapes.small),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = audioFile.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = audioFile.artist ?: "Unknown Artist",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        // Checkbox
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { /* handled by Row clickable */ }
                        )
                    }
                }
            }
        }
    }
}