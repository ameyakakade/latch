// path: com/vinnovateit/autonetconnector/screen/stats/components/SessionCard.kt
package com.vinnovateit.autonetconnector.screen.stats.components

import android.R.attr.textSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinnovateit.autonetconnector.functionality.DataUsage
import com.vinnovateit.autonetconnector.functionality.LiveDataPoint
import com.vinnovateit.autonetconnector.functionality.SessionSummary
import com.vinnovateit.autonetconnector.screen.stats.ui.Tag
import com.vinnovateit.autonetconnector.screen.stats.utils.DisplayMode
import com.vinnovateit.autonetconnector.screen.stats.utils.Timeframe
import com.vinnovateit.autonetconnector.screen.stats.utils.createGraphPaths
import com.vinnovateit.autonetconnector.screen.stats.utils.formatBytes
import com.vinnovateit.autonetconnector.screen.stats.utils.formatDurationDynamic
import kotlinx.coroutines.delay
import kotlin.math.atan2

@Composable
fun SessionCard(
    timeframe: Timeframe,
    session: SessionSummary,
    isLive: Boolean
) {
    var isGraphExpanded by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val scrollState = rememberScrollState()

    LaunchedEffect(isGraphExpanded) {
        if (isGraphExpanded) scrollState.animateScrollTo(scrollState.maxValue)
    }

    LaunchedEffect(isGraphExpanded, lastInteractionTime) {
        if (isGraphExpanded) {
            delay(3000)
            if (System.currentTimeMillis() - lastInteractionTime >= 3000) {
                isGraphExpanded = false
            }
        }
    }

    AnimatedContent(
        modifier = Modifier.padding(16.dp),
        targetState = isGraphExpanded,
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
        label = "SessionCardAnimation"
    ) { expanded ->
        if (expanded) {
            ExpandedGraphCard(
                rateHistory = session.history,
                onInteraction = { lastInteractionTime = System.currentTimeMillis() },
                onDismiss = { isGraphExpanded = false },
                scrollState = scrollState
            )
        } else {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    SessionCardHeader(timeframe, session, isLive)
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DataUsageCircle(
                            modifier = Modifier.size(100.dp),
                            dataUsage = session.totalData
                        )
                        SessionRateGraph(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { isGraphExpanded = true },
                            rateHistory = session.history.takeLast(60)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SessionCardHeader(
    timeframe: Timeframe,
    session: SessionSummary,
    isLive: Boolean
) {
    var duration by remember(session.startTimestamp) {
        mutableLongStateOf(System.currentTimeMillis() - session.startTimestamp)
    }

    if (isLive) {
        LaunchedEffect(Unit) {
            while (true) {
                duration = System.currentTimeMillis() - session.startTimestamp
                delay(1000)
            }
        }
    } else {
        duration = session.endTimestamp - session.startTimestamp
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f, fill = false)) {
            Text(
                text = session.ssid,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatDurationDynamic(duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Tag(
            text = timeframe.name,
            color = if (timeframe == Timeframe.LIVE) Color.Red else Color.Gray
        )
    }
}

@Composable
fun DataUsageCircle(
    modifier: Modifier = Modifier,
    dataUsage: DataUsage
) {
    var displayMode by remember { mutableStateOf(DisplayMode.TOTAL) }

    LaunchedEffect(displayMode) {
        if (displayMode != DisplayMode.TOTAL) {
            delay(3_000)
            displayMode = DisplayMode.TOTAL
        }
    }

    val totalBytes = dataUsage.rxBytes + dataUsage.txBytes
    val downloadFraction by animateFloatAsState(
        targetValue = if (totalBytes > 0) dataUsage.rxBytes.toFloat() / totalBytes else 0f,
        animationSpec = tween(500),
        label = "DownloadFraction"
    )

    val dlColor = Color(0xFF0089D0)
    val ulColor = Color(0xFFFFA500)

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { tap ->
                val cx = size.width / 2f
                val cy = size.height / 2f
                val angle = (Math.toDegrees(
                    atan2(tap.y - cy, tap.x - cx).toDouble()
                ) + 450) % 360
                displayMode =
                    if (angle < downloadFraction * 360) DisplayMode.DOWNLOAD else DisplayMode.UPLOAD
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            drawArc(
                color = ulColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(stroke)
            )
            drawArc(
                color = dlColor,
                startAngle = -90f,
                sweepAngle = downloadFraction * 360f,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }

        val (valueStr, unitStr) = remember(dataUsage, displayMode) {
            val bytes = when (displayMode) {
                DisplayMode.TOTAL    -> totalBytes
                DisplayMode.DOWNLOAD -> dataUsage.rxBytes
                DisplayMode.UPLOAD   -> dataUsage.txBytes
            }
            formatBytes(bytes)
        }

        AnimatedContent(
            targetState = displayMode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "DataCircleContent"
        ) { mode ->
            val icon = when (mode) {
                DisplayMode.DOWNLOAD -> Icons.Default.ArrowDownward
                DisplayMode.UPLOAD   -> Icons.Default.ArrowUpward
                else                 -> null
            }
            val tint = when (mode) {
                DisplayMode.DOWNLOAD -> dlColor
                DisplayMode.UPLOAD   -> ulColor
                else                 -> MaterialTheme.colorScheme.onSurface
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = mode.name,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    valueStr,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    unitStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f)
                )
            }
        }
    }
}

@Composable
fun SessionRateGraph(
    modifier: Modifier = Modifier,
    rateHistory: List<LiveDataPoint>,
    isInteractive: Boolean = false,
    showAxis: Boolean = false,
    scrollState: ScrollState = rememberScrollState(),
    onInteraction: () -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current

    val transformable = rememberTransformableState { zoomChange, _, _ ->
        if (isInteractive) {
            scale = (scale * zoomChange).coerceIn(1f, 10f)
            onInteraction()
        }
    }

    LaunchedEffect(rateHistory.size) {
        if (!isInteractive) scrollState.animateScrollTo(scrollState.maxValue)
    }

    LaunchedEffect(scrollState.value) {
        if (scrollState.isScrollInProgress) onInteraction()
    }

    val dlColor = Color(0xFF0089D0)
    val ulColor = Color(0xFFFFA500)
    val axisCol = MaterialTheme.colorScheme.onSurfaceVariant.copy(.5f)

    val graphData by remember(rateHistory, scale, density) {
        derivedStateOf {
            val w = with(density) { (300.dp * scale).toPx() }
            val h = with(density) { (if (isInteractive) 200.dp else 120.dp).toPx() }
            createGraphPaths(rateHistory, w, h)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(if (isInteractive) Modifier.transformable(transformable) else Modifier)
            .horizontalScroll(scrollState, enabled = isInteractive)
            .padding(
                start = if (showAxis) 48.dp else 0.dp,
                end = if (showAxis) 16.dp else 0.dp,
                top = if (showAxis) 8.dp else 0.dp,
                bottom = if (showAxis) 8.dp else 0.dp
            )
    ) {
        val wDp = 300.dp * scale
        val hDp = (if (isInteractive) 200.dp else 120.dp) * scale

        Canvas(Modifier.width(wDp).height(hDp)) {
            drawPath(
                graphData.downloadPath,
                Brush.verticalGradient(listOf(dlColor.copy(.4f), Color.Transparent))
            )
            drawPath(
                graphData.uploadPath,
                Brush.verticalGradient(listOf(ulColor.copy(.4f), Color.Transparent))
            )

            drawPath(graphData.lineDownloadPath, dlColor, style = Stroke(1.5.dp.toPx()))
            drawPath(graphData.lineUploadPath, ulColor, style = Stroke(1.5.dp.toPx()))

            if (showAxis) {
                val textPaint = Paint().apply {
                    color = axisCol.toArgb()
                    textAlign = Paint.Align.RIGHT
                    textSize = 10.sp.toPx()
                }

                drawLine(axisCol, Offset.Zero, Offset(0f, size.height), 1.dp.toPx())
                drawLine(axisCol, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())

                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        graphData.maxRateFormatted,
                        -8.dp.toPx(),
                        10.sp.toPx(),
                      textPaint as android.graphics.Paint
                    )
                    canvas.nativeCanvas.drawText(
                        "0 B/s",
                        -8.dp.toPx(),
                        size.height,
                        textPaint
                    )
                    graphData.labels.forEach { (txt, x) ->
                        textPaint.textAlign = Paint.Align.CENTER
                        canvas.nativeCanvas.drawText(
                            txt,
                            x,
                            size.height + 14.dp.toPx(),
                            textPaint
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandedGraphCard(
    rateHistory: List<LiveDataPoint>,
    onInteraction: () -> Unit,
    onDismiss: () -> Unit,
    scrollState: ScrollState
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDismiss),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        SessionRateGraph(
            rateHistory = rateHistory,
            isInteractive = true,
            showAxis = true,
            onInteraction = onInteraction,
            scrollState = scrollState,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        )
    }
}