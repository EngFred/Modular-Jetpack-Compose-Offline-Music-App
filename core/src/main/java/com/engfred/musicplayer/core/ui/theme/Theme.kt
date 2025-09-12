package com.engfred.musicplayer.core.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Updated Theme Types
enum class AppThemeType {
    BLUE,
    SUNSET_GROOVE,
    LIGHT,
    NEON_DARK,
    DARK,
}

// --- Existing Themes ---

private val LightColorScheme = lightColorScheme(
    primary = FrostPrimary,
    onPrimary = FrostOnPrimary,
    primaryContainer = FrostPrimaryContainer,
    onPrimaryContainer = FrostOnPrimaryContainer,
    secondary = FrostSecondary,
    onSecondary = FrostOnSecondary,
    secondaryContainer = FrostSecondaryContainer,
    onSecondaryContainer = FrostOnSecondaryContainer,
    background = FrostBackground,
    onBackground = FrostOnBackground,
    surface = FrostSurface,
    onSurface = FrostOnSurface,
    surfaceVariant = FrostSurfaceVariant,
    onSurfaceVariant = FrostOnSurfaceVariant,
    error = FrostError,
    onError = FrostOnError,
    outline = FrostOutline,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = DarkError,
    onError = DarkOnError,
    outline = DarkOutline,
)

private val DeepBlueColorScheme = darkColorScheme(
    primary = DeepBluePrimary,
    onPrimary = DeepBlueOnPrimary,
    primaryContainer = DeepBluePrimaryContainer,
    onPrimaryContainer = DeepBlueOnPrimaryContainer,
    secondary = DeepBlueSecondary,
    onSecondary = DeepBlueOnSecondary,
    secondaryContainer = DeepBlueSecondaryContainer,
    onSecondaryContainer = DeepBlueOnSecondaryContainer,
    background = DeepBlueBackground,
    onBackground = DeepBlueOnBackground,
    surface = DeepBlueSurface,
    onSurface = DeepBlueOnSurface,
    surfaceVariant = DeepBlueSurfaceVariant,
    onSurfaceVariant = DeepBlueOnSurfaceVariant,
    error = DeepBlueError,
    onError = DeepBlueOnError,
    outline = DeepBlueOutline,
)

private val NeonPulseColorScheme = darkColorScheme(
    primary = NeonPrimary,
    onPrimary = NeonOnPrimary,
    primaryContainer = NeonPrimaryContainer,
    onPrimaryContainer = NeonOnPrimaryContainer,
    secondary = NeonSecondary,
    onSecondary = NeonOnSecondary,
    secondaryContainer = NeonSecondaryContainer,
    onSecondaryContainer = NeonOnSecondaryContainer,
    background = NeonBackground,
    onBackground = NeonOnBackground,
    surface = NeonSurface,
    onSurface = NeonOnSurface,
    surfaceVariant = NeonSurfaceVariant,
    onSurfaceVariant = NeonOnSurfaceVariant,
    error = NeonError,
    onError = NeonOnError,
    outline = NeonOutline,
)

private val SunsetGrooveColorScheme = darkColorScheme(
    primary = SunsetPrimary,
    onPrimary = SunsetOnPrimary,
    primaryContainer = SunsetPrimaryContainer,
    onPrimaryContainer = SunsetOnPrimaryContainer,
    secondary = SunsetSecondary,
    onSecondary = SunsetOnSecondary,
    secondaryContainer = SunsetSecondaryContainer,
    onSecondaryContainer = SunsetOnSecondaryContainer,
    background = SunsetBackground,
    onBackground = SunsetOnBackground,
    surface = SunsetSurface,
    onSurface = SunsetOnSurface,
    surfaceVariant = SunsetSurfaceVariant,
    onSurfaceVariant = SunsetOnSurfaceVariant,
    error = SunsetError,
    onError = SunsetOnError,
    outline = SunsetOutline,
)

// --- Apply Theme ---

@Composable
fun MusicPlayerAppTheme(
    selectedTheme: AppThemeType,
    content: @Composable () -> Unit
) {
    val colorScheme = when (selectedTheme) {
        AppThemeType.LIGHT -> LightColorScheme
        AppThemeType.DARK -> DarkColorScheme
        AppThemeType.BLUE -> DeepBlueColorScheme
        AppThemeType.NEON_DARK -> NeonPulseColorScheme
        AppThemeType.SUNSET_GROOVE -> SunsetGrooveColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()

            // Determine if the current theme is a light theme (Frostbyte)
            val isLightTheme = selectedTheme == AppThemeType.LIGHT

            // Set status bar icons to dark for light themes, and light for dark themes
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = isLightTheme
                isAppearanceLightNavigationBars = isLightTheme
            }
        }
    }


    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
