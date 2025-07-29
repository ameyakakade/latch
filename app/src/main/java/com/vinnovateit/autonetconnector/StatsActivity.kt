package com.vinnovateit.autonetconnector

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vinnovateit.autonetconnector.functionality.StatsViewModel
import com.vinnovateit.autonetconnector.functionality.StatsViewModelFactory
import com.vinnovateit.autonetconnector.screen.stats.components.HistoryItemCard
import com.vinnovateit.autonetconnector.screen.stats.components.HistorySection
import com.vinnovateit.autonetconnector.screen.stats.components.LiveSpeedSection
import com.vinnovateit.autonetconnector.screen.stats.components.SessionCard
import com.vinnovateit.autonetconnector.screen.stats.utils.formatFriendlyDate
import com.vinnovateit.autonetconnector.ui.components.TooltipHint
import com.vinnovateit.autonetconnector.ui.theme.AutoNetConnectorTheme
import com.vinnovateit.autonetconnector.ui.theme.CollapsedAppBar

class StatsActivity : ComponentActivity() {
  private var currentSsid: String? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    currentSsid = intent.getStringExtra("CURRENT_SSID")
    setContent {
      AutoNetConnectorTheme {
        StatsScreen(overrideSsid = currentSsid)
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
  overrideSsid: String?
) {
  var showResetDialog by remember { mutableStateOf(false) }
  val context = LocalContext.current as Activity

  val sessionToShow by statsViewModel.sessionToShow.collectAsStateWithLifecycle()
  val historyToShow by statsViewModel.sessionHistory.collectAsStateWithLifecycle()
  val liveStatus by statsViewModel.liveStatus.collectAsStateWithLifecycle()

  val isLive = remember(liveStatus) { liveStatus != null }
  val lazyListState = rememberLazyListState()

  // Alpha for the large title in the list, fades out on scroll
  val largeTitleAlpha by remember {
    derivedStateOf {
      if (lazyListState.firstVisibleItemIndex == 0) {
        val scrollOffset = lazyListState.firstVisibleItemScrollOffset.toFloat()
        (1f - (scrollOffset / 200f)).coerceIn(0f, 1f)
      } else {
        0f
      }
    }
  }

  // Alpha for the small title in the app bar, fades in on scroll
  val appBarTitleAlpha by remember {
    derivedStateOf {
      if (lazyListState.firstVisibleItemIndex == 0) {
        val scrollOffset = lazyListState.firstVisibleItemScrollOffset.toFloat()
        ((scrollOffset - 150f) / 50f).coerceIn(0f, 1f)
      } else {
        1f
      }
    }
  }


  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = lerp(
            start = MaterialTheme.colorScheme.background.copy(alpha = 0f),
            stop = CollapsedAppBar,
            fraction = appBarTitleAlpha
          )
        ),
        title = {
          Text(
            text = "Network Statistics",
            modifier = Modifier.alpha(appBarTitleAlpha), // Title fades in
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
        },
        navigationIcon = {
          Box(modifier = Modifier.padding(start = 8.dp)) {
            TooltipHint(tooltipText = "Go back") {
              FilledIconButton(
                onClick = { context.finish() },
                colors = IconButtonDefaults.filledIconButtonColors(
                  containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                  contentColor = MaterialTheme.colorScheme.onBackground
                )
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                  contentDescription = "Back"
                )
              }
            }
          }
        },
        actions = {
          TooltipHint(tooltipText = "Reset all stats") {
            IconButton(onClick = { showResetDialog = true }) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Reset Stats",
              )
            }
          }
        }
      )
    }
  ) { innerPadding ->
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
        // Large Title as the first item, which will fade out
        item {
          Text(
            "Network Statistics",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.alpha(largeTitleAlpha).fillMaxWidth()
          )
        }

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
            LiveSpeedSection(session = sessionToShow)
          }
          item {
            HistorySection(history = historyToShow)
          }
        } else {
          // Show history first if disconnected
          item {
            HistorySection(history = historyToShow)
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
