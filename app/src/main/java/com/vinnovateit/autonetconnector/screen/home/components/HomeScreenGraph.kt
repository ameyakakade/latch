package com.vinnovateit.autonetconnector.screen.home.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.vinnovateit.autonetconnector.functionality.LiveDataPoint
import com.vinnovateit.autonetconnector.screen.stats.utils.createGraphPaths
import kotlin.math.max

private const val GRAPH_HEIGHT_SCALE = 0.7f

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun HomeScreenGraph(
  modifier: Modifier = Modifier,
  rateHistory: List<LiveDataPoint>,
) {
  BoxWithConstraints(modifier = modifier) {
    // This graph is a non-interactive ticker. It shows the last 60 data points.
    val pointsToShow = rateHistory.takeLast(60)

    val maxSpeed = remember(pointsToShow) {
      val maxRx = pointsToShow.maxOfOrNull { it.usage.rxBytes } ?: 0L
      val maxTx = pointsToShow.maxOfOrNull { it.usage.txBytes } ?: 0L
      max(maxRx, maxTx)
    }

    // Animate the max speed for smoother transitions when the peak usage changes.
    val animatedMaxSpeed by animateFloatAsState(
      targetValue = maxSpeed.toFloat(),
      animationSpec = tween(500),
      label = "MaxSpeedAnimation"
    )

    val density = LocalDensity.current
    val graphData by remember(pointsToShow, maxWidth, maxHeight, animatedMaxSpeed) {
      derivedStateOf {
        val graphWidthPx = with(density) { maxWidth.toPx() }
        val graphHeightPx = with(density) { maxHeight.toPx() }
        createGraphPaths(pointsToShow, graphWidthPx, graphHeightPx, animatedMaxSpeed.coerceAtLeast(1f), GRAPH_HEIGHT_SCALE)
      }
    }

    val dlColor = Color(0xFF0089D0)
    val ulColor = Color(0xFFFFA500)

    Canvas(modifier = Modifier.fillMaxSize()) {
      val dlBrush = Brush.verticalGradient(listOf(dlColor.copy(0.4f), Color.Transparent), endY = size.height * GRAPH_HEIGHT_SCALE)
      val ulBrush = Brush.verticalGradient(listOf(ulColor.copy(0.4f), Color.Transparent), endY = size.height * GRAPH_HEIGHT_SCALE)

      drawPath(graphData.downloadPath, brush = dlBrush)
      drawPath(graphData.uploadPath, brush = ulBrush)
      drawPath(graphData.lineDownloadPath, dlColor, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
      drawPath(graphData.lineUploadPath, ulColor, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
    }
  }
}
