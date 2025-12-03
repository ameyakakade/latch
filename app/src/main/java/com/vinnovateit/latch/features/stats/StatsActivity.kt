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
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinnovateit.latch.R
import com.vinnovateit.latch.common.ui.components.ExpressiveTopBarContent
import com.vinnovateit.latch.common.util.generateCsvReport
import com.vinnovateit.latch.domain.model.LiveConnectionStatus
import com.vinnovateit.latch.domain.model.SessionSummary
import com.vinnovateit.latch.features.settings.manager.SettingsManager
import com.vinnovateit.latch.features.stats.components.SessionCard
import com.vinnovateit.latch.features.stats.components.StatsList
import com.vinnovateit.latch.ui.theme.LatchTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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

@Composable
private fun StatsTopBar(
  collapseFraction: Float,
  headerHeight: Dp,
  onBackPressed: () -> Unit,
  onDevOption: () -> Unit
) {
  val surfaceColor = MaterialTheme.colorScheme.surface
  val haptic = LocalHapticFeedback.current

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(headerHeight)
      .background(surfaceColor.copy(alpha = collapseFraction))
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
    ) {
      // Replaced FilledIconButton with Surface+Box to handle LongPress
      Surface(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(start = 12.dp, top = 4.dp)
          .size(40.dp) // Standard IconButton size
          .clip(CircleShape)
          .pointerInput(Unit) {
            detectTapGestures(
              onTap = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onBackPressed()
              },
              onLongPress = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDevOption()
              }
            )
          },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = CircleShape
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onSurface
          )
        }
      }

      ExpressiveTopBarContent(
        title = "Stats",
        collapseFraction = collapseFraction,
        modifier = Modifier
          .fillMaxSize()
          .padding(start = 0.dp, end = 0.dp)
      )
    }
  }
}

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

  val density = LocalDensity.current
  val coroutineScope = rememberCoroutineScope()
  val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isPortrait = maxHeight > maxWidth

    // -- Scroll & AppBar Animation Logic --
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minTopBarHeight = 100.dp + statusBarHeight
    val maxTopBarHeight = 180.dp
    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }

    val topBarHeight = remember(isPortrait) { Animatable(maxTopBarHeightPx) }
    var collapseFraction by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(topBarHeight.value, maxTopBarHeightPx, minTopBarHeightPx) {
      collapseFraction = 1f - ((topBarHeight.value - minTopBarHeightPx) / (maxTopBarHeightPx - minTopBarHeightPx)).coerceIn(0f, 1f)
    }

    val nestedScrollConnection = remember(isPortrait, maxTopBarHeightPx) {
      object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
          val delta = available.y
          val isScrollingDown = delta < 0

          if (!isScrollingDown && (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0)) {
            return Offset.Zero
          }

          val previousHeight = topBarHeight.value
          val newHeight = (previousHeight + delta).coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
          val consumed = newHeight - previousHeight

          if (consumed.roundToInt() != 0) {
            coroutineScope.launch { topBarHeight.snapTo(newHeight) }
          }

          val canConsumeScroll = !(isScrollingDown && newHeight == minTopBarHeightPx)
          return if (canConsumeScroll) Offset(0f, consumed) else Offset.Zero
        }
      }
    }

    // Snap open/closed when scroll ends
    LaunchedEffect(lazyListState.isScrollInProgress) {
      if (!lazyListState.isScrollInProgress) {
        val shouldExpand = topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2
        val canExpand = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
        val targetValue = if (shouldExpand && canExpand) maxTopBarHeightPx else minTopBarHeightPx
        if (topBarHeight.value != targetValue) {
          coroutineScope.launch { topBarHeight.animateTo(targetValue, spring(stiffness = Spring.StiffnessMedium)) }
        }
      }
    }

    if (!isLive && historyToShow.isEmpty()) {
      // Empty State: Use simple Scaffolding
      val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
      Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
          // Reuse new TopBar logic but fixed
          StatsTopBar(
            collapseFraction = 0f,
            headerHeight = maxTopBarHeight,
            onBackPressed = { context.finish() },
            onDevOption = { statsViewModel.generateDummyData() }
          )
        }
      ) { innerPadding ->
        EmptyStatsView(modifier = Modifier.padding(innerPadding).fillMaxSize())
      }
    } else {
      if (!isPortrait && isLive && sessionToShow != null) {
        // --- LANDSCAPE SPLIT VIEW ---
        Row(modifier = Modifier.fillMaxSize()) {
          // Left Pane: Header + Card (Fixed Header)
          Column(
            modifier = Modifier
              .weight(0.5f)
              .fillMaxHeight()
              .background(MaterialTheme.colorScheme.background)
          ) {
            // In split view, keep header small/collapsed to save space
            StatsTopBar(
              collapseFraction = 1f,
              headerHeight = minTopBarHeight,
              onBackPressed = { context.finish() },
              onDevOption = { statsViewModel.generateDummyData() }
            )
            Box(
              modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              SessionCard(session = sessionToShow!!, speedUnit = speedUnits)
            }
          }

          // Right Pane: List (No Header)
          StatsList(
            modifier = Modifier
              .weight(0.5f)
              .fillMaxHeight()
              .background(MaterialTheme.colorScheme.background),
            isLive = true,
            showSessionCard = false,
            sessionToShow = sessionToShow,
            historyToShow = historyToShow,
            liveStatus = liveStatus,
            speedUnits = speedUnits,
            showAllSessions = showAllSessions,
            onToggleShowAll = { showAllSessions = !showAllSessions },
            onDownloadReport = onDownloadReport,
            addSpacer = true,
            contentPadding = PaddingValues(top = 16.dp), // Simple padding for right pane
            statsViewModel = statsViewModel
          )
        }
      } else {
        // --- PORTRAIT / LANDSCAPE SINGLE PANE ---
        val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }

        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(nestedScrollConnection)
        ) {
          // List Content with top padding to slide behind the header
          StatsList(
            modifier = Modifier.fillMaxSize(),
            isLive = isLive,
            showSessionCard = true,
            sessionToShow = sessionToShow,
            historyToShow = historyToShow,
            liveStatus = liveStatus,
            speedUnits = speedUnits,
            showAllSessions = showAllSessions,
            onToggleShowAll = { showAllSessions = !showAllSessions },
            onDownloadReport = onDownloadReport,
            contentPadding = PaddingValues(top = currentTopBarHeightDp),
            statsViewModel = statsViewModel
          )

          // Collapsible App Bar Overlay
          StatsTopBar(
            collapseFraction = collapseFraction,
            headerHeight = currentTopBarHeightDp,
            onBackPressed = { context.finish() },
            onDevOption = { statsViewModel.generateDummyData() }
          )
        }
      }
    }
  }
}

@Composable
fun DownloadReportButton(onDownloadReport: () -> Unit) {
  val haptic = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val pressedFromInteraction by interactionSource.collectIsPressedAsState()
  var pressedManual by remember { mutableStateOf(false) }
  val isPressed = pressedFromInteraction || pressedManual

  val cornerRadius by animateDpAsState(
    targetValue = if (isPressed) 8.dp else 24.dp,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    label = "cornerRadiusAnim"
  )

  OutlinedButton(
    onClick = {
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
      onDownloadReport()
    },
    interactionSource = interactionSource,
    modifier = Modifier
      .fillMaxWidth()
      .height(50.dp)
      .padding(start = 16.dp, end = 16.dp)
      .pointerInput(Unit) {
        detectTapGestures(
          onPress = {
            pressedManual = true
            try { awaitRelease() } finally { pressedManual = false }
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