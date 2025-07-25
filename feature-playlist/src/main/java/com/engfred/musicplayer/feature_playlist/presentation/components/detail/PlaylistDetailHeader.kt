package com.engfred.musicplayer.feature_playlist.presentation.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    isCompact: Boolean, // New parameter
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Custom Top Row (Back & More Menu)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = if (isCompact) 8.dp else 16.dp), // Adjust top/bottom padding
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

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

        val imageSize = if (isCompact) 180.dp else 240.dp // Larger image for larger screens
        val titleStyle = if (isCompact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall
        val subtitleStyle = if (isCompact) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = if (isCompact) 16.dp else 24.dp), // Add bottom padding to separate image/title from buttons
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CoilImage(
                imageModel = { firstSongAlbumArt },
                imageOptions = ImageOptions(
                    contentDescription = "Playlist Album Art",
                    contentScale = ContentScale.Crop
                ),
                modifier = Modifier
                    .size(imageSize) // Dynamic image size
                    .clip(RoundedCornerShape(if (isCompact) 16.dp else 20.dp)) // Slightly larger corner radius
                    .shadow(
                        elevation = if (isCompact) 12.dp else 20.dp, // Deeper shadow on larger screens
                        shape = RoundedCornerShape(if (isCompact) 16.dp else 20.dp),
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                failure = {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = "No Album Art",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(imageSize) // Dynamic image size for fallback
                            .clip(RoundedCornerShape(if (isCompact) 16.dp else 20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                },
                loading = {
                    Box(
                        modifier = Modifier
                            .size(imageSize) // Dynamic image size for loading
                            .clip(RoundedCornerShape(if (isCompact) 16.dp else 20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        // Better loading indicator for the image itself
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = "Loading Album Art",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(imageSize * 0.6f) // Icon scales with image size
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(if (isCompact) 24.dp else 32.dp)) // More space
            Text(
                text = playlistName,
                style = titleStyle.copy(fontWeight = FontWeight.Bold), // Dynamic text style
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}