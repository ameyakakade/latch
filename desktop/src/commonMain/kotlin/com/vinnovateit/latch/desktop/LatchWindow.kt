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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
private val TitleBarHeight = 36.dp

/** What the window opens at when there is room for it. */
private const val PREFERRED_W = 460f
private const val PREFERRED_H = 1000f

/**
 * The floor the preferred size is clamped to on small screens. Below this the
 * responsive layouts stop having anywhere to put things: the compact home
 * arrangement needs roughly this much height for the panel and the power button
 * not to overlap.
 */
private const val MIN_W = 420
private const val MIN_H = 600

/**
 * The window's size -- its only one, since the window is not resizable: the
 * preferred size, clamped to 90% of the usable screen so it never opens larger
 * than the display it lands on.
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
        undecorated = true,
        title = "Latch",
        icon = remember { LatchIcon.brand() },
    ) {
        LatchTheme {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
 * The custom title bar replacing the OS one: brand mark + wordmark on the left
 * (draggable, via the enclosing [WindowDraggableArea]), minimize/close on the
 * right. Deliberately no maximize control -- the window is fixed-size.
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
        Icon(
            imageVector = LatchMark,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp).size(16.dp),
        )
        Text(
            text = "Latch",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp).weight(1f),
        )
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
