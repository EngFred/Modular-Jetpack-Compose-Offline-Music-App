package com.engfred.musicplayer.feature_settings.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.engfred.musicplayer.core.ui.CustomTopBar
import com.engfred.musicplayer.core.ui.theme.AppThemeType
import com.engfred.musicplayer.feature_settings.presentation.viewmodel.SettingsEvent
import com.engfred.musicplayer.feature_settings.presentation.viewmodel.SettingsViewModel

/**
 * Composable screen for displaying application settings.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState = viewModel.uiState

    Scaffold(
        // Remove topBar parameter from Scaffold
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Do not apply top padding from Scaffold here, CustomTopBar will handle it
                .padding(bottom = paddingValues.calculateBottomPadding())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- Custom Top Bar ---
            CustomTopBar(
                title = "Settings",
                showNavigationIcon = true,
                onNavigateBack = onNavigateBack,
                // No actions needed for Settings screen, so actions lambda is empty by default
            )
            // --- End Custom Top Bar ---

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp) // Apply vertical padding here for content
                    .verticalScroll(rememberScrollState()) // Make the entire content scrollable
            ) {
                if (uiState.error != null) {
                    Text(
                        text = "Error: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Theme Selection Section
                Text(
                    text = "App Theme",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        AppThemeType.entries.forEach { themeType ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onEvent(SettingsEvent.UpdateTheme(themeType)) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.selectedTheme == themeType,
                                    onClick = { viewModel.onEvent(SettingsEvent.UpdateTheme(themeType)) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = themeType.name.replace("_", " ").lowercase()
                                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}