package com.engfred.musicplayer.feature_equalizer.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EqualizerErrorDisplay(errorMessage: String?) {
    if (errorMessage != null) {
        Text(
            text = "Error: $errorMessage",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
    }
}