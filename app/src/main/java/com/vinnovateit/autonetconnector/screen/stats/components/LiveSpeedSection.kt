package com.vinnovateit.autonetconnector.screen.stats.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vinnovateit.autonetconnector.functionality2.manager.SessionSummary
import com.vinnovateit.autonetconnector.screen.stats.utils.formatBitsPerSecond
import com.vinnovateit.autonetconnector.ui.components.TooltipHint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveSpeedSection(session: SessionSummary?) {
  // Calculate the maximum download and upload speeds from the session history.
  val maxDownloadBps = session?.history?.maxOfOrNull { it.usage.rxBytes } ?: 0L
  val maxUploadBps = session?.history?.maxOfOrNull { it.usage.txBytes } ?: 0L

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(24.dp)
  ) {
    // Download Report Button
    TooltipHint(tooltipText = "Download a CSV report of the session") {
      OutlinedButton(
        onClick = { /* TODO: Implement report generation logic */ },
        shape = RoundedCornerShape(50),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        colors = ButtonDefaults.outlinedButtonColors(
          contentColor = MaterialTheme.colorScheme.error
        )
      ) {
        Icon(
          imageVector = Icons.Default.Download,
          contentDescription = "Download Report"
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Download report", fontWeight = FontWeight.Bold)
      }
    }

    // Max Speeds
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      SpeedIndicator(label = "Max Download", bytesPerSecond = maxDownloadBps)
      VerticalDivider(
        modifier = Modifier
          .height(80.dp)
          .width(1.dp),
        color = MaterialTheme.colorScheme.outline
      )
      SpeedIndicator(label = "Max Upload", bytesPerSecond = maxUploadBps)
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
