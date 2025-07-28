package com.vinnovateit.autonetconnector

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vinnovateit.autonetconnector.functionality.StatsViewModel
import com.vinnovateit.autonetconnector.functionality.StatsViewModelFactory
import com.vinnovateit.autonetconnector.screen.stats.components.HistorySection
import com.vinnovateit.autonetconnector.screen.stats.components.LiveSpeedSection
import com.vinnovateit.autonetconnector.screen.stats.components.SessionCard
import com.vinnovateit.autonetconnector.screen.stats.utils.Timeframe
import com.vinnovateit.autonetconnector.ui.components.TooltipHint
import com.vinnovateit.autonetconnector.ui.theme.AutoNetConnectorTheme

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
  viewModel: StatsViewModel = viewModel(
    factory = StatsViewModelFactory(LocalContext.current.applicationContext as Application)
  ),
  overrideSsid: String?
) {
  var showResetDialog by remember { mutableStateOf(false) }
  val context = LocalContext.current as Activity

  val sessionToShow by viewModel.sessionToShow.collectAsStateWithLifecycle()
  val historyToShow by viewModel.historyToShow.collectAsStateWithLifecycle()
  val liveStatus by viewModel.liveStatus.collectAsStateWithLifecycle()

  val isLive = remember(liveStatus) { liveStatus != null }
  val currentTimeframe = if (isLive) Timeframe.LIVE else Timeframe.LAST

  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

  fun Color.darker(percent: Float): Color {
    return Color(
      ColorUtils.blendARGB(this.toArgb(), Color.Black.toArgb(), percent)
    )
  }

  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      LargeTopAppBar(
        colors = TopAppBarDefaults.largeTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
          scrolledContainerColor = MaterialTheme.colorScheme.background.darker(0.07f), // Darker color on collapse
          titleContentColor = MaterialTheme.colorScheme.onBackground,
          actionIconContentColor = MaterialTheme.colorScheme.onBackground,
          navigationIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
        title = {
          Text(
            "Network Statistics",
            style = MaterialTheme.typography.headlineLarge,
            fontSize = lerp(
              start = MaterialTheme.typography.headlineLarge.fontSize,
              stop = MaterialTheme.typography.titleLarge.fontSize,
              fraction = scrollBehavior.state.collapsedFraction
            ),
            fontWeight = FontWeight.Bold,
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
        },
        scrollBehavior = scrollBehavior
      )
    }
  ) { innerPadding ->
    if (sessionToShow == null && historyToShow.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          // TODO: Add your vector image here
          // Image(
          //     painter = painterResource(id = R.drawable.your_vector),
          //     contentDescription = "No stats available"
          // )
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
        modifier = modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        item {
          sessionToShow?.let {
            SessionCard(
              timeframe = currentTimeframe,
              session = it,
              isLive = isLive,
              overrideSsid = overrideSsid
            )
          }
        }

        if (sessionToShow != null) {
          item {
            LiveSpeedSection(session = sessionToShow)
          }
        }

        item {
          HistorySection(history = historyToShow)
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
