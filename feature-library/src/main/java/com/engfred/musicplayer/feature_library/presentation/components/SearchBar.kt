package com.engfred.musicplayer.feature_library.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties

/**
 * Represents the different filtering options for audio files.
 */
enum class FilterOption {
    DATE_ADDED_ASC, DATE_ADDED_DESC,
    LENGTH_ASC, LENGTH_DESC,
    ALPHABETICAL_ASC, ALPHABETICAL_DESC
}

/**
 * A standard and professional search bar component with an integrated filter option.
 *
 * @param query The current search query string.
 * @param onQueryChange Callback invoked when the search query changes.
 * @param onSearch Callback invoked when the search action is triggered (e.g., by pressing Enter).
 * @param onFilterSelected Callback invoked when a filter option is selected from the dropdown.
 * @param placeholder The placeholder text to display when the query is empty.
 * @param modifier The modifier to be applied to the search bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit = {},
    onFilterSelected: (FilterOption) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    var showFilterMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp),
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search icon"
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), // This is the color we'll mimic
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            shape = MaterialTheme.shapes.extraLarge
        )

        // Filter Button with Dropdown
        IconButton(
            onClick = { showFilterMenu = true }, // Toggle dropdown visibility
            modifier = Modifier
                .size(56.dp)
                .background(
                    // Mimicking the placeholder text color with a suitable alpha for background
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), // Adjusted alpha for a subtle background
                    MaterialTheme.shapes.extraLarge
                )
                .fillMaxHeight()
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filter songs",
                tint = MaterialTheme.colorScheme.onSurface, // Use onSurface for better contrast on this new background
                modifier = Modifier.size(24.dp)
            )

            DropdownMenu(
                expanded = showFilterMenu,
                onDismissRequest = { showFilterMenu = false },
                properties = PopupProperties(focusable = true)
            ) {
                DropdownMenuItem(
                    text = { Text("Date Added (Newest First)") },
                    onClick = {
                        onFilterSelected(FilterOption.DATE_ADDED_DESC)
                        showFilterMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Date Added (Oldest First)") },
                    onClick = {
                        onFilterSelected(FilterOption.DATE_ADDED_ASC)
                        showFilterMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Length (Shortest First)") },
                    onClick = {
                        onFilterSelected(FilterOption.LENGTH_ASC)
                        showFilterMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Length (Longest First)") },
                    onClick = {
                        onFilterSelected(FilterOption.LENGTH_DESC)
                        showFilterMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Alphabetical (A-Z)") },
                    onClick = {
                        onFilterSelected(FilterOption.ALPHABETICAL_ASC)
                        showFilterMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Alphabetical (Z-A)") },
                    onClick = {
                        onFilterSelected(FilterOption.ALPHABETICAL_DESC)
                        showFilterMenu = false
                    }
                )
            }
        }
    }
}