package com.vinnovateit.latch.features.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vinnovateit.latch.domain.model.SessionSummary
import com.vinnovateit.latch.common.util.NoDataCard

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
      Column {
        history.forEachIndexed { index, session ->
          val shape = when {
            history.size == 1 -> RoundedCornerShape(16.dp)
            index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
            index == history.size - 1 -> RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
            else -> RoundedCornerShape(5.dp)
          }
          HistoryItemCard(session = session, shape = shape)
        }
      }
    } else {
      NoDataCard("No session history available.")
    }
  }
}