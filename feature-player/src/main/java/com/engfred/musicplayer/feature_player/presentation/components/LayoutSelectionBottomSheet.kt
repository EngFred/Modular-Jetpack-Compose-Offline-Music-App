package com.engfred.musicplayer.feature_player.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.feature_player.domain.model.PlayerLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutSelectionBottomSheet(
    currentLayout: PlayerLayout,
    onLayoutSelected: (PlayerLayout) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp) // Padding from bottom edge
        ) {
            // REMOVED: Text(
            // REMOVED:    text = "Choose Player Layout",
            // REMOVED:    style = MaterialTheme.typography.titleLarge,
            // REMOVED:    color = MaterialTheme.colorScheme.onSurface,
            // REMOVED:    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp)
            // REMOVED: )
            // If you want to keep some top padding, you can add a Spacer here instead,
            // or adjust the overall column padding.
            Spacer(modifier = Modifier.height(16.dp)) // This spacer was already present, effectively acting as top padding.

            // Layout options
            LayoutOption(
                icon = Icons.Default.Info,
                title = "Ethereal Flow",
                description = "Album art-driven dynamic gradients.",
                isSelected = currentLayout == PlayerLayout.ETHEREAL_FLOW,
                onClick = { onLayoutSelected(PlayerLayout.ETHEREAL_FLOW) }
            )
            LayoutOption(
                icon = Icons.Default.Photo,
                title = "Immersive Canvas",
                description = "Full-screen blurred album art background.",
                isSelected = currentLayout == PlayerLayout.IMMERSIVE_CANVAS,
                onClick = { onLayoutSelected(PlayerLayout.IMMERSIVE_CANVAS) }
            )
            LayoutOption(
                icon = Icons.Default.List,
                title = "Minimalist Groove",
                description = "Clean, compact, and control-focused.",
                isSelected = currentLayout == PlayerLayout.MINIMALIST_GROOVE,
                onClick = { onLayoutSelected(PlayerLayout.MINIMALIST_GROOVE) }
            )
        }
    }
}

@Composable
fun LayoutOption(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check, // Changed from Icons.Default.Info for a more appropriate checkmark
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}