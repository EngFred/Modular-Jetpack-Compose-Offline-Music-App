package com.engfred.musicplayer.feature_playlist.presentation.components.detail

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/**
 * NEW: Composable for the sticky top bar in compact mode.
 * This bar displays the back arrow, and conditionally shows the playlist art and name
 * when the user has scrolled past the main header section.
 *
 * @param playlistName The name of the playlist.
 * @param playlistArtUri The URI of the playlist's album art.
 * @param scrolledPastHeader Boolean indicating if the user has scrolled past the header.
 * @param onNavigateBack Callback to navigate back.
 * @param moreMenuExpanded State for the expanded/collapsed more options menu.
 * @param onMoreMenuExpandedChange Callback to change the expanded state of the more options menu.
 * @param isAutomaticPlaylist Boolean indicating if the current playlist is an automatic one.
 * @param onAddSongsClick Callback to trigger adding songs.
 * @param onRenamePlaylistClick Callback to trigger renaming the playlist.
 * @param modifier Modifier for the composable.
 */
@Composable
fun PlaylistDetailTopBar(
    playlistName: String?,
    playlistArtUri: Uri?,
    scrolledPastHeader: Boolean,
    onNavigateBack: () -> Unit,
    moreMenuExpanded: Boolean,
    onMoreMenuExpandedChange: (Boolean) -> Unit,
    isAutomaticPlaylist: Boolean,
    onAddSongsClick: () -> Unit,
    onRenamePlaylistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = if (scrolledPastHeader) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background
            )
            .statusBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            // Animate visibility of the album art and title
            AnimatedVisibility(
                visible = scrolledPastHeader,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CoilImage(
                        imageModel = { playlistArtUri },
                        imageOptions = ImageOptions(
                            contentDescription = "Playlist Art",
                            contentScale = ContentScale.Crop
                        ),
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape), // Make the image circular
                        failure = {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = "No Album Art",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = playlistName ?: "Unknown Playlist",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (isAutomaticPlaylist.not()) {
            Box {
                IconButton(onClick = { onMoreMenuExpandedChange(true) }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                DropdownMenu(
                    expanded = moreMenuExpanded,
                    onDismissRequest = { onMoreMenuExpandedChange(false) }
                ) {
                    if (!isAutomaticPlaylist) {
                        DropdownMenuItem(
                            text = { Text("Add songs to playlist") },
                            onClick = {
                                onAddSongsClick()
                                onMoreMenuExpandedChange(false)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename playlist") },
                            onClick = {
                                onRenamePlaylistClick()
                                onMoreMenuExpandedChange(false)
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Cannot modify automatic playlist") },
                            onClick = { onMoreMenuExpandedChange(false) },
                            enabled = false
                        )
                    }
                }
            }
        }
    }
}