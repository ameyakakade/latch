package com.vinnovateit.latch.ui.components

import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/**
 * The themed leaf background overlay, ported from Android's ThemedDrawables.LeafOverlay.
 *
 * Path data and viewport are taken directly from the Android source so the visual
 * is pixel-identical on desktop. The vector is rebuilt only when the primary colour
 * changes (accent switch or dark/light toggle), not on every recomposition.
 */
@Composable
internal fun LeafOverlay(
    modifier: Modifier = Modifier,
    contentDescription: String?,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.None,
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val themedVector = remember(primaryColor) {
        ImageVector.Builder(
            name = "LeafOverlay",
            defaultWidth = 402.dp,
            defaultHeight = 456.dp,
            viewportWidth = 402f,
            viewportHeight = 456f,
        ).apply {
            addPath(
                fill = SolidColor(primaryColor),
                fillAlpha = 0.085f,
                pathData = PathParser().parsePathString(
                    "M190.48,418.69C121.88,394.11 66.58,358.72 21.53,318.42" +
                        "C-65.51,240.76 -112.61,143.45 -138.21,66.78" +
                        "C-97.25,133.62 -39.91,203.41 38.94,261.41" +
                        "C141.33,336.11 281.61,388.21 469,381.33" +
                        "C467.98,394.11 469,405.91 469,418.69" +
                        "C469,430.48 469,442.28 467.98,454.07" +
                        "C357.39,459.97 265.23,446.21 190.48,418.69Z",
                ).toNodes(),
            )
            addPath(
                fill = SolidColor(primaryColor),
                fillAlpha = 0.085f,
                pathData = PathParser().parsePathString(
                    "M9.52,197.87C-67.27,129.06 -113.35,45.5 -141,-26.25" +
                        "C-134.86,-43.95 -128.71,-59.68 -124.62,-75.4" +
                        "C-97.99,-39.03 -70.35,-6.6 -43.72,21.91" +
                        "C-11.98,54.35 17.71,66.15 34.1,72.04" +
                        "C147.76,110.38 229.68,63.2 229.68,63.2" +
                        "C182.57,126.11 127.28,137.9 85.3,135.94" +
                        "C230.7,238.17 340.26,233.26 340.26,233.26" +
                        "C340.26,233.26 317.73,-43.95 -121.54,-175.67" +
                        "C177.45,-164.86 421.15,52.38 461.09,332.54" +
                        "C248.11,345.32 104.75,283.39 9.52,197.87Z",
                ).toNodes(),
            )
        }.build()
    }

    Image(
        painter = rememberVectorPainter(themedVector),
        contentDescription = contentDescription,
        modifier = modifier,
        alignment = alignment,
        contentScale = contentScale,
    )
}
