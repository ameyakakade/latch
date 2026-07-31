package com.vinnovateit.latch.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import kotlin.math.min

/**
 * The phone UI is authored against these logical dimensions. Content is always
 * laid out at exactly this size so no layout code has to change; only the
 * physical pixel size of the window varies with screen fit.
 */
private const val DESIGN_W = 412f
private const val DESIGN_H = 900f

/** How the fixed design surface is mapped onto the host screen. */
internal data class ScreenFit(
    /** The OS scale factor, e.g. 1.5 at 150% Windows scaling. */
    val system: Float,
    /** The density we actually render at, shrunk if the window would not fit. */
    val target: Float,
)

/**
 * Compose Desktop defaults [LocalDensity] to the OS scale factor. At 150%
 * Windows scaling a 412x900dp window becomes 618x1350 *physical* pixels, which
 * is taller than a 1080p panel -- the bottom of every screen would be
 * unreachable. 1080p at 125-150% is the Windows default on 14" laptops, so this
 * is the common case, not an edge case.
 *
 * Fix: keep content at exactly 412x900 logical dp and shrink the physical size
 * by lowering the density instead. Geometry is preserved exactly; only apparent
 * size changes.
 */
internal fun computeScreenFit(): ScreenFit {
    val system: Float
    val usableHeight: Int
    try {
        val gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .defaultScreenDevice.defaultConfiguration
        val insets = Toolkit.getDefaultToolkit().getScreenInsets(gc)
        usableHeight = gc.bounds.height - insets.top - insets.bottom
        system = gc.defaultTransform.scaleY.toFloat().coerceAtLeast(1f)
    } catch (e: Throwable) {
        // Headless or an exotic display setup: fall back to 1:1.
        return ScreenFit(system = 1f, target = 1f)
    }
    // Leave ~8% headroom so the title bar and taskbar never clip the content.
    val target = min(system, (usableHeight * 0.92f) / DESIGN_H).coerceAtLeast(1.0f)
    return ScreenFit(system = system, target = target)
}

/**
 * The main window.
 *
 * Note [visible] is passed as a *parameter* rather than wrapping this call in
 * `if (visible)`. Conditionally emitting a Window destroys and recreates it,
 * which wipes the whole composition -- nav backstack, scroll positions,
 * expanded cards, half-typed text. For a tray app that is shown and hidden
 * dozens of times a day that is very visible. Compose skips rendering for an
 * invisible window, so keeping it alive is cheap, and live stats collection
 * keeps running while hidden, which is what we want.
 */
@Composable
internal fun LatchWindow(
    visible: Boolean,
    onCloseRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val fit = remember { computeScreenFit() }
    // WindowState.size is interpreted at the SYSTEM density while LocalDensity
    // governs content, so the two must be reconciled by their ratio.
    val scale = fit.target / fit.system
    val state = rememberWindowState(
        position = WindowPosition(Alignment.Center),
        size = DpSize((DESIGN_W * scale).dp, (DESIGN_H * scale).dp),
    )

    Window(
        visible = visible,
        onCloseRequest = onCloseRequest,
        state = state,
        resizable = false,
        title = "Latch",
    ) {
        CompositionLocalProvider(
            LocalDensity provides Density(density = fit.target, fontScale = 1f)
        ) {
            content()
        }
    }
}
