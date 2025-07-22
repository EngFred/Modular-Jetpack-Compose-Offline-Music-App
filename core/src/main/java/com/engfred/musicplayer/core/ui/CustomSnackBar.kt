package com.engfred.musicplayer.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A custom-designed Snackbar composable for consistent feedback across the app.
 * Reduced size version.
 *
 * @param snackbarData The SnackbarData object provided by SnackbarHostState.
 */
@Composable
fun CustomSnackbar(snackbarData: SnackbarData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp) // Reduced outer padding
            .clip(RoundedCornerShape(10.dp)) // Slightly smaller rounded corners
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp), // Reduced inner padding
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = snackbarData.visuals.message,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal), // Changed to bodyMedium and normal weight
                maxLines = 2, // Allow up to 2 lines
                modifier = Modifier.weight(1f)
            )
            snackbarData.visuals.actionLabel?.let { actionLabel ->
                Text(
                    text = actionLabel,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), // Changed to labelMedium
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable { snackbarData.performAction() }
                )
            }
        }
    }
}