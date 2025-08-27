package com.vinnovateit.latch.features.home.components

import android.annotation.SuppressLint
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.padding
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
import com.vinnovateit.latch.common.util.createGraphPaths
import com.vinnovateit.latch.common.util.formatBitsPerSecond
import com.vinnovateit.latch.domain.model.LiveDataPoint
import com.vinnovateit.latch.ui.theme.ColorGraphDownload
import com.vinnovateit.latch.ui.theme.ColorGraphUpload
import com.vinnovateit.latch.ui.theme.ColorTransparent
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlinx.coroutines.delay

const val GRAPH_HEIGHT_SCALE = 0.77f
val Y_AXIS_WIDTH = 70.dp
const val POINTS_IN_30_SECONDS = 20

/**
 * Calculates a "nice" rounded number for the top of the Y-axis.
 */
fun calculateNiceMaxSpeed(maxSpeed: Float): Float {
  if (maxSpeed <= 0f) return 1f
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
  rateHistory: List<LiveDataPoint>,
  speedUnit: String
) {
  val initialScale = if (rateHistory.size > POINTS_IN_30_SECONDS) {
    rateHistory.size.toFloat() / POINTS_IN_30_SECONDS.toFloat()
  } else {
    1f
  }
  var scale by remember { mutableStateOf(initialScale) }
  val scrollState = rememberScrollState()
  var lastInteraction by remember { mutableLongStateOf(0L) }
  var yAxisVisible by remember { mutableStateOf(true) }
  var isAutoScrolling by remember { mutableStateOf(false) }

  // Auto-scroll logic when new data arrives and user is idle
  LaunchedEffect(rateHistory.size) {
    val idleDuration = System.currentTimeMillis() - lastInteraction
    if (lastInteraction == 0L || idleDuration > 5000L) { // Autoscroll on first load or after being idle
      isAutoScrolling = true
      scrollState.animateScrollTo(
        scrollState.maxValue,
        animationSpec = tween(durationMillis = 500)
      )
      isAutoScrolling = false
    }
  }

  // Timer for Y-axis visibility and scale reset
  LaunchedEffect(lastInteraction) {
    if (lastInteraction == 0L) return@LaunchedEffect // Don't run on initial composition

    yAxisVisible = true
    delay(5000L)
    // Check again after delay in case of new interaction
    if (System.currentTimeMillis() - lastInteraction >= 5000L) {
      yAxisVisible = false
      if (scale != initialScale) {
        animate(initialValue = scale, targetValue = initialScale) { value, _ ->
          scale = value
        }
      }
    }
  }


  Box(
    modifier = modifier
      .pointerInput(Unit) {
        detectTransformGestures { _, _, zoom, _ ->
          scale = (scale * zoom).coerceIn(1f, initialScale * 3) // Allow zooming in further, but not out past 1:1
          lastInteraction = System.currentTimeMillis() // Update interaction time on zoom
        }
      }
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(bottom = 16.dp)) {
      val containerWidthPx = constraints.maxWidth
      val containerHeightPx = constraints.maxHeight

      // Detect user interaction on scroll
      LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress && !isAutoScrolling) {
          lastInteraction = System.currentTimeMillis()
        }
      }

      if (containerWidthPx > 0 && rateHistory.size > 1) {

        var maxSpeed by remember { mutableLongStateOf(1L) }

        // Recalculate max speed only when scrolling stops or data/zoom changes.
        // This prevents expensive recalculations during scroll animations.
        LaunchedEffect(rateHistory, scrollState.isScrollInProgress, scale) {
          if (rateHistory.size < 2 || scrollState.isScrollInProgress) return@LaunchedEffect

          val totalGraphWidthPx = containerWidthPx * scale
          val firstTimestamp = rateHistory.first().timestamp
          val totalDuration = (rateHistory.last().timestamp - firstTimestamp).coerceAtLeast(1)

          val visibleStartRatio = scrollState.value / totalGraphWidthPx
          val visibleEndRatio = (scrollState.value + containerWidthPx) / totalGraphWidthPx

          val visibleStartTime = firstTimestamp + (totalDuration * visibleStartRatio).toLong()
          val visibleEndTime = firstTimestamp + (totalDuration * visibleEndRatio).toLong()

          val visiblePoints = rateHistory.filter { it.timestamp in visibleStartTime..visibleEndTime }

          maxSpeed = if (visiblePoints.isEmpty()) {
            1L
          } else {
            val maxRx = visiblePoints.maxOfOrNull { it.usage.rxBytes } ?: 0L
            val maxTx = visiblePoints.maxOfOrNull { it.usage.txBytes } ?: 0L
            max(maxRx, maxTx)
          }
        }

        val animatedMaxSpeed by animateFloatAsState(
          targetValue = maxSpeed.toFloat(),
          animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
          ),
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
              val dlBrush = Brush.verticalGradient(listOf(ColorGraphDownload.copy(0.4f), ColorTransparent))
              val ulBrush = Brush.verticalGradient(listOf(ColorGraphUpload.copy(0.4f), ColorTransparent))

              drawPath(graphData.downloadPath, brush = dlBrush)
              drawPath(graphData.uploadPath, brush = ulBrush)
              drawPath(graphData.lineDownloadPath, ColorGraphDownload, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
              drawPath(graphData.lineUploadPath, ColorGraphUpload, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
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
                      ColorTransparent
                    )
                  )
                )
            ) {
              val animatedFontWeight by animateFloatAsState(
                targetValue = if (yAxisVisible) 700f else 400f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
              )
              Canvas(modifier = Modifier.fillMaxSize()) {
                val axisPaint = Paint().apply {
                  color = onBackgroundColor.copy(alpha = 0.7f).toArgb()
                  textAlign = Paint.Align.LEFT
                  textSize = 12.sp.toPx()
                  typeface = Typeface.create(Typeface.DEFAULT, animatedFontWeight.toInt())
                }
                val rulerTopValue = calculateNiceMaxSpeed(animatedMaxSpeed / 2)
                val numLines = 4

                for (i in 0..numLines) {
                  val fraction = i.toFloat() / numLines
                  val yValue = rulerTopValue * fraction
                  val yPos = size.height - ((yValue / rulerTopValue) * (size.height * GRAPH_HEIGHT_SCALE))
                  val (value, unit) = formatBitsPerSecond(yValue.toLong(), speedUnit)
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