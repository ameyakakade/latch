package com.vinnovateit.autonetconnector.screen.stats

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vinnovateit.autonetconnector.functionality.StatsViewModel
import com.vinnovateit.autonetconnector.functionality.StatsViewModelFactory
import com.vinnovateit.autonetconnector.screen.stats.components.HistorySection
import com.vinnovateit.autonetconnector.screen.stats.components.SessionCard
import com.vinnovateit.autonetconnector.screen.stats.ui.NoDataCard
import com.vinnovateit.autonetconnector.screen.stats.utils.Timeframe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
  modifier: Modifier = Modifier,
  viewModel: StatsViewModel = viewModel(
    factory = StatsViewModelFactory(LocalContext.current.applicationContext as Application)
  )
) {
  val showMockData by viewModel.showMockData.collectAsStateWithLifecycle()
  var showResetDialog by remember { mutableStateOf(false) }

  val sessionToShow by viewModel.sessionToShow.collectAsStateWithLifecycle()
  val historyToShow by viewModel.historyToShow.collectAsStateWithLifecycle()
  val liveStatus by viewModel.liveStatus.collectAsStateWithLifecycle()

  val isLive = remember(liveStatus, showMockData) { liveStatus != null && !showMockData }
  val currentTimeframe = if (isLive) Timeframe.LIVE else Timeframe.LAST

  Scaffold(
    containerColor = Color(0xFF0B1957),
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color.Transparent,
          titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        title = {
          Text(
            "Network Statistics",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
          )
        }
      )
    },
    bottomBar = {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Button(
          onClick = { showResetDialog = true },
          modifier = Modifier.fillMaxWidth(),
          enabled = !showMockData
        ) { Text("Reset All Stats") }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.End,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Mock Data")
          Spacer(Modifier.width(8.dp))
          Switch(
            checked = showMockData,
            onCheckedChange = { viewModel.onToggleMockData(it) }
          )
        }
      }
    }
  ) { padding ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .padding(padding),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      item {
        if (sessionToShow != null) {
          SessionCard(
            timeframe = currentTimeframe,
            session = sessionToShow!!,
            isLive = isLive
          )
        } else {
          NoDataCard("No Wi-Fi session data available.")
        }
      }
      item { HistorySection(history = historyToShow) }
    }
  }

  if (showResetDialog) {
    AlertDialog(
      onDismissRequest = { showResetDialog = false },
      title = { Text("Reset Stats") },
      text = { Text("Are you sure you want to delete all session history? This action cannot be undone.") },
      confirmButton = {
        TextButton(onClick = {
          viewModel.onClearHistory()
          showResetDialog = false
        }) { Text("Reset") }
      },
      dismissButton = {
        TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
      }
    )
  }
}