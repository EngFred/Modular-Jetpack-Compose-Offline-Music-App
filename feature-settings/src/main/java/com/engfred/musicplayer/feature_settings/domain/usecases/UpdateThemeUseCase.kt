package com.engfred.musicplayer.feature_settings.domain.usecases

import com.engfred.musicplayer.core.ui.theme.AppThemeType
import com.engfred.musicplayer.feature_settings.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case to update the application's theme setting.
 */
class UpdateThemeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(theme: AppThemeType) {
        settingsRepository.updateTheme(theme)
    }
}