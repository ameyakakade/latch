package com.vinnovateit.autonetconnector.screen.stats.components

import android.annotation.SuppressLint
import android.graphics.Paint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.vinnovateit.autonetconnector.ui.theme.Pink40
import com.vinnovateit.autonetconnector.ui.theme.Purple40
import com.vinnovateit.autonetconnector.ui.theme.PurpleGrey40
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.max

private val CardLightColorScheme = lightColorScheme(
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF1F1F1),
    onSurfaceVariant = Color(0xFF49454F),
    background = Color.White,
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private const val GRAPH_HEIGHT_SCALE = 0.7f
private val POINT_SPACING = 6.dp
private val AXIS_LABEL_HEIGHT = 20.dp
private val CARD_CORNER_RADIUS = 24.dp

@Composable
fun SessionCard(
    timeframe: Timeframe,
    session: SessionSummary,
    isLive: Boolean,
    overrideSsid: String? = null
) {
    var isGraphExpanded by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val scrollState = rememberScrollState()

    LaunchedEffect(isGraphExpanded, lastInteractionTime) {
        if (isGraphExpanded) {
            delay(5000)
            if (System.currentTimeMillis() - lastInteractionTime >= 5000) {
                isGraphExpanded = false
            }
        }
    }

    MaterialTheme(colorScheme = CardLightColorScheme) {
        AnimatedContent(
            // Removed outer padding from here. It's now handled by the parent container.
            targetState = isGraphExpanded,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
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
                    shape = RoundedCornerShape(CARD_CORNER_RADIUS),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(24.dp)) {
                        SessionCardHeader(timeframe, session, isLive, overrideSsid)
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DataUsageCircle(
                                modifier = Modifier.size(80.dp),
                                dataUsage = session.totalData
                            )
                            Spacer(Modifier.width(24.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { isGraphExpanded = true },
                                contentAlignment = Alignment.Center
                            ) {
                                SessionRateGraph(
                                    modifier = Modifier.fillMaxSize(),
                                    rateHistory = session.history.takeLast(60),
                                    isInteractive = false
                                )
                                FadedEdge(alignment = Alignment.CenterStart)
                                FadedEdge(alignment = Alignment.CenterEnd)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ExpandedGraphCard(
    rateHistory: List<LiveDataPoint>,
    onInteraction: () -> Unit,
    onDismiss: () -> Unit,
    scrollState: ScrollState
) {
    // This effect triggers the auto-scroll animation when the card is expanded.
    LaunchedEffect(scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            scrollState.animateScrollTo(
                scrollState.maxValue,
                animationSpec = tween(
                    durationMillis = (scrollState.maxValue * 10).toInt(), // Duration scales with graph width
                    easing = LinearEasing
                )
            )
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss
            ),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(CARD_CORNER_RADIUS),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        SessionRateGraph(
            modifier = Modifier.fillMaxSize(),
            rateHistory = rateHistory,
            isInteractive = true,
            scrollState = scrollState,
            onInteraction = onInteraction
        )
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun SessionRateGraph(
    modifier: Modifier = Modifier,
    rateHistory: List<LiveDataPoint>,
    isInteractive: Boolean,
    scrollState: ScrollState = rememberScrollState(),
    onInteraction: () -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var zoomLevel by remember { mutableFloatStateOf(1f) }

    val hapticFeedbackTrigger by remember {
        derivedStateOf {
            floor(zoomLevel / 0.5f)
        }
    }

    LaunchedEffect(hapticFeedbackTrigger) {
        if (zoomLevel > 1f && zoomLevel < 10f) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val boundaryHapticTrigger by remember {
        derivedStateOf {
            zoomLevel == 1f || zoomLevel == 10f
        }
    }

    LaunchedEffect(boundaryHapticTrigger) {
        if (boundaryHapticTrigger) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(CARD_CORNER_RADIUS))
            .pointerInput(Unit) {
                if (isInteractive) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val newZoomLevel = zoomLevel * zoom
                        zoomLevel = newZoomLevel.coerceIn(1f, 10f)
                        scale = zoomLevel
                        onInteraction()
                    }
                }
            }
    ) {
        val visibleMaxSpeedData by remember(rateHistory, scrollState.value, scale) {
            derivedStateOf {
                if (!isInteractive || rateHistory.isEmpty()) {
                    val maxRx = rateHistory.maxOfOrNull { it.usage.rxBytes } ?: 0L
                    val maxTx = rateHistory.maxOfOrNull { it.usage.txBytes } ?: 0L
                    max(maxRx, maxTx) to (maxRx >= maxTx)
                } else {
                    val pointWidthPx = with(density) { (POINT_SPACING * scale).toPx() }
                    val viewportWidthPx = with(density) { maxWidth.toPx() }
                    val startPointIndex = (scrollState.value / pointWidthPx).toInt().coerceAtMost(rateHistory.size - 1)
                    val visiblePointsCount = (viewportWidthPx / pointWidthPx).toInt() + 2
                    val endPointIndex = (startPointIndex + visiblePointsCount).coerceAtMost(rateHistory.size)
                    val sublist = rateHistory.subList(startPointIndex, endPointIndex)
                    val maxRx = sublist.maxOfOrNull { it.usage.rxBytes } ?: 0L
                    val maxTx = sublist.maxOfOrNull { it.usage.txBytes } ?: 0L
                    max(maxRx, maxTx) to (maxRx >= maxTx)
                }
            }
        }

        val (maxSpeed, isDownloadMax) = visibleMaxSpeedData
        val animatedMaxSpeed by animateFloatAsState(
            targetValue = maxSpeed.toFloat(),
            animationSpec = tween(500),
            label = "MaxSpeed"
        )

        val graphData by remember(rateHistory, scale, animatedMaxSpeed) {
            derivedStateOf {
                val axisHeightPx = if (isInteractive) with(density) { AXIS_LABEL_HEIGHT.toPx() } else 0f
                val graphWidthPx = with(density) { (POINT_SPACING * rateHistory.size * scale).toPx() }
                val graphHeightPx = with(density) { maxHeight.toPx() } - axisHeightPx
                createGraphPaths(rateHistory, graphWidthPx, graphHeightPx, animatedMaxSpeed.coerceAtLeast(1f), GRAPH_HEIGHT_SCALE)
            }
        }

        val dlColor = Color(0xFF0089D0)
        val ulColor = Color(0xFFFFA500)
        val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isInteractive) Modifier.horizontalScroll(scrollState) else Modifier)
        ) {
            val canvasWidth: Dp = POINT_SPACING * rateHistory.size * scale
            Canvas(modifier = Modifier.width(canvasWidth).fillMaxHeight()) {
                val axisAreaHeight = if (isInteractive) AXIS_LABEL_HEIGHT.toPx() else 0f
                val graphAreaHeight = size.height - axisAreaHeight

                val dlBrush = Brush.verticalGradient(listOf(dlColor.copy(0.4f), Color.Transparent), endY = graphAreaHeight)
                val ulBrush = Brush.verticalGradient(listOf(ulColor.copy(0.4f), Color.Transparent), endY = graphAreaHeight)

                drawPath(graphData.downloadPath, brush = dlBrush)
                drawPath(graphData.uploadPath, brush = ulBrush)
                drawPath(graphData.lineDownloadPath, dlColor, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
                drawPath(graphData.lineUploadPath, ulColor, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))

                if (isInteractive) {
                    val axisY = graphAreaHeight
                    val textPaint = Paint().apply {
                        color = axisColor.toArgb()
                        textAlign = Paint.Align.CENTER
                        textSize = 10.sp.toPx()
                    }
                    drawLine(axisColor, Offset(0f, axisY), Offset(size.width, axisY), 1.dp.toPx())

                    val labelMinSpacing = 70.dp.toPx()
                    val maxLabels = (size.width / labelMinSpacing).toInt().coerceAtLeast(2)
                    if (graphData.labels.isNotEmpty()) {
                        val step = (graphData.labels.size / maxLabels).coerceAtLeast(1)
                        graphData.labels.filterIndexed { i, _ -> i % step == 0 }.forEach { (txt, x) ->
                            drawContext.canvas.nativeCanvas.drawText(txt, x, axisY + 12.dp.toPx(), textPaint)
                        }
                    }
                }
            }
        }

        if (isInteractive && maxSpeed > 0) {
            val (value, unit) = formatBytes(maxSpeed)
            val tagColor = if (isDownloadMax) dlColor else ulColor
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Tag(text = "MAX $value${unit}/s", color = tagColor)
            }
        }
    }
}

