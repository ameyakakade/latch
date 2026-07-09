package com.vinnovateit.latch.features.home.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinnovateit.latch.R
import com.vinnovateit.latch.domain.model.LiveDataPoint
import com.vinnovateit.latch.domain.model.SessionSummary
import com.vinnovateit.latch.features.wifi.manager.ConnectionStatus
import com.vinnovateit.latch.ui.theme.ModernizFontFamily

@Composable
fun SpectrumCard(
  session: SessionSummary?,
  historyForHomeScreen: List<LiveDataPoint>,
  connectionStatus: ConnectionStatus,
  speedUnit: String,
  isLandscape: Boolean,
  networkSpeed: String,
  onNavigateToStats: () -> Unit = {},
) {
  val topPadding = if (isLandscape) 0.dp else 105.dp
  Card(
    modifier = Modifier
      .padding(top = topPadding)
      .padding(horizontal = 24.dp, vertical = 24.dp)
      .fillMaxSize(),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onNavigateToStats() }
          .padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = stringResource(id = R.string.home_network_statistics),
            fontFamily = ModernizFontFamily,
            color = MaterialTheme.colorScheme.primary,
          )
          Icon(
            imageVector = Icons.Rounded.ArrowOutward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp)
          )
        }
        Text(
          text = networkSpeed,
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = com.vinnovateit.latch.ui.theme.SatoshiFontFamily,
          modifier = Modifier.padding(end = 8.dp)
        )
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        val showGraph = connectionStatus is ConnectionStatus.Idle && session?.history?.isNotEmpty() == true

        AnimatedContent(
          modifier = Modifier.fillMaxSize(),
          targetState = showGraph,
          transitionSpec = { fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500)) },
          label = "GraphVsStatus"
        ) { isGraphVisible ->
          if (isGraphVisible) {
            HomeScreenGraph(
              modifier = Modifier.fillMaxSize(),
              rateHistory = historyForHomeScreen,
              speedUnit = speedUnit
            )
          } else {
            StatusIndicator(connectionStatus = connectionStatus)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalAnimationApi::class)
@Composable
private fun StatusIndicator(connectionStatus: ConnectionStatus) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .padding(16.dp)
        .animateContentSize()
    ) {
      AnimatedVisibility(
        visible = connectionStatus !is ConnectionStatus.Idle,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          AnimatedContent(
            targetState = connectionStatus,
            transitionSpec = { 
                (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f, animationSpec = tween(300))) togetherWith (fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f, animationSpec = tween(300)))
            },
            label = "IconAnimation"
          ) { status ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(92.dp)) {
              when (status) {
                is ConnectionStatus.Companion.Connecting -> LoadingIndicator(
                  modifier = Modifier
                    .size(92.dp)
                    .graphicsLayer { alpha = 0.35f }
                )
                is ConnectionStatus.Success -> Icon(
                  imageVector = Icons.Rounded.Wifi,
                  contentDescription = stringResource(R.string.status_connected),
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(64.dp)
                )
                is ConnectionStatus.Failed -> {
                    val isUnsupported = status.message.equals(stringResource(R.string.status_unsupported_network), ignoreCase = true)
                    Icon(
                      imageVector = if (isUnsupported) Icons.Rounded.QuestionMark else Icons.Rounded.Error,
                      contentDescription = stringResource(R.string.status_login_failed),
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(64.dp)
                    )
                }
                else -> {}
              }
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
        }
      }

      AnimatedContent(
        targetState = connectionStatus,
        transitionSpec = {
          (fadeIn(tween(300)) + scaleIn(initialScale = 0.9f, animationSpec = tween(300))) togetherWith (fadeOut(tween(300)) + scaleOut(targetScale = 0.9f, animationSpec = tween(300)))
        },
        label = "TextAnimation"
      ) { status ->
        Text(
          text = when (status) {
            is ConnectionStatus.Idle -> stringResource(R.string.home_no_data_for_graph)
            is ConnectionStatus.Companion.Connecting -> status.message.replace(".", "")
            is ConnectionStatus.Success -> stringResource(R.string.status_connected).replace(".", "")
            is ConnectionStatus.Failed -> status.message.replace(".", "")
          },
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          textAlign = TextAlign.Center
        )
      }
    }
}