package com.vinnovateit.latch.desktop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import kotlin.math.min

/**
 * The tray icon, drawn rather than loaded from a raster asset.
 *
 * java.awt.TrayIcon scales bitmaps poorly, and a 24dp vector rasterised down to
 * 16px reads as mush. Drawing a simple high-contrast shape stays legible at every
 * size Windows might ask for, and lets the icon reflect state without shipping a
 * separate PNG per state.
 *
 * Note this is the tray icon only -- the MSI still needs a real multi-resolution
 * .ico, which jpackage requires and will not accept a PNG for.
 */
internal class LatchTrayIcon(private val latched: Boolean) : Painter() {

    override val intrinsicSize: Size = Size(32f, 32f)

    override fun DrawScope.onDraw() {
        val dimension = min(size.width, size.height)
        val center = Offset(size.width / 2f, size.height / 2f)

        val accent = if (latched) Color(0xFF167D00) else Color(0xFF9E9E9E)

        // Outer ring: always drawn so the icon has a stable silhouette.
        drawCircle(
            color = accent,
            radius = dimension * 0.46f,
            center = center,
        )

        // Punch out the middle so it reads as a ring rather than a blob.
        drawCircle(
            color = Color(0x00000000),
            radius = dimension * 0.30f,
            center = center,
            blendMode = androidx.compose.ui.graphics.BlendMode.Clear,
        )

        // Filled core only when latched, giving an unmistakable on/off read at 16px.
        if (latched) {
            drawCircle(
                color = accent,
                radius = dimension * 0.18f,
                center = center,
            )
        }
    }
}
