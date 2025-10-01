package com.vinnovateit.latch.common.util

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import com.vinnovateit.latch.ui.theme.tooltipContainer
import com.vinnovateit.latch.ui.theme.tooltipContent
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * A simple, reliable PopupPositionProvider that ALWAYS places the tooltip
 * below the anchor, adjusting horizontally to stay on screen.
 */
private val SimpleBelowTooltipPositionProvider = object : PopupPositionProvider {
  override fun calculatePosition(
    anchorBounds: IntRect,
    windowSize: IntSize,
    layoutDirection: LayoutDirection,
    popupContentSize: IntSize
  ): IntOffset {
    val screenEdgeMargin = 32
    val verticalMargin = 24

    val centeredX = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
    val y = anchorBounds.bottom + verticalMargin
    val x = centeredX.coerceIn(
      minimumValue = screenEdgeMargin,
      maximumValue = windowSize.width - popupContentSize.width - screenEdgeMargin
    )

    return IntOffset(x, y)
  }
}

/**
 * A wrapper around Material3's TooltipBox that reliably positions the tooltip
 * below the anchor content and provides haptic feedback on long press.
 *
 * @param tooltipText The simple text to be displayed inside the tooltip.
 * @param modifier The modifier to be applied to the TooltipBox.
 * @param content The composable that the tooltip will be anchored to.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipHint(
  tooltipText: String,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  val tooltipState = rememberTooltipState()
  val haptic = LocalHapticFeedback.current

  LaunchedEffect(tooltipState) {
    snapshotFlow { tooltipState.isVisible }
      .distinctUntilChanged()
      .filter { it } // Only when it becomes visible
      .collect {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
      }
  }

  TooltipBox(
    positionProvider = SimpleBelowTooltipPositionProvider,
    tooltip = {
      PlainTooltip(
        containerColor = MaterialTheme.colorScheme.tooltipContainer,
        contentColor = MaterialTheme.colorScheme.tooltipContent
      ) {
        Text(tooltipText)
      }
    },
    state = tooltipState,
    modifier = modifier
  ) {
    content()
  }
}
