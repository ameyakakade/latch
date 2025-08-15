package com.vinnovateit.latch.features.stats.components

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinnovateit.latch.domain.model.LiveConnectionStatus
import com.vinnovateit.latch.domain.model.SessionSummary
import com.vinnovateit.latch.features.stats.DownloadReportButton
import com.vinnovateit.latch.features.stats.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatsList(
  modifier: Modifier = Modifier,
  isLive: Boolean,
  showSessionCard: Boolean = true,
  sessionToShow: SessionSummary?,
  historyToShow: List<SessionSummary>,
  liveStatus: LiveConnectionStatus?,
  speedUnits: String,
  onDownloadReport: () -> Unit,
  addSpacer: Boolean = false,
  statsViewModel: StatsViewModel
) {
  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
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
            speedUnit = speedUnits
          )
          Spacer(modifier = Modifier.height(15.dp))
        }
      }
      item {
        val liveDownloadBps = liveStatus?.liveData?.lastOrNull()?.usage?.rxBytes ?: 0L
        val liveUploadBps = liveStatus?.liveData?.lastOrNull()?.usage?.txBytes ?: 0L
        LiveSpeedSection(
          isLive = true,
          downloadBps = liveDownloadBps,
          uploadBps = liveUploadBps,
          speedUnit = speedUnits,
        )
      }
      item {
        val chartItems by statsViewModel.chartItems.collectAsStateWithLifecycle()
        HistoryBarChart(history = chartItems)
        Spacer(modifier = Modifier.height(15.dp))
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
          speedUnit = speedUnits,
        )
      }
      item {
        val chartItems by statsViewModel.chartItems.collectAsStateWithLifecycle()
        HistoryBarChart(history = chartItems)
        Spacer(modifier = Modifier.height(15.dp))
      }
    }

    if (historyToShow.isNotEmpty()) {
      item {
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
        val cornerRadius = 24.dp

        val shape = when {
          // Case 1: There is only ONE item in the list.
          // All four corners should be rounded.
          historyToShow.size == 1 -> RoundedCornerShape(cornerRadius)

          // Case 2: This is the FIRST item in a multi-item list.
          // Only the top-left and top-right corners are rounded.
          index == 0 -> RoundedCornerShape(
            topStart = cornerRadius,
            topEnd = cornerRadius,
            bottomStart = 5.dp,
            bottomEnd = 5.dp
          )

          // Case 3: This is the LAST item in a multi-item list.
          // Only the bottom-left and bottom-right corners are rounded.
          index == historyToShow.size - 1 -> RoundedCornerShape(
            topStart = 5.dp,
            topEnd = 5.dp,
            bottomStart = cornerRadius,
            bottomEnd = cornerRadius
          )

          // Case 4: This is any item in the MIDDLE.
          // No corners are rounded.
          else -> RoundedCornerShape(5.dp)
        }
        if(!isLive || index > 0) {
          StatsItemCard(session = session, shape = shape, speedUnit = speedUnits)
        }
      }
    }

    if (historyToShow.isNotEmpty()) {
      item {
        Spacer(modifier = Modifier.height(15.dp))
        DownloadReportButton(onDownloadReport)
      }
    }
    item { Spacer(modifier = Modifier.height(100.dp)) }
  }
}