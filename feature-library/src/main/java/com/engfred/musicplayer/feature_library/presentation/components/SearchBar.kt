package com.engfred.musicplayer.feature_library.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
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
import com.engfred.musicplayer.core.domain.model.FilterOption

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit = {},
    currentFilter: FilterOption,
    onFilterSelected: (FilterOption) -> Unit,
    placeholder: String
) {
    var showFilterMenu by remember { mutableStateOf(false) }

    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(24.dp))
            .heightIn(min = 48.dp),
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search icon"
            )
        },
        trailingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
                Box {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
                            contentDescription = "Filter songs",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false },
                        properties = PopupProperties(focusable = true),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        DropdownMenuItem(
                            text = { Text("Date Added (Newest First)") },
                            onClick = {
                                onFilterSelected(FilterOption.DATE_ADDED_DESC)
                                showFilterMenu = false
                            },
                            trailingIcon = {
                                if (currentFilter == FilterOption.DATE_ADDED_DESC) {
                                    Icon(Icons.Rounded.Check, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Date Added (Oldest First)") },
                            onClick = {
                                onFilterSelected(FilterOption.DATE_ADDED_ASC)
                                showFilterMenu = false
                            },
                            trailingIcon = {
                                if (currentFilter == FilterOption.DATE_ADDED_ASC) {
                                    Icon(Icons.Rounded.Check, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Length (Shortest First)") },
                            onClick = {
                                onFilterSelected(FilterOption.LENGTH_ASC)
                                showFilterMenu = false
                            },
                            trailingIcon = {
                                if (currentFilter == FilterOption.LENGTH_ASC) {
                                    Icon(Icons.Rounded.Check, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Length (Longest First)") },
                            onClick = {
                                onFilterSelected(FilterOption.LENGTH_DESC)
                                showFilterMenu = false
                            },
                            trailingIcon = {
                                if (currentFilter == FilterOption.LENGTH_DESC) {
                                    Icon(Icons.Rounded.Check, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Alphabetical (A-Z)") },
                            onClick = {
                                onFilterSelected(FilterOption.ALPHABETICAL_ASC)
                                showFilterMenu = false
                            },
                            trailingIcon = {
                                if (currentFilter == FilterOption.ALPHABETICAL_ASC) {
                                    Icon(Icons.Rounded.Check, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Alphabetical (Z-A)") },
                            onClick = {
                                onFilterSelected(FilterOption.ALPHABETICAL_DESC)
                                showFilterMenu = false
                            },
                            trailingIcon = {
                                if (currentFilter == FilterOption.ALPHABETICAL_DESC) {
                                    Icon(Icons.Rounded.Check, contentDescription = null)
                                }
                            }
                        )
                    }
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(24.dp)
    )
}
