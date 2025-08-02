package com.vinnovateit.autonetconnector

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vinnovateit.autonetconnector.functionality2.manager.LiveConnectionStatus
import com.vinnovateit.autonetconnector.functionality2.manager.SessionSummary
import com.vinnovateit.autonetconnector.functionality2.manager.StatsViewModel
import com.vinnovateit.autonetconnector.screen.stats.components.HistoryBarChart
import com.vinnovateit.autonetconnector.screen.stats.components.HistorySessionList
import com.vinnovateit.autonetconnector.screen.stats.components.LiveSpeedSection
import com.vinnovateit.autonetconnector.screen.stats.components.SessionCard
import com.vinnovateit.autonetconnector.screen.stats.utils.generateCsvReport
import com.vinnovateit.autonetconnector.ui.components.TooltipHint
import com.vinnovateit.autonetconnector.ui.theme.AutoNetConnectorTheme

class StatsActivity : ComponentActivity() {
  private var currentSsid: String? = null

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
    currentSsid = intent.getStringExtra("CURRENT_SSID")
    setContent {
      AutoNetConnectorTheme {
        StatsScreen(
          overrideSsid = currentSsid,
          onDownloadReport = {
            try {
              createDocumentLauncher.launch("session_report.csv")
            } catch (e: ActivityNotFoundException) {
              Toast.makeText(this, "No app found to create CSV files.", Toast.LENGTH_LONG).show()
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
  overrideSsid: String?,
  onDownloadReport: () -> Unit
) {
  var showResetDialog by remember { mutableStateOf(false) }
  val context = LocalContext.current as Activity

  val sessionToShow by statsViewModel.sessionToShow.collectAsStateWithLifecycle()
  val historyToShow by statsViewModel.sessionHistory.collectAsStateWithLifecycle()
  val liveStatus by statsViewModel.liveStatus.collectAsStateWithLifecycle()
  val isLive = remember(liveStatus) { liveStatus != null }

  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      StatsTopAppBar(
        scrollBehavior = scrollBehavior,
        onBackClick = { context.finish() },
        onResetClick = { showResetDialog = true }
      )
    }
  ) { innerPadding ->
    StatsScreenContent(
      modifier = Modifier.padding(innerPadding),
      isLive = isLive,
      sessionToShow = sessionToShow,
      historyToShow = historyToShow,
      liveStatus = liveStatus,
      overrideSsid = overrideSsid,
      onDownloadReport = onDownloadReport
    )
  }

  if (showResetDialog) {
    ResetStatsDialog(
      onDismiss = { showResetDialog = false },
      onConfirm = {
        statsViewModel.onClearHistory()
        showResetDialog = false
      }
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
  overrideSsid: String?,
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
      overrideSsid = overrideSsid,
      onDownloadReport = onDownloadReport
    )
  }
}

/**
 * The main list of stats items, displayed in a LazyColumn.
 * The order of items changes based on the connection status.
 */
@Composable
private fun StatsList(
  modifier: Modifier = Modifier,
  isLive: Boolean,
  sessionToShow: SessionSummary?,
  historyToShow: List<SessionSummary>,
  liveStatus: LiveConnectionStatus?,
  overrideSsid: String?,
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
      item { SessionCard(session = sessionToShow, overrideSsid = overrideSsid) }
      item {
        val liveDownloadBps = liveStatus?.liveData?.lastOrNull()?.usage?.rxBytes ?: 0L
        val liveUploadBps = liveStatus?.liveData?.lastOrNull()?.usage?.txBytes ?: 0L
        LiveSpeedSection(
          isLive = true,
          downloadBps = liveDownloadBps,
          uploadBps = liveUploadBps,
          onDownloadReport = onDownloadReport
        )
      }
      item { HistoryBarChart(history = historyToShow) }
      item { HistorySessionList(history = historyToShow) }
    } else {
      // WHEN NOT CONNECTED:
      item {
        val allTimeMaxDownloadBps = historyToShow.maxOfOrNull { it.history.maxOfOrNull { p -> p.usage.rxBytes } ?: 0L } ?: 0L
        val allTimeMaxUploadBps = historyToShow.maxOfOrNull { it.history.maxOfOrNull { p -> p.usage.txBytes } ?: 0L } ?: 0L
        LiveSpeedSection(
          isLive = false,
          downloadBps = allTimeMaxDownloadBps,
          uploadBps = allTimeMaxUploadBps,
          onDownloadReport = onDownloadReport
        )
      }
      item { HistoryBarChart(history = historyToShow) }
      item { HistorySessionList(history = historyToShow) }
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
  onResetClick: () -> Unit
) {
  LargeTopAppBar(
    title = {
      Text(
        text = "Network Statistics",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
      )
    },
    navigationIcon = {
      TooltipHint(tooltipText = "Go Back") {
        IconButton(onClick = onBackClick) {
          Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
      }
    },
    actions = {
      TooltipHint(tooltipText = "Reset Stats") {
        IconButton(onClick = onResetClick) {
          Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Stats")
        }
      }
    },
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

/**
 * A composable for the dialog that asks for confirmation before resetting stats.
 */
@Composable
private fun ResetStatsDialog(
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Reset Stats") },
    text = { Text("Are you sure you want to delete all session history? This action cannot be undone.") },
    confirmButton = {
      TextButton(onClick = onConfirm) { Text("Reset") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}
