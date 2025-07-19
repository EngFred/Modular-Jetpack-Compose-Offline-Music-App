package com.engfred.musicplayer.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Define your custom theme types
enum class AppThemeType {
    LIGHT,
    DARK,
    DEEP_BLUE,
    VIBRANT_GREEN,
    WARM_AMBER
}

// Light color scheme
private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = LightError,
    onError = LightOnError,
    outline = LightOutline,
)

// Dark color scheme
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

// Deep Blue color scheme
private val DeepBlueColorScheme = darkColorScheme( // Often custom themes are dark-based
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

// Vibrant Green color scheme
private val VibrantGreenColorScheme = darkColorScheme(
    primary = VibrantGreenPrimary,
    onPrimary = VibrantGreenOnPrimary,
    primaryContainer = VibrantGreenPrimaryContainer,
    onPrimaryContainer = VibrantGreenOnPrimaryContainer,
    secondary = VibrantGreenSecondary,
    onSecondary = VibrantGreenOnSecondary,
    secondaryContainer = VibrantGreenSecondaryContainer,
    onSecondaryContainer = VibrantGreenOnSecondaryContainer,
    background = VibrantGreenBackground,
    onBackground = VibrantGreenOnBackground,
    surface = VibrantGreenSurface,
    onSurface = VibrantGreenOnSurface,
    surfaceVariant = VibrantGreenSurfaceVariant,
    onSurfaceVariant = VibrantGreenOnSurfaceVariant,
    error = VibrantGreenError,
    onError = VibrantGreenOnError,
    outline = VibrantGreenOutline,
)

// Warm Amber color scheme
private val WarmAmberColorScheme = darkColorScheme(
    primary = WarmAmberPrimary,
    onPrimary = WarmAmberOnPrimary,
    primaryContainer = WarmAmberPrimaryContainer,
    onPrimaryContainer = WarmAmberOnPrimaryContainer,
    secondary = WarmAmberSecondary,
    onSecondary = WarmAmberOnSecondary,
    secondaryContainer = WarmAmberSecondaryContainer,
    onSecondaryContainer = WarmAmberOnSecondaryContainer,
    background = WarmAmberBackground,
    onBackground = WarmAmberOnBackground,
    surface = WarmAmberSurface,
    onSurface = WarmAmberOnSurface,
    surfaceVariant = WarmAmberSurfaceVariant,
    onSurfaceVariant = WarmAmberOnSurfaceVariant,
    error = WarmAmberError,
    onError = WarmAmberOnError,
    outline = WarmAmberOutline,
)

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun MusicPlayerAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    selectedTheme: AppThemeType,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when (selectedTheme) {
        AppThemeType.LIGHT -> if (dynamicColor) dynamicLightColorScheme(LocalContext.current) else LightColorScheme
        AppThemeType.DARK -> if (dynamicColor) dynamicDarkColorScheme(LocalContext.current) else DarkColorScheme
        AppThemeType.DEEP_BLUE -> DeepBlueColorScheme
        AppThemeType.VIBRANT_GREEN -> VibrantGreenColorScheme
        AppThemeType.WARM_AMBER -> WarmAmberColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}