package com.engfred.musicplayer.feature_library.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.foundation.text.selection.TextSelectionColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    enabled: Boolean = true
) {
    SearchBar(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = { /* Triggered on Enter key; not used here */ },
        active = false,
        onActiveChange = { /* Not used for non-expandable SearchBar */ },
        placeholder = { Text(placeholder) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        enabled = enabled,
        colors = SearchBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            inputFieldColors = SearchBarDefaults.inputFieldColors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedTrailingIconColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                selectionColors = TextSelectionColors(
                    handleColor = MaterialTheme.colorScheme.primary,
                    backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
            )
        )
    ) {
        // Optional: Add search suggestions here if active = true
    }
}