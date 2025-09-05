package com.vinnovateit.latch.common.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.vinnovateit.latch.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LeafOverlay(
  modifier: Modifier = Modifier,
  contentDescription: String?,
  alignment: Alignment = Alignment.Center,
  contentScale: ContentScale = ContentScale.None
) {
  val primaryColor = MaterialTheme.colorScheme.primary

  val themedVector = remember(primaryColor) {
    Builder(
      name = "LeafOverlay",
      defaultWidth = 402.dp,
      defaultHeight = 456.dp,
      viewportWidth = 402f,
      viewportHeight = 456f
    ).apply {
      addPath(
        fill = SolidColor(primaryColor),
        fillAlpha = 0.085f,
        pathData = PathParser().parsePathString("M190.48,418.69C121.88,394.11 66.58,358.72 21.53,318.42C-65.51,240.76 -112.61,143.45 -138.21,66.78C-97.25,133.62 -39.91,203.41 38.94,261.41C141.33,336.11 281.61,388.21 469,381.33C467.98,394.11 469,405.91 469,418.69C469,430.48 469,442.28 467.98,454.07C357.39,459.97 265.23,446.21 190.48,418.69Z").toNodes()
      )
      addPath(
        fill = SolidColor(primaryColor),
        fillAlpha = 0.085f,
        pathData = PathParser().parsePathString("M9.52,197.87C-67.27,129.06 -113.35,45.5 -141,-26.25C-134.86,-43.95 -128.71,-59.68 -124.62,-75.4C-97.99,-39.03 -70.35,-6.6 -43.72,21.91C-11.98,54.35 17.71,66.15 34.1,72.04C147.76,110.38 229.68,63.2 229.68,63.2C182.57,126.11 127.28,137.9 85.3,135.94C230.7,238.17 340.26,233.26 340.26,233.26C340.26,233.26 317.73,-43.95 -121.54,-175.67C177.45,-164.86 421.15,52.38 461.09,332.54C248.11,345.32 104.75,283.39 9.52,197.87Z").toNodes()
      )
    }.build()
  }
  Image(
    imageVector = themedVector,
    contentDescription = contentDescription,
    modifier = modifier,
    alignment = alignment,
    contentScale = contentScale)
}

@Composable
fun HandsConnectAnimation(
  modifier: Modifier = Modifier,
  leftRes: Int = R.drawable.ic_hand_left,
  rightRes: Int = R.drawable.ic_hand_right,
  sizeDp: Dp = 140.dp,
  durationMs: Int = 500 // total animation time 500ms
) {

  // initial positions (off corners)
  val leftOffsetX = remember { Animatable(-200f) } // top-left X
  val leftOffsetY = remember { Animatable(-200f) } // top-left Y
  val rightOffsetX = remember { Animatable(200f) } // bottom-right X
  val rightOffsetY = remember { Animatable(200f) } // bottom-right Y

  LaunchedEffect(Unit) {
    val spec = tween<Float>(durationMillis = durationMs, easing = FastOutSlowInEasing)

    launch { leftOffsetX.animateTo(0f, spec) }
    launch { leftOffsetY.animateTo(0f, spec) }
    launch { rightOffsetX.animateTo(0f, spec) }
    launch { rightOffsetY.animateTo(0f, spec) }
  }

  Box(
    modifier = modifier.fillMaxSize().offset(y = -50.dp),
    contentAlignment = Alignment.Center
  ) {
    // right hand on top
    Image(
      painter = painterResource(id = rightRes),
      contentDescription = null,
      modifier = Modifier
        .size(sizeDp)
        .offset {
          IntOffset(
            rightOffsetX.value.roundToInt(),
            rightOffsetY.value.roundToInt()
          )
        },
      contentScale = ContentScale.Fit
    )

    Image(
      painter = painterResource(id = leftRes),
      contentDescription = null,
      modifier = Modifier
        .size(sizeDp)
        .offset {
          IntOffset(
            leftOffsetX.value.roundToInt(),
            leftOffsetY.value.roundToInt()
          )
        },
      contentScale = ContentScale.Fit
    )
  }
}
