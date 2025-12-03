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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinnovateit.latch.R
import com.vinnovateit.latch.common.util.generateCsvReport
import com.vinnovateit.latch.common.util.TooltipHint
import com.vinnovateit.latch.domain.model.LiveConnectionStatus
import com.vinnovateit.latch.domain.model.SessionSummary
import com.vinnovateit.latch.features.settings.manager.SettingsManager
import com.vinnovateit.latch.features.stats.components.SessionCard
import com.vinnovateit.latch.features.stats.components.StatsList
import com.vinnovateit.latch.ui.theme.LatchTheme
import com.vinnovateit.latch.ui.theme.ModernizFontFamily
import androidx.compose.foundation.layout.statusBarsPadding

class StatsActivity : ComponentActivity() {
  private val createDocumentLauncher =
    registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
      uri?.let {
        try {
          val viewModel: StatsViewModel by viewModels()
          contentResolver.openOutputStream(it)?.use { outputStream ->
            generateCsvReport(viewModel.historyToShow.value, outputStream)
          }
          Toast.makeText(this, "Report saved successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
          Toast.makeText(this, "Failed to save report: $e", Toast.LENGTH_SHORT).show()
        }
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)
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
  onDownloadReport: () -> Unit
) {
  val context = LocalContext.current as Activity
  val statsViewModel: StatsViewModel by (context as ComponentActivity).viewModels()
  val sessionToShow by statsViewModel.sessionToShow.collectAsStateWithLifecycle()
  val historyToShow by statsViewModel.historyToShow.collectAsStateWithLifecycle()
  val liveStatus by statsViewModel.liveStatus.collectAsStateWithLifecycle()
  val isLive = remember(liveStatus) { liveStatus != null }
  val speedUnits by SettingsManager.speedUnits.collectAsStateWithLifecycle()
  var showAllSessions by remember { mutableStateOf(false) }

  StatsScreenContent(
    modifier = modifier,
    isLive = isLive,
    sessionToShow = sessionToShow,
    historyToShow = historyToShow,
    liveStatus = liveStatus,
    speedUnits = speedUnits,
    showAllSessions = showAllSessions,
    onToggleShowAll = { showAllSessions = !showAllSessions },
    onDownloadReport = onDownloadReport,
    onBackClick = { context.finish() },
    statsViewModel = statsViewModel
  )
}


/**
 * Handles the content of the screen, deciding whether to show the stats list or an empty state message.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsScreenContent(
  modifier: Modifier = Modifier,
  isLive: Boolean,
  sessionToShow: SessionSummary?,
  historyToShow: List<SessionSummary>,
  liveStatus: LiveConnectionStatus?,
  speedUnits: String,
  showAllSessions: Boolean,
  onToggleShowAll: () -> Unit,
  onDownloadReport: () -> Unit,
  onBackClick: () -> Unit,
  statsViewModel: StatsViewModel
) {
  if (!isLive && historyToShow.isEmpty()) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
      modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      topBar = { StatsAppBar(scrollBehavior = scrollBehavior, onBackClick = onBackClick) }
    ) { innerPadding ->
      EmptyStatsView(modifier = Modifier.padding(innerPadding).fillMaxSize())
    }
  } else {
    BoxWithConstraints(modifier = modifier) {
      val isPortrait = maxHeight > maxWidth

      if (!isPortrait && isLive && sessionToShow != null) {
        // Landscape, Live Session: Split layout
        Row(modifier = Modifier.fillMaxSize()) {
          val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
          Scaffold(
            modifier = Modifier
              .weight(0.5f)
              .fillMaxHeight()
              .nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { StatsAppBar(scrollBehavior = scrollBehavior, onBackClick = onBackClick, isLarge = false) } // Normal TopAppBar
          ) { innerPadding ->
            Box(
              modifier = Modifier
                .padding(innerPadding)
                .padding(24.dp),
              contentAlignment = Alignment.Center
            ) {
              SessionCard(session = sessionToShow, speedUnit = speedUnits)
            }
          }

          StatsList(
            modifier = Modifier
              .weight(0.5f)
              .fillMaxHeight()
              .background(MaterialTheme.colorScheme.background), // Matching background
            isLive = true,
            showSessionCard = false,
            sessionToShow = sessionToShow,
            historyToShow = historyToShow,
            liveStatus = liveStatus,
            speedUnits = speedUnits,
            showAllSessions = showAllSessions,
            onToggleShowAll = onToggleShowAll,
            onDownloadReport = onDownloadReport,
            addSpacer = true,
            statsViewModel = statsViewModel
          )
        }
      } else {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

        Scaffold(
          modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
          topBar = { StatsAppBar(scrollBehavior = scrollBehavior, onBackClick = onBackClick) }
        ) { innerPadding ->
          StatsList(
            modifier = (if (isPortrait) Modifier.fillMaxSize() else Modifier.fillMaxSize().padding(horizontal=32.dp))
              .padding(innerPadding),
            isLive = isLive,
            showSessionCard = true,
            sessionToShow = sessionToShow,
            historyToShow = historyToShow,
            liveStatus = liveStatus,
            speedUnits = speedUnits,
            showAllSessions = showAllSessions,
            onToggleShowAll = onToggleShowAll,
            onDownloadReport = onDownloadReport,
            statsViewModel = statsViewModel
          )
        }
      }
    }
  }
}

@Composable
fun DownloadReportButton(onDownloadReport: () -> Unit) {
  // Observe interactions produced by the button itself
  val interactionSource = remember { MutableInteractionSource() }
  val pressedFromInteraction by interactionSource.collectIsPressedAsState()

  // Fallback manual press detector to catch very short taps reliably
  var pressedManual by remember { mutableStateOf(false) }

  // Combine both signals so either one can drive the animation
  val isPressed = pressedFromInteraction || pressedManual

  // Animate radius — smaller when pressed. Use a snappy spring so it completes quickly.
  val cornerRadius by animateDpAsState(
    targetValue = if (isPressed) 8.dp else 24.dp,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium // snappy but not instant
    ),
    label = "cornerRadiusAnim"
  )

  // The button keeps its Material look; we just change its `shape`.
  OutlinedButton(
    onClick = onDownloadReport,
    interactionSource = interactionSource,
    modifier = Modifier
      .fillMaxWidth()
      .height(50.dp)
      .padding(start = 16.dp, end = 16.dp)
      // pointerInput on the button to set pressedManual while finger is down.
      .pointerInput(Unit) {
        detectTapGestures(
          onPress = {
            pressedManual = true
            try {
              awaitRelease() // suspends until release/cancel
            } finally {
              pressedManual = false
            }
          }
        )
      },
    shape = RoundedCornerShape(cornerRadius)
  ) {
    Icon(
      imageVector = Icons.Rounded.Download,
      contentDescription = "Download Usage Report"
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      "Usage Report",
      fontWeight = FontWeight.Bold
    )
  }
}


/**
 * A composable for the top app bar of the stats screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsAppBar(
  scrollBehavior: TopAppBarScrollBehavior,
  onBackClick: () -> Unit,
  isLarge: Boolean = true
) {
  if(isLarge) {
    LargeTopAppBar(
      title = {
        Text(
          "Stats",
          fontSize = 24.sp,
          maxLines = 1,
          fontFamily = ModernizFontFamily,
          overflow = TextOverflow.Ellipsis
        )
      },
      navigationIcon = {
        TooltipHint(tooltipText = stringResource(R.string.stats_go_back)) {
          IconButton(onClick = onBackClick) {
            Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back",  tint = MaterialTheme.colorScheme.onSurface)
          }
        }
      },
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        navigationIconContentColor = Color.Unspecified,
        titleContentColor = Color.Unspecified,
        actionIconContentColor = Color.Unspecified
      ),
      scrollBehavior = scrollBehavior
    )
  } else {
    TopAppBar(
      modifier = Modifier.statusBarsPadding(),
      title = {
        Text(
          "Stats",
          fontSize = 23.sp,
          maxLines = 1,
          color = MaterialTheme.colorScheme.primary,
          fontFamily = ModernizFontFamily,
          overflow = TextOverflow.Ellipsis
        )
      },
      navigationIcon = {
        TooltipHint(tooltipText = stringResource(R.string.stats_go_back)) {
          IconButton(onClick = onBackClick) {
            Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
          }
        }
      },
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        navigationIconContentColor = Color.Unspecified,
        titleContentColor = Color.Unspecified,
        actionIconContentColor = Color.Unspecified
      ),
      scrollBehavior = scrollBehavior
    )
  }
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