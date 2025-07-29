package com.vinnovateit.autonetconnector.screen.stats.utils

import androidx.compose.ui.graphics.Path
import com.vinnovateit.autonetconnector.functionality2.manager.LiveDataPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class DisplayMode { TOTAL, DOWNLOAD, UPLOAD }
enum class Timeframe { LIVE, LAST }

data class GraphData(
    val downloadPath: Path,
    val uploadPath: Path,
    val lineDownloadPath: Path,
    val lineUploadPath: Path,
    val labels: List<Pair<String, Float>>,
    private val startTime: Long,
    private val duration: Long,
    private val width: Float,
    private val height: Float,
    private val maxRate: Float,
    private val graphHeightScale: Float
) {
    fun xAtTimestamp(timestamp: Long): Float {
        val elapsedTime = (timestamp - startTime).toFloat()
        return (elapsedTime / duration) * width
    }

    fun yAtRate(rate: Long): Float {
        val usageFraction = (rate.toFloat() / maxRate).coerceIn(0f, 1f)
        return height - (usageFraction * height * graphHeightScale)
    }

    fun timestampAtX(x: Float): Long {
        return startTime + ((x / width) * duration).toLong()
    }
}


fun formatBytes(bytes: Long): Pair<String, String> = when {
    bytes < 1_024L                    -> bytes.toString() to "B"
    bytes < 1_048_576L               -> {
        val kbFormatted = "%.1f".format(bytes / 1_024f)
        if (kbFormatted == "1024.0") {
            "1.0" to "MB"
        } else {
            kbFormatted to "KB"
        }
    }
    bytes < 1_073_741_824L           -> {
        val mbFormatted = "%.1f".format(bytes / 1_048_576f)
        if (mbFormatted == "1024.0") {
            "1.0" to "GB"
        } else {
            mbFormatted to "MB"
        }
    }
    else                             -> "%.2f".format(bytes / 1_073_741_824f)    to "GB"
}

fun formatBitsPerSecond(bytesPerSecond: Long, includeUnit: Boolean = true): Pair<String, String> {
    val bitsPerSecond = bytesPerSecond * 8
    val (value, unit) = when {
        bitsPerSecond < 1_000L -> bitsPerSecond.toString() to "bps"
        bitsPerSecond < 1_000_000L -> "%.1f".format(bitsPerSecond / 1_000f) to "Kbps"
        bitsPerSecond < 1_000_000_000L -> "%.1f".format(bitsPerSecond / 1_000_000f) to "Mbps"
        else -> "%.2f".format(bitsPerSecond / 1_000_000_000f) to "Gbps"
    }
    return if (includeUnit) value to unit else value to ""
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
    height: Float,
    maxRate: Float,
    graphHeightScale: Float = 0.6f
): GraphData {
    if (history.size < 2) {
        return GraphData(Path(), Path(), Path(), Path(), emptyList(), 0, 0, 0f, 0f, 0f, 0f)
    }

    val effectiveMaxRate = maxRate.coerceAtLeast(1f)
    val startTime = history.first().timestamp
    val duration = (history.last().timestamp - startTime).coerceAtLeast(1)

    fun x(t: Long): Float {
        val elapsedTime = (t - startTime).toFloat()
        return (elapsedTime / duration) * width
    }

    fun y(bytes: Long): Float {
        val usageFraction = (bytes.toFloat() / effectiveMaxRate).coerceIn(0f, 1f)
        return height - (usageFraction * height * graphHeightScale)
    }

    val fillDL = Path().apply { moveTo(0f, height) }
    val fillUL = Path().apply { moveTo(0f, height) }
    val lineDL = Path()
    val lineUL = Path()

    history.forEachIndexed { i, p ->
        val xp = x(p.timestamp)
        val yDL = y(p.usage.rxBytes)
        val yUL = y(p.usage.txBytes)

        if (i == 0) {
            lineDL.moveTo(xp, yDL)
            fillDL.lineTo(xp, yDL)
            lineUL.moveTo(xp, yUL)
            fillUL.lineTo(xp, yUL)
        } else {
            val prevPoint = history[i - 1]
            val prevX = x(prevPoint.timestamp)
            val prevYDL = y(prevPoint.usage.rxBytes)
            val prevYUL = y(prevPoint.usage.txBytes)
            val cx = (prevX + xp) / 2f

            lineDL.cubicTo(cx, prevYDL, cx, yDL, xp, yDL)
            fillDL.cubicTo(cx, prevYDL, cx, yDL, xp, yDL)

            lineUL.cubicTo(cx, prevYUL, cx, yUL, xp, yUL)
            fillUL.cubicTo(cx, prevYUL, cx, yUL, xp, yUL)
        }
    }

    val lastX = x(history.last().timestamp)
    fillDL.lineTo(lastX, height)
    fillDL.close()

    fillUL.lineTo(lastX, height)
    fillUL.close()

    val labels = if (history.size > 1) {
        (0..5).map { i ->
            val timestamp = startTime + (i * duration / 5)
            formatDate(timestamp, "hh:mm a") to x(timestamp)
        }
    } else {
        emptyList()
    }

    return GraphData(fillDL, fillUL, lineDL, lineUL, labels, startTime, duration, width, height, effectiveMaxRate, graphHeightScale)
}