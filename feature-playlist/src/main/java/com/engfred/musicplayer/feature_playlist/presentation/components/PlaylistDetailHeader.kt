package com.engfred.musicplayer.feature_playlist.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.engfred.musicplayer.feature_playlist.domain.model.Playlist
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage

@Composable
fun PlaylistDetailHeaderSection(
    playlist: Playlist?,
    onNavigateBack: () -> Unit,
    onAddSongsClick: () -> Unit,
    onRenamePlaylistClick: () -> Unit,
    moreMenuExpanded: Boolean,
    onMoreMenuExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Custom Top Row (Back & More Menu)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Box {
                IconButton(onClick = { onMoreMenuExpandedChange(true) }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                DropdownMenu(
                    expanded = moreMenuExpanded,
                    onDismissRequest = { onMoreMenuExpandedChange(false) }
                ) {
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
                }
            }
        }

        // Playlist Header (Image and Title)
        val firstSongAlbumArt = playlist?.songs?.firstOrNull()?.albumArtUri
        val playlistName = playlist?.name ?: "Unknown Playlist"
        val songCount = playlist?.songs?.size ?: 0

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CoilImage(
                imageModel = { firstSongAlbumArt },
                imageOptions = ImageOptions(
                    contentDescription = "Playlist Album Art",
                    contentScale = ContentScale.Crop
                ),
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                failure = {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = "No Album Art",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                },
                loading = {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = "No Album Art",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = playlistName,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}