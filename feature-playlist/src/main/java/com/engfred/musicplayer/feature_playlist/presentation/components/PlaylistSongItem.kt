//package com.engfred.musicplayer.feature_playlist.presentation.components
//
//import android.net.Uri
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.core.RepeatMode
//import androidx.compose.animation.core.animateFloat
//import androidx.compose.animation.core.infiniteRepeatable
//import androidx.compose.animation.core.rememberInfiniteTransition
//import androidx.compose.animation.core.tween
//import androidx.compose.animation.fadeIn
//import androidx.compose.animation.fadeOut
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Delete
//import androidx.compose.material.icons.filled.MusicNote
//import androidx.compose.material.icons.filled.PlayArrow
//import androidx.compose.material3.Card
//import androidx.compose.material3.CardDefaults
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.draw.scale
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import com.skydoves.landscapist.coil.CoilImage // Changed import
//import com.engfred.musicplayer.core.domain.model.AudioFile
//
//@Composable
//fun PlaylistSongItem(
//    audioFile: AudioFile,
//    onClick: (AudioFile) -> Unit,
//    onRemoveClick: (Long) -> Unit,
//    modifier: Modifier = Modifier,
//    isCurrentPlaying: Boolean = false
//) {
//    val infiniteTransition = rememberInfiniteTransition(label = "playing_animation")
//    val scale by infiniteTransition.animateFloat(
//        initialValue = 1f,
//        targetValue = 1.1f,
//        animationSpec = infiniteRepeatable(
//            animation = tween(durationMillis = 800),
//            repeatMode = RepeatMode.Reverse
//        ), label = "playing_scale_animation"
//    )
//
//    Card(
//        modifier = modifier
//            .fillMaxWidth()
//            .clickable {
//                onClick(audioFile)
//            }
//            .padding(horizontal = 8.dp, vertical = 4.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = if (isCurrentPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
//            else MaterialTheme.colorScheme.surfaceVariant
//        ),
//        shape = RoundedCornerShape(12.dp),
//        elevation = CardDefaults.cardElevation(2.dp)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(12.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            // Album Art or Placeholder Icon
//            // Using CoilImage from Landscapist
//            CoilImage(
//                imageModel = { audioFile.albumArtUri }, // Pass the URI directly
//                modifier = Modifier
//                    .size(56.dp)
//                    .clip(RoundedCornerShape(8.dp)),
//                loading = {
//                    // You can add a shimmer effect or a simple Box here
//                    Box(Modifier.size(56.dp).background(MaterialTheme.colorScheme.surfaceContainer))
//                },
//                failure = {
//                    Icon(
//                        imageVector = Icons.Default.MusicNote, // Using MusicNote as a generic placeholder
//                        contentDescription = "No Album Art",
//                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
//                        modifier = Modifier.size(56.dp)
//                    )
//                }
//            )
//
//            Spacer(modifier = Modifier.width(12.dp))
//
//            Column(
//                modifier = Modifier.weight(1f)
//            ) {
//                Text(
//                    text = audioFile.title,
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Bold,
//                    color = if (isCurrentPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//                Spacer(modifier = Modifier.height(2.dp))
//                Text(
//                    text = audioFile.artist ?: "Unknown Artist",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = if (isCurrentPlaying) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//            }
//
//            Spacer(modifier = Modifier.width(12.dp))
//
//            // Now Playing Indicator
//            AnimatedVisibility(
//                visible = isCurrentPlaying,
//                enter = fadeIn(),
//                exit = fadeOut()
//            ) {
//                Icon(
//                    imageVector = Icons.Default.PlayArrow,
//                    contentDescription = "Currently Playing",
//                    tint = MaterialTheme.colorScheme.primary,
//                    modifier = Modifier
//                        .size(32.dp)
//                        .scale(scale)
//                )
//            }
//
//            // Remove from Playlist Button
//            IconButton(onClick = { onRemoveClick(audioFile.id) }) {
//                Icon(
//                    imageVector = Icons.Default.Delete,
//                    contentDescription = "Remove from Playlist",
//                    tint = MaterialTheme.colorScheme.error
//                )
//            }
//        }
//    }
//}
