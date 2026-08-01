package com.vinnovateit.latch.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.Toolkit

/** What the window opens at when there is room for it. */
private const val PREFERRED_W = 1060f
private const val PREFERRED_H = 700f

/**
 * Below this the responsive layouts stop having anywhere to put things: the
 * compact home arrangement needs roughly this much height for the panel and the
 * power button not to overlap.
 */
private const val MIN_W = 560
private const val MIN_H = 540

/**
 * The window's initial size: the preferred size, clamped to 90% of the usable
 * screen so it never opens larger than the display it lands on.
 *
 * The previous implementation pinned the window to a fixed 412x900 phone surface
 * and lowered [androidx.compose.ui.platform.LocalDensity] to make that fit on a
 * 1080p panel. That density override existed only to serve the fixed phone
 * layout; now that the UI is responsive it would just make everything small, so
 * it is gone and the OS scale factor is honoured as-is.
 */
private fun preferredWindowSize(): DpSize {
    return try {
        val gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .defaultScreenDevice.defaultConfiguration
        val insets = Toolkit.getDefaultToolkit().getScreenInsets(gc)
        val scale = gc.defaultTransform.scaleY.toFloat().coerceAtLeast(1f)

        // Screen bounds are physical pixels; the window size is in dp, so divide
        // the usable area back out by the OS scale before comparing.
        val usableW = (gc.bounds.width - insets.left - insets.right) / scale
        val usableH = (gc.bounds.height - insets.top - insets.bottom) / scale

        DpSize(
            width = minOf(PREFERRED_W, usableW * 0.9f).coerceAtLeast(MIN_W.toFloat()).dp,
            height = minOf(PREFERRED_H, usableH * 0.9f).coerceAtLeast(MIN_H.toFloat()).dp,
        )
    } catch (e: Throwable) {
        // Headless or an exotic display setup.
        DpSize(PREFERRED_W.dp, PREFERRED_H.dp)
    }
}

/**
 * The main window.
 *
 * Note [visible] is passed as a *parameter* rather than wrapping this call in
 * `if (visible)`. Conditionally emitting a Window destroys and recreates it,
 * which wipes the whole composition -- nav destination, scroll positions,
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
    val initialSize = remember { preferredWindowSize() }
    val state = rememberWindowState(
        position = WindowPosition(Alignment.Center),
        size = initialSize,
    )

    Window(
        visible = visible,
        onCloseRequest = onCloseRequest,
        state = state,
        resizable = true,
        title = "Latch",
    ) {
        // Compose has no minimum-size property, so it is set on the AWT window
        // directly. Without it the user can drag the window down to a few pixels
        // and the layouts collapse.
        LaunchedEffect(window) {
            window.minimumSize = Dimension(MIN_W, MIN_H)
        }
        content()
    }
}
