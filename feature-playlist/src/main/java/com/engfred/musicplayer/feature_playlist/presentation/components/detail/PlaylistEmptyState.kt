package com.engfred.musicplayer.feature_playlist.presentation.components.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.engfred.musicplayer.core.ui.InfoIndicator

@Composable
fun PlaylistEmptyState(modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = true, // This component is only shown when the playlist is empty
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        InfoIndicator(
            modifier = modifier
                .fillMaxWidth(),
            message = "This playlist is empty. Use the menu to add songs!",
            icon = Icons.Outlined.LibraryMusic
        )
    }
}