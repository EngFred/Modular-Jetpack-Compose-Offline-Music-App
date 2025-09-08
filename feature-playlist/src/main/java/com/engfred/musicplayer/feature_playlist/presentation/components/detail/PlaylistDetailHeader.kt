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
import com.engfred.musicplayer.core.domain.model.Playlist
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage

/**
 * Composable for the header section of the Playlist Detail screen.
 * Displays playlist art, title, and options like adding songs or renaming.
 *
 * NOTE: The top bar functionality (back arrow and more options) has been moved to a separate
 * `PlaylistDetailTopBar` composable which is now placed outside the scrollable list.
 * The `onNavigateBack` and `onMoreMenuExpandedChange` callbacks are no longer used here.
 */
@Composable
fun PlaylistDetailHeaderSection(
    playlist: Playlist?,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Old Top Row with back arrow and more menu is now gone.
        // It has been moved to a separate sticky top bar composable.

        // Playlist Header (Image and Title)
        val firstSongAlbumArt = playlist?.songs?.firstOrNull()?.albumArtUri
        val playlistName = playlist?.name ?: "Unknown Playlist"

        val imageSize = if (isCompact) 300.dp else 240.dp
        val titleStyle = if (isCompact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = if (isCompact) 16.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.size(20.dp))
            CoilImage(
                imageModel = { firstSongAlbumArt },
                imageOptions = ImageOptions(
                    contentDescription = "Playlist Album Art",
                    contentScale = ContentScale.FillBounds
                ),
                modifier = Modifier
                    .size(imageSize)
                    .clip(RoundedCornerShape(if (isCompact) 16.dp else 20.dp))
                    .shadow(
                        elevation = if (isCompact) 12.dp else 20.dp,
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
                            .size(imageSize)
                            .clip(RoundedCornerShape(if (isCompact) 16.dp else 20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                },
                loading = {
                    Box(
                        modifier = Modifier
                            .size(imageSize)
                            .clip(RoundedCornerShape(if (isCompact) 16.dp else 20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = "Loading Album Art",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(imageSize * 0.6f)
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(if (isCompact) 13.dp else 32.dp))
            Text(
                text = playlistName,
                style = titleStyle.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}