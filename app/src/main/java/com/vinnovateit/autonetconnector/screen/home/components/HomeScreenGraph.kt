package com.vinnovateit.autonetconnector.screen.home.components

import android.annotation.SuppressLint
import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinnovateit.autonetconnector.functionality2.manager.LiveDataPoint
import com.vinnovateit.autonetconnector.screen.stats.utils.createGraphPaths
import com.vinnovateit.autonetconnector.screen.stats.utils.formatBitsPerSecond
import com.vinnovateit.autonetconnector.ui.theme.GraphDownload
import com.vinnovateit.autonetconnector.ui.theme.GraphUpload
import com.vinnovateit.autonetconnector.ui.theme.Transparent
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlinx.coroutines.delay

private const val GRAPH_HEIGHT_SCALE = 0.7f
private val Y_AXIS_WIDTH = 70.dp
private const val POINTS_IN_ONE_MINUTE = 30  // 1 min * 30 points/min


/**
 * Calculates a "nice" rounded number for the top of the Y-axis.
 */
private fun calculateNiceMaxSpeed(maxSpeed: Float): Float {
  if (maxSpeed <= 0f) return 1f // CRASH FIX: Handle zero or negative maxSpeed
  val exponent = floor(log10(maxSpeed))
  val fraction = maxSpeed / 10f.pow(exponent)

  val niceFraction = when {
    fraction <= 1 -> 1f
    fraction <= 2 -> 2f
    fraction <= 5 -> 5f
    else -> 10f
  }
  return niceFraction * 10f.pow(exponent)
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun HomeScreenGraph(
  modifier: Modifier = Modifier,
  rateHistory: List<LiveDataPoint>
) {
  val initialScale = if (rateHistory.size > POINTS_IN_ONE_MINUTE) {
    rateHistory.size.toFloat() / POINTS_IN_ONE_MINUTE.toFloat()
  } else {
    1f
  }
  var scale by remember { mutableStateOf(initialScale) }
  val scrollState = rememberScrollState()
  var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
  var yAxisVisible by remember { mutableStateOf(true) }
  var isAutoScrolling by remember { mutableStateOf(false) }

  // Update interaction time on user-initiated scroll
  LaunchedEffect(scrollState.isScrollInProgress) {
    if (scrollState.isScrollInProgress && !isAutoScrolling) {
      lastInteraction = System.currentTimeMillis()
    }
  }

  // Timer for Y-axis visibility and auto-scrolling
  LaunchedEffect(lastInteraction, rateHistory.size) {
    while (true) {
      val timeSinceInteraction = System.currentTimeMillis() - lastInteraction

      // Visibility logic: Visible if user is interacting or within 5s of last interaction
      val isUserInteracting = scrollState.isScrollInProgress && !isAutoScrolling
      val shouldBeVisible = isUserInteracting || timeSinceInteraction < 5000L
      if (yAxisVisible != shouldBeVisible) {
        yAxisVisible = shouldBeVisible
      }

      // Auto-scroll logic: If idle for more than 5 seconds, scroll to the end
      if (!scrollState.isScrollInProgress && timeSinceInteraction > 5000) {
        if (scrollState.value != scrollState.maxValue) {
          isAutoScrolling = true
          scrollState.animateScrollTo(
            scrollState.maxValue,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
          )
          isAutoScrolling = false
        }
      }
      delay(200)
    }
  }


  Box(
    modifier = modifier
      .pointerInput(Unit) {
        detectTransformGestures { _, _, zoom, _ ->
          scale = (scale * zoom).coerceIn(1f, initialScale * 2) // Allow zooming in further, but not out past 1:1
          lastInteraction = System.currentTimeMillis() // Update interaction time on zoom
        }
      }
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val containerWidthPx = constraints.maxWidth
      val containerHeightPx = constraints.maxHeight

      if (containerWidthPx > 0 && rateHistory.size > 1) {

        // --- DYNAMIC MAX SPEED CALCULATION ---
        val maxSpeed by remember(rateHistory, scrollState.value, containerWidthPx, scale) {
          derivedStateOf {
            if (rateHistory.size < 2) return@derivedStateOf 1L

            val totalGraphWidthPx = containerWidthPx * scale
            val firstTimestamp = rateHistory.first().timestamp
            val totalDuration = (rateHistory.last().timestamp - firstTimestamp).coerceAtLeast(1)

            val visibleStartRatio = scrollState.value / totalGraphWidthPx
            val visibleEndRatio = (scrollState.value + containerWidthPx) / totalGraphWidthPx

            val visibleStartTime = firstTimestamp + (totalDuration * visibleStartRatio).toLong()
            val visibleEndTime = firstTimestamp + (totalDuration * visibleEndRatio).toLong()

            val visiblePoints = rateHistory.filter { it.timestamp in visibleStartTime..visibleEndTime }

            if (visiblePoints.isEmpty()) {
              1L
            } else {
              val maxRx = visiblePoints.maxOfOrNull { it.usage.rxBytes } ?: 0L
              val maxTx = visiblePoints.maxOfOrNull { it.usage.txBytes } ?: 0L
              max(maxRx, maxTx)
            }
          }
        }

        val animatedMaxSpeed by animateFloatAsState(
          targetValue = maxSpeed.toFloat(),
          animationSpec = tween(500, easing = FastOutSlowInEasing),
          label = "MaxSpeedAnimation"
        )

        val graphData by remember(rateHistory, containerWidthPx, containerHeightPx, scale, animatedMaxSpeed) {
          derivedStateOf {
            val graphWidthPx = containerWidthPx * scale
            createGraphPaths(
              history = rateHistory,
              width = graphWidthPx,
              height = containerHeightPx.toFloat(),
              maxRate = animatedMaxSpeed.coerceAtLeast(1f),
              graphHeightScale = GRAPH_HEIGHT_SCALE
            )
          }
        }

        val canvasWidthDp = with(LocalDensity.current) { (containerWidthPx * scale).toDp() }
        val backgroundColor = MaterialTheme.colorScheme.background
        val onBackgroundColor = MaterialTheme.colorScheme.onBackground

        // Parent Box for layering the graph and the Y-axis
        Box(modifier = Modifier.fillMaxSize()) {
          // Layer 1: The Scrollable Graph
          Box(
            modifier = Modifier
              .fillMaxSize()
              .horizontalScroll(scrollState),
            contentAlignment = Alignment.CenterEnd
          ) {
            Canvas(
              modifier = Modifier
                .width(canvasWidthDp)
                .fillMaxHeight()
            ) {
              val dlBrush = Brush.verticalGradient(listOf(GraphDownload.copy(0.4f), Transparent))
              val ulBrush = Brush.verticalGradient(listOf(GraphUpload.copy(0.4f), Transparent))

              drawPath(graphData.downloadPath, brush = dlBrush)
              drawPath(graphData.uploadPath, brush = ulBrush)
              drawPath(graphData.lineDownloadPath, GraphDownload, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
              drawPath(graphData.lineUploadPath, GraphUpload, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
            }
          }

          // Layer 2: The Y-Axis with Faded Background and Timed Visibility
          AnimatedVisibility(
            visible = yAxisVisible,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(1000))
          ) {
            Box(
              modifier = Modifier
                .fillMaxHeight()
                .width(Y_AXIS_WIDTH)
                .background(
                  brush = Brush.horizontalGradient(
                    colors = listOf(
                      backgroundColor,
                      backgroundColor.copy(alpha = 0.8f),
                      backgroundColor.copy(alpha = 0.5f),
                      Transparent
                    )
                  )
                )
            ) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                val axisPaint = Paint().apply {
                  color = onBackgroundColor.copy(alpha = 0.7f).toArgb()
                  textAlign = Paint.Align.LEFT
                  textSize = 12.sp.toPx()
                }
                val rulerTopValue = calculateNiceMaxSpeed(animatedMaxSpeed / 2)
                val numLines = 4

                for (i in 0..numLines) {
                  val fraction = i.toFloat() / numLines
                  val yValue = rulerTopValue * fraction
                  val yPos = size.height - ((yValue / rulerTopValue) * (size.height * GRAPH_HEIGHT_SCALE))
                  val (value, unit) = formatBitsPerSecond(yValue.toLong(), includeUnit = i == numLines)
                  val markingEnd = (4 + i * 2).dp.toPx()

                  if (i > 0) {
                    drawContext.canvas.nativeCanvas.drawText(
                      "$value $unit",
                      markingEnd + 4.dp.toPx(),
                      yPos + 4.dp.toPx(),
                      axisPaint
                    )
                  }

                  drawLine(
                    color = onBackgroundColor.copy(alpha = 0.5f),
                    start = Offset(0f, yPos),
                    end = Offset(markingEnd, yPos),
                    strokeWidth = 2.dp.toPx()
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
