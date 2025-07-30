package com.vinnovateit.autonetconnector.screen.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vinnovateit.autonetconnector.functionality2.manager.SessionSummary
import com.vinnovateit.autonetconnector.screen.stats.ui.NoDataCard

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
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        history.forEachIndexed { index, session ->
          // Determine the shape based on the item's position in the single list
          val shape = when {
            history.size == 1 -> RoundedCornerShape(16.dp) // Single item in the list
            index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp) // First item
            index == history.size - 1 -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp) // Last item
            else -> RoundedCornerShape(0.dp) // Middle items
          }
          HistoryItemCard(session = session, shape = shape)
        }
      }
    } else {
      NoDataCard("No session history available.")
    }
  }
}