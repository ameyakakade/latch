package com.vinnovateit.autonetconnector.screen.home.components

import android.annotation.SuppressLint
import android.graphics.Paint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinnovateit.autonetconnector.functionality.LiveDataPoint
import com.vinnovateit.autonetconnector.screen.stats.utils.createGraphPaths
import com.vinnovateit.autonetconnector.screen.stats.utils.formatBytes
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

private const val GRAPH_HEIGHT_SCALE = 0.7f
private val Y_AXIS_WIDTH = 70.dp // Dedicated space for the Y-axis ruler

/**
 * Calculates a "nice" rounded number for the top of the Y-axis.
 */
private fun calculateNiceMaxSpeed(maxSpeed: Float): Float {
  if (maxSpeed == 0f) return 1f
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

@Composable
private fun Int.toDp(): Dp = with(LocalDensity.current) { this@toDp.toDp() }

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun HomeScreenGraph(
  modifier: Modifier = Modifier,
  rateHistory: List<LiveDataPoint>
) {
  var scale by remember { mutableStateOf(1f) }
  val scrollState = rememberScrollState()
  val isInteracting = scrollState.isScrollInProgress

  // Auto-scroll with a smooth ease-in-out animation
  LaunchedEffect(rateHistory.size, isInteracting) {
    if (!isInteracting) {
      scrollState.animateScrollTo(
        scrollState.maxValue,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
      )
    }
  }

  Box(
    modifier = modifier
      .pointerInput(Unit) {
        detectTransformGestures { _, _, zoom, _ ->
          scale = (scale * zoom).coerceIn(1f, 10f)
        }
      }
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val containerWidthPx = constraints.maxWidth
      val containerHeightPx = constraints.maxHeight
      val yAxisWidthPx = with(LocalDensity.current) { Y_AXIS_WIDTH.toPx() }

      if (containerWidthPx > 0 && rateHistory.size > 1) {

        val maxSpeed by remember(rateHistory, scrollState.value, scale, containerWidthPx) {
          derivedStateOf {
            val totalWidth = containerWidthPx * scale
            val visibleStartPx = scrollState.value.toFloat()
            val visibleEndPx = visibleStartPx + containerWidthPx
            val startFraction = (visibleStartPx / totalWidth).coerceIn(0f, 1f)
            val endFraction = (visibleEndPx / totalWidth).coerceIn(0f, 1f)
            val startIndex = (startFraction * rateHistory.size).toInt()
            val endIndex = (endFraction * rateHistory.size).toInt().coerceAtMost(rateHistory.size)
            if (startIndex >= endIndex) return@derivedStateOf 0L
            val sublist = rateHistory.subList(startIndex, endIndex)
            val maxRx = sublist.maxOfOrNull { it.usage.rxBytes } ?: 0L
            val maxTx = sublist.maxOfOrNull { it.usage.txBytes } ?: 0L
            max(maxRx, maxTx)
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

        // Parent Box for layering
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
              val dlColor = Color(0xFF0089D0)
              val ulColor = Color(0xFFFFA500)
              val dlBrush = Brush.verticalGradient(listOf(dlColor.copy(0.4f), Color.Transparent))
              val ulBrush = Brush.verticalGradient(listOf(ulColor.copy(0.4f), Color.Transparent))

              drawPath(graphData.downloadPath, brush = dlBrush)
              drawPath(graphData.uploadPath, brush = ulBrush)
              drawPath(graphData.lineDownloadPath, dlColor, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
              drawPath(graphData.lineUploadPath, ulColor, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
            }
          }

          // Layer 2: The Y-Axis with Faded Background
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .width(Y_AXIS_WIDTH)
              .background(
                brush = Brush.horizontalGradient(
                  colors = listOf(
                    Color(0xFF1A237E).copy(alpha = 1f),
                    Color(0xFF1A237E).copy(alpha = 0.8f),
                    Color(0xFF1A237E).copy(alpha = 0.7f),
                    Color(0xFF1A237E).copy(alpha = 0.5f),
                    Color.Transparent
                  )
                )
              )
          ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              val axisPaint = Paint().apply {
                color = Color.White.copy(alpha = 0.7f).toArgb()
                textAlign = Paint.Align.RIGHT
                textSize = 12.sp.toPx()
              }
              val rulerTopValue = calculateNiceMaxSpeed(animatedMaxSpeed)
              val numLines = 4

              for (i in 0..numLines) {
                val fraction = i.toFloat() / numLines
                val yValue = rulerTopValue * fraction
                val yPos = size.height - (yValue / rulerTopValue) * (size.height * GRAPH_HEIGHT_SCALE)

                val (value, unit) = formatBytes(yValue.toLong())

                if (i > 0) {
                  drawContext.canvas.nativeCanvas.drawText(
                    "$value $unit",
                    size.width - 12.dp.toPx(),
                    yPos + 4.dp.toPx(),
                    axisPaint
                  )
                }

                drawLine(
                  color = Color.White.copy(alpha = 0.5f),
                  start = Offset(0f, yPos),
                  end = Offset(10.dp.toPx(), yPos),
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