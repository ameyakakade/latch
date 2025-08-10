package com.vinnovateit.latch.features.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NoDataPlaceholder(
  modifier: Modifier = Modifier,
  messageRes: String
) {
  MaterialExpressiveTheme(
    motionScheme = MotionScheme.expressive()
  ) {
    Box(
      modifier = modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
      ) {
        LoadingIndicator(
          modifier = Modifier
            .size(92.dp)
            .graphicsLayer { alpha = 0.35f }
        )
        Text(
          text = messageRes,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))
      }
    }
  }
}
