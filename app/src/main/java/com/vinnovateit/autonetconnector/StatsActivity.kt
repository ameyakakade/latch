package com.vinnovateit.autonetconnector

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.vinnovateit.autonetconnector.functionality2.StatsViewModel
import com.vinnovateit.autonetconnector.functionality2.StatsViewModelFactory
import com.vinnovateit.autonetconnector.screen.stats.components.HistoryItemCard
import com.vinnovateit.autonetconnector.screen.stats.components.HistorySection
import com.vinnovateit.autonetconnector.screen.stats.components.LiveSpeedSection
import com.vinnovateit.autonetconnector.screen.stats.components.SessionCard
import com.vinnovateit.autonetconnector.screen.stats.utils.formatFriendlyDate
import com.vinnovateit.autonetconnector.screen.stats.utils.generateCsvReport
import com.vinnovateit.autonetconnector.ui.components.TooltipHint
import com.vinnovateit.autonetconnector.ui.theme.AutoNetConnectorTheme

class StatsActivity : ComponentActivity() {
  private var currentSsid: String? = null

  private val createDocumentLauncher =
    registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
      uri?.let {
        try {
          val viewModel: StatsViewModel by viewModels { StatsViewModelFactory(application) }
          contentResolver.openOutputStream(it)?.use { outputStream ->
            generateCsvReport(viewModel.sessionHistory.value, outputStream)
          }
          Toast.makeText(this, "Report saved successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
          Toast.makeText(this, "Failed to save report", Toast.LENGTH_SHORT).show()
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
            createDocumentLauncher.launch("session_report.csv")
          }
        )
      }
    }
  }
}

@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatsScreen(
  modifier: Modifier = Modifier,
  statsViewModel: StatsViewModel = viewModel(
    factory = StatsViewModelFactory(LocalContext.current.applicationContext as Application)
  ),
  overrideSsid: String?,
  onDownloadReport: () -> Unit
) {
  var showResetDialog by remember { mutableStateOf(false) }
  val context = LocalContext.current as Activity

  val sessionToShow by statsViewModel.sessionToShow.collectAsStateWithLifecycle()
  val historyToShow by statsViewModel.sessionHistory.collectAsStateWithLifecycle()
  val liveStatus by statsViewModel.liveStatus.collectAsStateWithLifecycle()

  val isLive = remember(liveStatus) { liveStatus != null }
  val lazyListState = rememberLazyListState()


  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())


  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
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
            IconButton(onClick = { context.finish() }) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
              )
            }
          }
        },
        actions = {
          TooltipHint(tooltipText = "Reset Stats") {
            IconButton(onClick = { showResetDialog = true }) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset Stats"
              )
            }
          }
        },
        scrollBehavior = scrollBehavior
      )
    }
  )
  { innerPadding ->
    if (!isLive && historyToShow.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
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
    } else {
      LazyColumn(
        state = lazyListState,
        modifier = modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {

        // Conditional content ordering
        if (isLive && sessionToShow != null) {
          // Show live session card if connected
          item {
            SessionCard(
              session = sessionToShow!!,
              overrideSsid = overrideSsid
            )
          }
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
          item {
            HistorySection(history = historyToShow)
          }
        } else {
          // Show history first if disconnected
          item {
            HistorySection(history = historyToShow)
          }
          item {
            val allTimeMaxDownloadBps = historyToShow
              .maxOfOrNull { it.history.maxOfOrNull { p -> p.usage.rxBytes } ?: 0L } ?: 0L
            val allTimeMaxUploadBps = historyToShow
              .maxOfOrNull { it.history.maxOfOrNull { p -> p.usage.txBytes } ?: 0L } ?: 0L
            LiveSpeedSection(
              isLive = false,
              downloadBps = allTimeMaxDownloadBps,
              uploadBps = allTimeMaxUploadBps,
              onDownloadReport = onDownloadReport
            )
          }
          if (historyToShow.isNotEmpty()) {
            val groupedSessions = historyToShow.groupBy {
              formatFriendlyDate(it.startTimestamp)
            }

            groupedSessions.forEach { (date, sessions) ->
              stickyHeader {
                Text(
                  text = date,
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(vertical = 8.dp)
                )
              }
              items(sessions, key = { it.startTimestamp }) { session ->
                HistoryItemCard(session = session)
              }
            }
          }
        }

        item {
          Spacer(modifier = Modifier.height(100.dp))
        }
      }
    }
  }

  if (showResetDialog) {
    AlertDialog(
      onDismissRequest = { showResetDialog = false },
      title = { Text("Reset Stats") },
      text = { Text("Are you sure you want to delete all session history? This action cannot be undone.") },
      confirmButton = {
        TextButton(onClick = {
          statsViewModel.onClearHistory()
          showResetDialog = false
        }) { Text("Reset") }
      },
      dismissButton = {
        TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
      }
    )
  }
}