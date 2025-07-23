package com.engfred.musicplayer.feature_playlist.presentation.components.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.feature_playlist.presentation.screens.PlaylistSortOrder // Import the enum

@Composable
fun PlaylistSongsHeader(
    songCount: Int,
    currentSortOrder: PlaylistSortOrder,
    onSortOrderChange: (PlaylistSortOrder) -> Unit,
    sortMenuExpanded: Boolean,
    onSortMenuExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Songs ($songCount)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Box {
            IconButton(onClick = { onSortMenuExpandedChange(true) }) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "Sort songs",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            DropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = { onSortMenuExpandedChange(false) }
            ) {
                DropdownMenuItem(
                    text = { Text("Date Added") },
                    onClick = {
                        onSortOrderChange(PlaylistSortOrder.DATE_ADDED)
                        onSortMenuExpandedChange(false)
                    },
                    leadingIcon = {
                        if (currentSortOrder == PlaylistSortOrder.DATE_ADDED) {
                            Icon(Icons.Default.Check, contentDescription = "Selected")
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Alphabetical (A-Z)") },
                    onClick = {
                        onSortOrderChange(PlaylistSortOrder.ALPHABETICAL)
                        onSortMenuExpandedChange(false)
                    },
                    leadingIcon = {
                        if (currentSortOrder == PlaylistSortOrder.ALPHABETICAL) {
                            Icon(Icons.Default.Check, contentDescription = "Selected")
                        }
                    }
                )
            }
        }
    }
}