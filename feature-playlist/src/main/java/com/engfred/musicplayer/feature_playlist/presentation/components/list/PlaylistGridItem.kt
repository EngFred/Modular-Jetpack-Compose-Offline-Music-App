package com.engfred.musicplayer.feature_playlist.presentation.components.list

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.core.domain.model.Playlist
import com.engfred.musicplayer.feature_playlist.presentation.components.DeleteConfirmationDialog
import com.skydoves.landscapist.coil.CoilImage

/**
 * Composable for displaying a single playlist item in a grid layout.
 *
 * @param playlist The playlist data to display.
 * @param onClick Callback when the playlist item is clicked.
 * @param onDeleteClick Callback when the delete action is confirmed.
 * @param isDeletable Boolean indicating if the playlist can be deleted (false for automatic playlists).
 * @param modifier Modifier for the composable.
 */
@Composable
fun PlaylistGridItem(
    playlist: Playlist,
    onClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    isDeletable: Boolean,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick(playlist.id) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val firstSongAlbumArtUri = playlist.songs.firstOrNull()?.albumArtUri
                if (firstSongAlbumArtUri != null) {
                    CoilImage(
                        imageModel = { firstSongAlbumArtUri },
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
                        },
                        failure = {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = "No album art available",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxSize().padding(16.dp)
                            )
                        }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = "No album art available",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    )
                }

                Box(modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Playlist options",
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (isDeletable) { // Only show delete option if deletable
                            DropdownMenuItem(
                                text = { Text("Delete Playlist") },
                                onClick = {
                                    showDeleteConfirmationDialog = true
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Cannot Delete") },
                                onClick = {
                                    Toast.makeText(context, "Automatic playlists cannot be deleted.", Toast.LENGTH_SHORT).show()
                                    showMenu = false
                                },
                                enabled = false,
                                leadingIcon = {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete (disabled)")
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${playlist.songs.size} songs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }

    if (showDeleteConfirmationDialog) {
        DeleteConfirmationDialog(
            itemName = playlist.name,
            onConfirm = {
                onDeleteClick(playlist.id)
                showDeleteConfirmationDialog = false
            },
            onDismiss = { showDeleteConfirmationDialog = false }
        )
    }
}