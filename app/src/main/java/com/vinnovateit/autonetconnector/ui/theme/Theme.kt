package com.vinnovateit.autonetconnector.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    // The darkTheme parameter is removed to enforce a single theme.
    content: @Composable () -> Unit
) {
    val colorScheme = AppColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
