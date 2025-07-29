package com.vinnovateit.autonetconnector.screen.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vinnovateit.autonetconnector.functionality.SessionSummary
import com.vinnovateit.autonetconnector.screen.stats.utils.formatBytes
import com.vinnovateit.autonetconnector.screen.stats.utils.formatDurationDynamic
import com.vinnovateit.autonetconnector.ui.theme.GraphDownload
import com.vinnovateit.autonetconnector.ui.theme.GraphUpload
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A Composable that displays a summary of a single past session in a card,
 * with a customizable shape for grouping.
 */
@Composable
fun HistoryItemCard(
  session: SessionSummary,
  shape: Shape = RoundedCornerShape(16.dp) // Default shape
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = shape, // Apply the dynamic shape
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = SimpleDateFormat("E, dd MMM yyyy", Locale.getDefault()).format(Date(session.startTimestamp)),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Duration: ${formatDurationDynamic(session.endTimestamp - session.startTimestamp)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Column(horizontalAlignment = Alignment.End) {
        val (totalValue, totalUnit) = formatBytes(session.totalData.rxBytes + session.totalData.txBytes)
        Text(
          text = "$totalValue $totalUnit",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Row {
          val (dlValue, dlUnit) = formatBytes(session.totalData.rxBytes)
          Icon(Icons.Default.ArrowDownward, contentDescription = "Download", tint = GraphDownload, modifier = Modifier.size(16.dp))
          Text(
            text = "$dlValue $dlUnit",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.width(8.dp))
          val (ulValue, ulUnit) = formatBytes(session.totalData.txBytes)
          Icon(Icons.Default.ArrowUpward, contentDescription = "Upload", tint = GraphUpload, modifier = Modifier.size(16.dp))
          Text(
            text = "$ulValue $ulUnit",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}