@Composable
private fun BoxScope.FadedEdge(alignment: Alignment) {
    val colors = listOf(MaterialTheme.colorScheme.surface, Color.Transparent)
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(30.dp)
            .align(alignment)
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (alignment == Alignment.CenterStart) colors else colors.reversed()
                )
            )
    )
}

@Composable
fun SessionCardHeader(
    timeframe: Timeframe,
    session: SessionSummary,
    isLive: Boolean,
    overrideSsid: String? = null
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
                text = overrideSsid ?: session.ssid,
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
                val angle = (Math.toDegrees(atan2(tap.y - cy, tap.x - cx).toDouble()) + 450) % 360
                displayMode = if (angle < downloadFraction * 360) DisplayMode.DOWNLOAD else DisplayMode.UPLOAD
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            drawArc(color = ulColor, -90f, 360f, useCenter = false, style = Stroke(stroke))
            drawArc(dlColor, -90f, downloadFraction * 360f, false, style = Stroke(stroke, cap = StrokeCap.Round))
        }

        val (valueStr, unitStr) = remember(dataUsage, displayMode) {
            val bytes = when (displayMode) {
                DisplayMode.TOTAL -> totalBytes
                DisplayMode.DOWNLOAD -> dataUsage.rxBytes
                DisplayMode.UPLOAD -> dataUsage.txBytes
            }
            formatBytes(bytes)
        }

        AnimatedContent(targetState = displayMode, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "DataCircleContent") { mode ->
            val icon = when (mode) {
                DisplayMode.DOWNLOAD -> Icons.Default.ArrowDownward
                DisplayMode.UPLOAD -> Icons.Default.ArrowUpward
                else -> null
            }
            val tint = when (mode) {
                DisplayMode.DOWNLOAD -> dlColor
                DisplayMode.UPLOAD -> ulColor
                else -> MaterialTheme.colorScheme.onSurface
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (icon != null) {
                    Icon(icon, mode.name, tint = tint, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.height(2.dp))
                }
                Text(valueStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(unitStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f))
            }
        }
    }
}
