package com.vinnovateit.latch.features.stats.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vinnovateit.latch.domain.model.LiveConnectionStatus
import com.vinnovateit.latch.domain.model.SessionSummary
import com.vinnovateit.latch.features.stats.DownloadReportButton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatsList(
  modifier: Modifier = Modifier,
  isLive: Boolean,
  showSessionCard: Boolean = true,
  sessionToShow: SessionSummary?,
  historyToShow: List<SessionSummary>,
  liveStatus: LiveConnectionStatus?,
  onDownloadReport: () -> Unit,
  addSpacer: Boolean = false
) {
  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(1.dp), // Spacing updated
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    if(addSpacer){
      item {
        Spacer(modifier = Modifier.height(20.dp))
      }
    }
    if (isLive && sessionToShow != null) {
      // WHEN CONNECTED:
      if (showSessionCard) {
        item {
          SessionCard(
            session = sessionToShow,
          )
          Spacer(modifier = Modifier.height(15.dp)) // Maintain visual separation
        }
      }
      item {
        val liveDownloadBps = liveStatus?.liveData?.lastOrNull()?.usage?.rxBytes ?: 0L
        val liveUploadBps = liveStatus?.liveData?.lastOrNull()?.usage?.txBytes ?: 0L
        LiveSpeedSection(
          isLive = true,
          downloadBps = liveDownloadBps,
          uploadBps = liveUploadBps,
          onDownloadReport = {}
        )
      }
      item {
        HistoryBarChart(history = historyToShow)
        Spacer(modifier = Modifier.height(15.dp)) // Maintain visual separation
      }
    } else {
      // WHEN NOT CONNECTED:
      item {
        val allTimeMaxDownloadBps = historyToShow.maxOfOrNull { it.history.maxOfOrNull { p -> p.usage.rxBytes } ?: 0L } ?: 0L
        val allTimeMaxUploadBps = historyToShow.maxOfOrNull { it.history.maxOfOrNull { p -> p.usage.txBytes } ?: 0L } ?: 0L
        LiveSpeedSection(
          isLive = false,
          downloadBps = allTimeMaxDownloadBps,
          uploadBps = allTimeMaxUploadBps,
          onDownloadReport = {} // Removed button from here
        )
      }
      item {
        HistoryBarChart(history = historyToShow)
        Spacer(modifier = Modifier.height(15.dp)) // Maintain visual separation
      }
    }

    // Integrated HistorySessionList logic
    if (historyToShow.isNotEmpty()) {
      stickyHeader {
        Column(modifier = Modifier
          .padding(vertical = 8.dp)) {
          Text(
            "Sessions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Left,
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 8.dp, start = 8.dp)
          )
        }
      }
      itemsIndexed(historyToShow) { index, session ->
        val shape = when {
          historyToShow.size == 1 -> RoundedCornerShape(16.dp)
          index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
          index == historyToShow.size - 1 -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
          else -> RoundedCornerShape(0.dp)
        }
        StatsItemCard(session = session, shape = shape)
      }
    }

    // Added download button at the end
    if (historyToShow.isNotEmpty()) {
      item {
        Spacer(modifier = Modifier.height(15.dp)) // Maintain visual separation
        DownloadReportButton(onDownloadReport)
      }
    }
    item { Spacer(modifier = Modifier.height(100.dp)) }
  }
}