package com.vinnovateit.autonetconnector.features.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vinnovateit.autonetconnector.domain.model.SessionSummary
import com.vinnovateit.autonetconnector.common.util.NoDataCard

@Composable
fun HistorySessionList(history: List<SessionSummary>) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      "Sessions",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground,
      textAlign = TextAlign.Left,
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp, start = 8.dp)
    )

    if (history.isNotEmpty()) {
      // Using a simple Column as the parent is a LazyColumn.
      // This prevents nested scrolling issues and is efficient for a reasonable number of sessions.
      LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        itemsIndexed(history) { index, session ->
          val shape = when {
            history.size == 1 -> MaterialTheme.shapes.medium
            index == 0 -> MaterialTheme.shapes.medium.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
            index == history.size - 1 -> MaterialTheme.shapes.medium.copy(topStart = CornerSize(0.dp), topEnd = CornerSize(0.dp))
            else -> MaterialTheme.shapes.small
          }
          HistoryItemCard(session = session, shape = shape)
        }
      }

    } else {
      NoDataCard("No session history available.")
    }
  }
}