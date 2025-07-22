package com.vinnovateit.autonetconnector

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.vinnovateit.autonetconnector.ui.theme.AutoNetConnectorTheme

class StatsActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AutoNetConnectorTheme {
        StatsScreen()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
  val haptic = LocalHapticFeedback.current

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
        },
        actions = {
          // This Icon replaces the button and switch from the bottom bar.
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Reset Stats or Toggle Mock Data",
            modifier = Modifier
              .padding(end = 8.dp)
              .clip(RoundedCornerShape(50)) // Makes the ripple effect circular
              .combinedClickable(
                onClick = {
                  // Single tap shows the reset dialog.
                  if (!showMockData) {
                    showResetDialog = true
                  }
                },
                onLongClick = {
                  // Long press toggles mock data.
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  viewModel.onToggleMockData(!showMockData)
                }
              )
              .padding(8.dp) // Adds padding for a larger touch target
          )
        }
      )
    },
    // The bottom bar is now empty.
    bottomBar = {}
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

      // **THE FIX IS HERE:** Adds empty space at the bottom of the list.
      item {
        Spacer(modifier = Modifier.height(100.dp))
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
