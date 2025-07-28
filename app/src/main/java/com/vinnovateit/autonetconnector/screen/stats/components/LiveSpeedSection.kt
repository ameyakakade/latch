package com.vinnovateit.autonetconnector.screen.stats.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vinnovateit.autonetconnector.functionality.SessionSummary
import com.vinnovateit.autonetconnector.screen.stats.utils.formatBitsPerSecond
import com.vinnovateit.autonetconnector.ui.theme.DownloadReportButton
import com.vinnovateit.autonetconnector.ui.theme.TextOnDanger

@Composable
fun LiveSpeedSection(session: SessionSummary?) {
  val latestDataPoint = session?.history?.lastOrNull()

  // The polling interval is 2 seconds, so we divide by 2 to get bytes/sec
  val downloadBps = latestDataPoint?.usage?.rxBytes ?: 0L
  val uploadBps = latestDataPoint?.usage?.txBytes ?: 0L

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(24.dp)
  ) {
    // Download Report Button
    Button(
      onClick = { /* TODO: Implement report generation logic */ },
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(containerColor = DownloadReportButton),
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Download,
        contentDescription = "Download Report",
        tint = TextOnDanger
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text("Download report", color = TextOnDanger, fontWeight = FontWeight.Bold)
    }

    // Live Speeds
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      SpeedIndicator(label = "Download", bytesPerSecond = downloadBps)
      VerticalDivider(
        modifier = Modifier
          .height(80.dp) // Give the divider a fixed height
          .width(1.dp),
        color = MaterialTheme.colorScheme.outline
      )
      SpeedIndicator(label = "Upload", bytesPerSecond = uploadBps)
    }
  }
}

@Composable
fun SpeedIndicator(label: String, bytesPerSecond: Long) {
  val (value, unit) = formatBitsPerSecond(bytesPerSecond)

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    RollingNumberText(
      value = value,
      textStyle = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
    )
    Text(
      text = unit,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
fun RollingNumberText(
  value: String,
  textStyle: androidx.compose.ui.text.TextStyle
) {
  var previousValue by remember { mutableStateOf<String?>(null) }

  SideEffect {
    previousValue = value
  }

  Row(
    horizontalArrangement = Arrangement.Center
  ) {
    val previousChars = previousValue?.padStart(value.length, ' ')?.toCharArray() ?: " ".repeat(value.length).toCharArray()
    val currentChars = value.padStart(previousValue?.length ?: value.length, ' ').toCharArray()

    for (i in currentChars.indices) {
      val oldChar = previousChars.getOrNull(i)
      val newChar = currentChars[i]

      AnimatedContent(
        targetState = newChar,
        transitionSpec = {
          if (oldChar != null && newChar.isDigit() && oldChar.isDigit()) {
            if (newChar > oldChar) {
              slideInVertically { it } togetherWith slideOutVertically { -it }
            } else {
              slideInVertically { -it } togetherWith slideOutVertically { it }
            }
          } else {
            fadeIn() togetherWith fadeOut()
          }
        },
        label = "char_animation"
      ) { char ->
        Text(
          text = char.toString(),
          style = textStyle,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}
