package com.engfred.musicplayer.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow

/**
 * A custom composable for the top application bar, now more flexible.
 */
@Composable
fun CustomTopBar(
    title: String,
    modifier: Modifier = Modifier,
    showNavigationIcon: Boolean = false,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {} // Slot API for actions
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp) // Standard TopAppBar height
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.CenterStart // Align content to start
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp), // Slightly less padding to accommodate icon buttons
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (showNavigationIcon) {
                IconButton(
                    onClick = onNavigateBack ?: {},
                    enabled = onNavigateBack != null // Disable button if no callback
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                // Spacer or empty Box to maintain alignment if no nav icon
                Spacer(modifier = Modifier.width(0.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (showNavigationIcon) 0.dp else 12.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Actions slot
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}