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
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.engfred.musicplayer.feature_library.domain.models.FilterOption

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
@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit = {},
    onFilterSelected: (FilterOption) -> Unit,
    placeholder: String
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
                .clip(RoundedCornerShape(16.dp))
                .weight(1f)
                .heightIn(min = 56.dp),
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search icon"
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = MaterialTheme.shapes.small
        )

        // Filter Button with Dropdown
        IconButton(
            onClick = { showFilterMenu = true },
            modifier = Modifier
                .size(56.dp)
                .background(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                    MaterialTheme.shapes.extraLarge
                )
                .fillMaxHeight()
        ) {
            Icon(
                imageVector = Icons.Rounded.FilterList,
                contentDescription = "Filter songs",
                tint = MaterialTheme.colorScheme.onSurface,
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