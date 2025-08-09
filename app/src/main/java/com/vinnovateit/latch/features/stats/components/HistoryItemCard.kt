package com.vinnovateit.latch.features.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vinnovateit.latch.domain.model.SessionSummary
import com.vinnovateit.latch.common.util.formatBytes
import com.vinnovateit.latch.common.util.formatDurationDynamic
import com.vinnovateit.latch.ui.theme.ColorGraphDownload
import com.vinnovateit.latch.ui.theme.ColorGraphUpload
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
  shape: Shape = RoundedCornerShape(16.dp)
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = shape,
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
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
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = formatDurationDynamic(session.endTimestamp - session.startTimestamp),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Column(horizontalAlignment = Alignment.End) {
        val total = remember(session.totalData) { formatBytes(session.totalData.rxBytes + session.totalData.txBytes) }
        Text(
          text = "${total.first} ${total.second}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Row {
          val dl = remember(session.totalData) { formatBytes(session.totalData.rxBytes) }
          Icon(Icons.Default.ArrowDownward, contentDescription = "Download data", tint = ColorGraphDownload, modifier = Modifier.size(16.dp))
          Text(
            text = "${dl.first} ${dl.second}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.width(12.dp))
          val ul = remember(session.totalData) { formatBytes(session.totalData.txBytes) }
          Icon(Icons.Default.ArrowUpward, contentDescription = "Upload data", tint = ColorGraphUpload, modifier = Modifier.size(16.dp))
          Text(
            text = "${ul.first} ${ul.second}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}
