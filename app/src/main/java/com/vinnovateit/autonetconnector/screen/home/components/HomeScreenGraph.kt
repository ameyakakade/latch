package com.vinnovateit.autonetconnector.screen.home.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vinnovateit.autonetconnector.functionality.LiveDataPoint
import com.vinnovateit.autonetconnector.screen.stats.ui.Tag
import com.vinnovateit.autonetconnector.screen.stats.utils.createGraphPaths
import com.vinnovateit.autonetconnector.screen.stats.utils.formatBytes
import kotlin.math.floor
import kotlin.math.max

private const val GRAPH_HEIGHT_SCALE = 0.7f
private val POINT_SPACING = 6.dp

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun HomeScreenGraph(
  modifier: Modifier = Modifier,
  rateHistory: List<LiveDataPoint>,
  scrollState: ScrollState = rememberScrollState(),
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
      haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
    }
  }

  val boundaryHapticTrigger by remember {
    derivedStateOf {
      zoomLevel == 1f || zoomLevel == 10f
    }
  }

  LaunchedEffect(boundaryHapticTrigger) {
    if (boundaryHapticTrigger) {
      haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
    }
  }

  LaunchedEffect(scrollState.maxValue) {
    if (scrollState.maxValue > 0) {
      scrollState.animateScrollTo(
        scrollState.maxValue,
        animationSpec = tween(
          durationMillis = (scrollState.maxValue * 5).toInt(), // Speed Increased
          easing = LinearEasing
        )
      )
    }
  }

  BoxWithConstraints(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .pointerInput(Unit) {
        detectTransformGestures { _, _, zoom, _ ->
          val newZoomLevel = zoomLevel * zoom
          zoomLevel = newZoomLevel.coerceIn(1f, 10f)
          scale = zoomLevel
        }
      }
  ) {
    val visibleMaxSpeedData by remember(rateHistory, scrollState.value, scale) {
      derivedStateOf {
        if (rateHistory.isEmpty()) {
          0L to true
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
        val graphWidthPx = with(density) { (POINT_SPACING * rateHistory.size * scale).toPx() }
        val graphHeightPx = with(density) { maxHeight.toPx() }
        createGraphPaths(rateHistory, graphWidthPx, graphHeightPx, animatedMaxSpeed.coerceAtLeast(1f), GRAPH_HEIGHT_SCALE)
      }
    }

    val dlColor = Color(0xFF0089D0)
    val ulColor = Color(0xFFFFA500)

    Box(
      modifier = Modifier
        .fillMaxSize()
        .horizontalScroll(scrollState)
    ) {
      val canvasWidth: Dp = POINT_SPACING * rateHistory.size * scale
      Canvas(modifier = Modifier.width(canvasWidth).fillMaxHeight()) {
        val dlBrush = Brush.verticalGradient(listOf(dlColor.copy(0.4f), Color.Transparent), endY = size.height * GRAPH_HEIGHT_SCALE)
        val ulBrush = Brush.verticalGradient(listOf(ulColor.copy(0.4f), Color.Transparent), endY = size.height * GRAPH_HEIGHT_SCALE)

        drawPath(graphData.downloadPath, brush = dlBrush)
        drawPath(graphData.uploadPath, brush = ulBrush)
        drawPath(graphData.lineDownloadPath, dlColor, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
        drawPath(graphData.lineUploadPath, ulColor, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
      }
    }

    // Max Speed Pill (Right Aligned)
    if (maxSpeed > 0) {
      val (value, unit) = formatBytes(maxSpeed)
      val tagColor = if (isDownloadMax) dlColor else ulColor
      Tag(
        text = "MAX $value${unit}/s",
        color = tagColor,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(16.dp)
      )
    }
  }
}