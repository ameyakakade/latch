package com.vinnovateit.latch.features.stats

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vinnovateit.latch.R
import com.vinnovateit.latch.common.util.generateCsvReport
import com.vinnovateit.latch.common.util.TooltipHint
import com.vinnovateit.latch.domain.model.LiveConnectionStatus
import com.vinnovateit.latch.domain.model.SessionSummary
import com.vinnovateit.latch.features.stats.components.HistoryBarChart
import com.vinnovateit.latch.features.stats.components.HistoryItemCard
import com.vinnovateit.latch.features.stats.components.HistorySessionList
import com.vinnovateit.latch.features.stats.components.LiveSpeedSection
import com.vinnovateit.latch.features.stats.components.SessionCard
import com.vinnovateit.latch.ui.theme.LatchTheme
import com.vinnovateit.latch.ui.theme.ModernizFontFamily

class StatsActivity : ComponentActivity() {
  private val createDocumentLauncher =
    registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
      uri?.let {
        try {
          val viewModel: StatsViewModel by viewModels()
          contentResolver.openOutputStream(it)?.use { outputStream ->
            generateCsvReport(viewModel.sessionHistory.value, outputStream)
          }
          Toast.makeText(this, "Report saved successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
          Toast.makeText(this, "Failed to save report: $e", Toast.LENGTH_SHORT).show()
        }
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      LatchTheme {
        StatsScreen(
          onDownloadReport = {
            try {
              createDocumentLauncher.launch("session_report.csv")
            } catch (e: ActivityNotFoundException) {
              Toast.makeText(this, "No app found to create CSV files. $e", Toast.LENGTH_LONG).show()
            }
          }
        )
      }
    }
  }
}

/**
 * Main screen composable, responsible for the overall layout (Scaffold) and state management for dialogs.
 */
@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
  modifier: Modifier = Modifier,
  statsViewModel: StatsViewModel = viewModel(),
  onDownloadReport: () -> Unit
) {
  val context = LocalContext.current as Activity
  val sessionToShow by statsViewModel.sessionToShow.collectAsStateWithLifecycle()
  val historyToShow by statsViewModel.sessionHistory.collectAsStateWithLifecycle()
  val liveStatus by statsViewModel.liveStatus.collectAsStateWithLifecycle()
  val isLive = remember(liveStatus) { liveStatus != null }

  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
    topBar = {
      StatsTopAppBar(
        scrollBehavior = scrollBehavior,
        onBackClick = { context.finish() }
      )
    }
  ) { innerPadding ->
    StatsScreenContent(
      modifier = Modifier.padding(innerPadding),
      isLive = isLive,
      sessionToShow = sessionToShow,
      historyToShow = historyToShow,
      liveStatus = liveStatus,
      onDownloadReport = onDownloadReport
    )
  }
}

/**
 * Handles the content of the screen, deciding whether to show the stats list or an empty state message.
 */
@Composable
private fun StatsScreenContent(
  modifier: Modifier = Modifier,
  isLive: Boolean,
  sessionToShow: SessionSummary?,
  historyToShow: List<SessionSummary>,
  liveStatus: LiveConnectionStatus?,
  onDownloadReport: () -> Unit
) {
  if (!isLive && historyToShow.isEmpty()) {
    EmptyStatsView(modifier = modifier.fillMaxSize())
  } else {
    StatsList(
      modifier = modifier.fillMaxSize(),
      isLive = isLive,
      sessionToShow = sessionToShow,
      historyToShow = historyToShow,
      liveStatus = liveStatus,
      onDownloadReport = onDownloadReport
    )
  }
}

@Composable
private fun StatsList(
  modifier: Modifier = Modifier,
  isLive: Boolean,
  sessionToShow: SessionSummary?,
  historyToShow: List<SessionSummary>,
  liveStatus: LiveConnectionStatus?,
  onDownloadReport: () -> Unit
) {
  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    if (isLive && sessionToShow != null) {
      // WHEN CONNECTED:
      item {
        SessionCard(
          session = sessionToShow,
        )
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
      item { HistoryBarChart(history = historyToShow) }
      item {
        HistorySessionList(
          history = historyToShow
        )
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
      item { HistoryBarChart(history = historyToShow) }
      item {
        HistorySessionList(history = historyToShow)
      }
    }
    // Added download button at the end
    if (historyToShow.isNotEmpty()) {
      item {
        TooltipHint(tooltipText = "Download a CSV report of all sessions") {
          OutlinedButton(
            onClick = onDownloadReport,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Download,
              contentDescription = "Download Report"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Download Usage Report", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
    item { Spacer(modifier = Modifier.height(100.dp)) }
  }
}

/**
 * A composable for the top app bar of the stats screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsTopAppBar(
  scrollBehavior: TopAppBarScrollBehavior,
  onBackClick: () -> Unit,
) {
  LargeTopAppBar(
    title = {
      Text(
        "Stats",
        fontSize = 23.sp,
        maxLines = 1,
        fontFamily = ModernizFontFamily,
        overflow = TextOverflow.Ellipsis
      )
    },
    navigationIcon = {
      TooltipHint(tooltipText = stringResource(R.string.stats_go_back)) {
        IconButton(onClick = onBackClick) {
          Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.surface,
      scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
      navigationIconContentColor = Color.Unspecified,
      titleContentColor = Color.Unspecified,
      actionIconContentColor = Color.Unspecified
    ),
    scrollBehavior = scrollBehavior
  )
}

/**
 * A composable shown when there is no stats history to display.
 */
@Composable
private fun EmptyStatsView(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = Icons.Rounded.BarChart,
        contentDescription = "Empty Stats Icon",
        modifier = Modifier.size(128.dp),
        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "No stats to show yet. Connect to a network to get started!",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 32.dp)
      )
    }
  }
}