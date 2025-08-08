package com.vinnovateit.autonetconnector.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinnovateit.autonetconnector.features.settings.manager.SettingsManager

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFC01221),
    onPrimary = Color(0xFFFFDFB1),
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFFC01221), // Using primary red for secondary elements
    onSecondary = Color(0xFFFF8686),
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF410002),
    tertiary = Color(0xFFB59300), // A rich, accessible yellow/gold
    onTertiary = Color(0xFFFFD078),
    tertiaryContainer = Color(0xFFFFE252),
    onTertiaryContainer = Color(0xFF241A00),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFDF0D5),
    onBackground = Color(0xFF201A19),
    surface = Color(0xFFFFF4E0), // A slightly lighter beige for cards
    onSurface = Color(0xFF201A19), // Dark text for readability on beige
    surfaceVariant = Color(0xFFFDE9E2), // A muted red/beige for variants
    onSurfaceVariant = Color(0xFF534341),
    outline = Color(0xFFC01221), // Red outlines for emphasis
    inverseOnSurface = Color(0xFF000000),
    inverseSurface = Color(0xFF362F2E),
    inversePrimary = Color(0xFFFFB4AB),
    surfaceTint = Color(0xFFC01221),
    outlineVariant = Color(0xFFD7C1BE),
    scrim = Color(0xFF000000),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFFFB4AB), // Using primary red for secondary elements
    onSecondary = Color(0xFF690005),
    secondaryContainer = Color(0xFF93000A),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFFE2C14D),
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF5A4400),
    onTertiaryContainer = Color(0xFFFBDD88),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1C1B1F), // A standard dark background
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF231F20), // A slightly lighter dark for cards/sheets
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF534341),
    onSurfaceVariant = Color(0xFFD7C1BE),
    outline = Color(0xFFA08C8A),
    inverseOnSurface = Color(0xFF1C1B1F),
    inverseSurface = Color(0xFFE6E1E5),
    inversePrimary = Color(0xFFC01221),
    surfaceTint = Color(0xFFFFB4AB),
    outlineVariant = Color(0xFF534341),
    scrim = Color(0xFF000000),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AutoNetConnectorTheme(
    content: @Composable () -> Unit
) {
    val themeSetting by SettingsManager.theme.collectAsStateWithLifecycle()
    val systemIsDark = isSystemInDarkTheme()

    val darkTheme = when (themeSetting) {
        "Light" -> false
        "Dark" -> true
        else -> systemIsDark
    }

    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = AppTypography,
        content = content
    )
}

// Tooltip color extensions
val ColorScheme.tooltipContainer: Color
    @Composable get() = ColorTooltipContainer

val ColorScheme.tooltipContent: Color
    @Composable get() = ColorTooltipContent