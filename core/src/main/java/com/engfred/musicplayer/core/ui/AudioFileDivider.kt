package com.engfred.musicplayer.core.ui

//package com.engfred.musicplayer.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Divider used after each audio file item.
 * Matches the spacing/indentation of Library & Favorites.
 */
@Composable
fun AudioFileDivider() {
    val dividerStart = 10.dp + 56.dp + 12.dp // leading padding + icon size + spacing

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline)
            .padding(start = dividerStart)
    )
}
