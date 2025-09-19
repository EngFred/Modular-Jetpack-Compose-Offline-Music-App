package com.engfred.musicplayer.core.domain.model

/**
 * How the home-screen widget background should behave.
 * - STATIC: use the app's premium/static background drawable (existing behavior).
 * - THEME_AWARE: adapt the widget to the device light/dark mode (colors/tints).
 */
enum class WidgetBackgroundMode {
    STATIC,
    THEME_AWARE
}
