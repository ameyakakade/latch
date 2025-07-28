package com.vinnovateit.autonetconnector.ui.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AppColorScheme = lightColorScheme(
    primary = ContainerPrimary,
    onPrimary = TextOnInteractive,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = SurfaceContainer,
    onSurface = TextPrimary,
    onSurfaceVariant = TextPrimary.copy(alpha = 0.7f),
    outline = TextPrimary.copy(alpha = 0.5f),
    error = TextPrimary,
    surfaceContainer = SurfaceContainer,
    primaryContainer = ContainerPrimary,
    onPrimaryContainer = TextOnInteractive
)

/**
 * Custom color extension for Tooltip container color.
 */
val ColorScheme.tooltipContainer: Color
    @Composable
    get() = TooltipContainer

/**
 * Custom color extension for Tooltip content color.
 */
val ColorScheme.tooltipContent: Color
    @Composable
    get() = TooltipContent


@Composable
fun AutoNetConnectorTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = AppColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar color to match the app's background.
            window.statusBarColor = colorScheme.background.toArgb()
            // Set status bar icons to be dark, as the theme's background is light.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
