package com.engfred.musicplayer.feature_player.presentation.components.layouts.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.core.domain.model.PlayerLayout

@Composable
fun TrackInfo(
    title: String?,
    artist: String?,
    playerLayout: PlayerLayout,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = if (playerLayout == PlayerLayout.IMMERSIVE_CANVAS) Alignment.Start else Alignment.CenterHorizontally, // Conditional alignment
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 0.dp)
    ) {
        Text(
            text = title ?: "Unknown Title",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = LocalContentColor.current,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = artist ?: "Unknown Artist",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
            color = LocalContentColor.current.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}