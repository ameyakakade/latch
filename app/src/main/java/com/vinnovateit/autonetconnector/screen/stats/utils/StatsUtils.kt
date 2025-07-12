// path: com/vinnovateit/autonetconnector/screen/stats/utils/StatsUtils.kt
package com.vinnovateit.autonetconnector.screen.stats.utils

import androidx.compose.ui.graphics.Path
import com.vinnovateit.autonetconnector.functionality.LiveDataPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

enum class DisplayMode { TOTAL, DOWNLOAD, UPLOAD }
enum class Timeframe { LIVE, LAST }

data class GraphData(
    val downloadPath: Path,
    val uploadPath: Path,
    val lineDownloadPath: Path,
    val lineUploadPath: Path,
    val labels: List<Pair<String, Float>>,
    val maxRateFormatted: String
)

fun formatBytes(bytes: Long): Pair<String, String> = when {
    bytes < 1_024L                    -> bytes.toString()         to "B"
    bytes < 1_048_576L               -> "%.1f".format(bytes / 1_024f)            to "KB"
    bytes < 1_073_741_824L           -> "%.1f".format(bytes / 1_048_576f)        to "MB"
    else                             -> "%.2f".format(bytes / 1_073_741_824f)    to "GB"
}

fun formatDurationDynamic(ms: Long): String {
    if (ms < 0) return "0s"
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return when {
        h > 0 -> if (m > 0) "${h}h ${m}m" else "${h}h"
        m > 0 -> "${m}m ${s}s"
        else  -> "${s}s"
    }
}

fun formatDate(millis: Long, pattern: String): String =
    SimpleDateFormat(pattern, Locale.US).format(Date(millis))

fun createGraphPaths(
    history: List<LiveDataPoint>,
    width: Float,
    height: Float
): GraphData {
    if (history.size < 2) {
        return GraphData(
            Path(), Path(), Path(), Path(), emptyList(), "0 B/s"
        )
    }

    val maxRx = history.maxOf { it.usage.rxBytes }.toFloat()
    val maxTx = history.maxOf { it.usage.txBytes }.toFloat()
    val maxRate = max(maxRx, maxTx).coerceAtLeast(1f)
    val maxLabel = formatBytes(maxRate.toLong()).let { "${it.first} ${it.second}/s" }

    val startTime = history.first().timestamp
    val duration = (history.last().timestamp - startTime).coerceAtLeast(1)

    fun x(t: Long) = ((t - startTime).toFloat() / duration) * width
    fun y(b: Long) = height - (b.toFloat() / maxRate) * height

    val fillDL = Path().apply { moveTo(0f, height) }
    val fillUL = Path().apply { moveTo(0f, height) }
    val lineDL = Path()
    val lineUL = Path()

    history.forEachIndexed { i, p ->
        val xp = x(p.timestamp)
        val yDL = y(p.usage.rxBytes)
        val yUL = y(p.usage.txBytes)

        if (i == 0) {
            listOf(lineDL, fillDL, lineUL, fillUL).forEach { it.moveTo(xp, if (it == lineUL || it == fillUL) yUL else yDL) }
        } else {
            val prev = history[i - 1]
            val xpPrev = x(prev.timestamp)
            val yDLPrev = y(prev.usage.rxBytes)
            val yULPrev = y(prev.usage.txBytes)
            val cx = (xpPrev + xp) / 2f

            lineDL.cubicTo(cx, yDLPrev, cx, yDL, xp, yDL)
            fillDL.cubicTo(cx, yDLPrev, cx, yDL, xp, yDL)

            lineUL.cubicTo(cx, yULPrev, cx, yUL, xp, yUL)
            fillUL.cubicTo(cx, yULPrev, cx, yUL, xp, yUL)
        }
    }

    fillDL.lineTo(width, height)
    fillDL.close()

    fillUL.lineTo(width, height)
    fillUL.close()

    val labels = (0..5).map { i ->
        val t = startTime + i * duration / 5
        formatDate(t, "hh:mm a") to x(t)
    }

    return GraphData(fillDL, fillUL, lineDL, lineUL, labels, maxLabel)
}