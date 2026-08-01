package com.vinnovateit.latch.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.vinnovateit.latch.ui.components.LatchIcons
import com.vinnovateit.latch.ui.theme.LatchTheme
import java.awt.GraphicsEnvironment
import java.awt.Toolkit

/** Windows' own close-button hover colour -- kept OS-consistent even in a custom bar. */
private val CloseHoverRed = Color(0xFFE81123)

/** Height of the custom title bar replacing the OS one. */
private val TitleBarHeight = 32.dp

/** Corner radius of the window itself -- requires [Window]'s transparent flag. */
private val WindowCornerRadius = 14.dp

/** What the window opens at when there is room for it. */
private const val PREFERRED_W = 400f
private const val PREFERRED_H = 720f

/**
 * The most of the usable screen the window may occupy in either axis.
 *
 * It has to leave enough of a margin to still read as a panel sitting on the
 * desktop rather than a maximised app, which is the whole point of a fixed-size
 * tray window.
 */
private const val MAX_SCREEN_FRACTION = 0.85f

/**
 * The floor the preferred size is clamped to on small screens. Below this the
 * responsive layouts stop having anywhere to put things: the compact home
 * arrangement needs roughly this much height for the panel and the power button
 * not to overlap.
 */
private const val MIN_W = 380
private const val MIN_H = 600

/**
 * The window's size -- its only one, since the window is not resizable: the
 * preferred size, shrunk to fit when the display cannot give it that much room.
 *
 * Both axes shrink by the *same* factor, so a small screen gets a smaller
 * window rather than a differently-shaped one. Clamping each axis on its own
 * (which is what this used to do) stretched the window towards whatever shape
 * the screen was: with a preferred height taller than any laptop panel, the
 * height was always decided by the screen clamp and never by the preference, so
 * the window opened at ~90% of screen height on every machine and looked
 * enormous on small ones -- while the width, whose preference *did* fit, stayed
 * put and looked wide by comparison.
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
        // the usable area back out by the OS scale before comparing. This is why
        // display *scaling*, not just resolution, decides how big this feels:
        // a 1080p panel at 150% has only 693dp of usable height to give.
        val usableW = (gc.bounds.width - insets.left - insets.right) / scale
        val usableH = (gc.bounds.height - insets.top - insets.bottom) / scale

        val fit = minOf(
            1f,
            usableW * MAX_SCREEN_FRACTION / PREFERRED_W,
            usableH * MAX_SCREEN_FRACTION / PREFERRED_H,
        )

        DpSize(
            // The minimum floors can exceed the screen on a genuinely tiny
            // display; coerceAtMost keeps the window on it regardless, since an
            // undecorated window hanging off the bottom cannot be dragged back.
            width = (PREFERRED_W * fit).coerceAtLeast(MIN_W.toFloat()).coerceAtMost(usableW).dp,
            height = (PREFERRED_H * fit).coerceAtLeast(MIN_H.toFloat()).coerceAtMost(usableH).dp,
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
        position = WindowPosition(Alignment.BottomEnd),
        size = initialSize,
    )

    LaunchedEffect(visible) {
        if (visible) {
            state.size = initialSize
            state.position = WindowPosition(Alignment.BottomEnd)
        }
    }

    Window(
        visible = visible,
        onCloseRequest = onCloseRequest,
        state = state,
        // Locked to the size it opens at. This maps to Frame.setResizable(false),
        // which is what actually removes the maximize box -- Windows then also
        // refuses Aero Snap, Win+Up and the maximise-on-title-bar-double-click,
        // so no separate placement guard is needed.
        resizable = false,
        // The OS chrome is replaced entirely by [LatchTitleBar] below -- title,
        // taskbar and Alt-Tab icon still use the brand mark, but the window itself
        // draws its own bar and a themed 1px edge instead of the native frame.
        // `transparent` is what lets the corners outside the rounded Surface below
        // actually show the desktop through rather than square OS window pixels.
        undecorated = true,
        transparent = true,
        title = "Latch",
        icon = remember { LatchIcon.brand() },
    ) {
        LatchTheme {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(WindowCornerRadius)),
                shape = RoundedCornerShape(WindowCornerRadius),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    WindowDraggableArea(modifier = Modifier.fillMaxWidth()) {
                        LatchTitleBar(
                            onMinimize = { state.isMinimized = true },
                            onClose = onCloseRequest,
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        content()
                    }
                }
            }
        }
    }
}

/**
 * The custom title bar replacing the OS one: an empty draggable strip (via the
 * enclosing [WindowDraggableArea]) plus minimize/close on the right. No brand
 * mark or wordmark here -- [content] below opens with its own Latch mark +
 * "LATCH" wordmark header, so repeating it in the bar just showed the logo
 * twice within the first 70px of the window. No maximize control either --
 * the window is fixed-size.
 */
@Composable
private fun LatchTitleBar(
    onMinimize: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TitleBarHeight)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f))
        TitleBarButton(
            icon = LatchIcons.Minimize,
            contentDescription = "Minimize",
            onClick = onMinimize,
            hoverColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TitleBarButton(
            icon = LatchIcons.Close,
            contentDescription = "Close",
            onClick = onClose,
            hoverColor = CloseHoverRed,
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
            hoverIconTint = Color.White,
        )
    }
}

@Composable
private fun TitleBarButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    hoverColor: Color,
    iconTint: Color,
    hoverIconTint: Color = iconTint,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .width(46.dp)
            .fillMaxHeight()
            .background(if (hovered) hoverColor else Color.Transparent)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (hovered) hoverIconTint else iconTint,
            modifier = Modifier.size(14.dp),
        )
    }
}
