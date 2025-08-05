package com.vinnovateit.autonetconnector.features.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinnovateit.autonetconnector.common.util.TooltipHint
import com.vinnovateit.autonetconnector.domain.model.SessionRepository
import com.vinnovateit.autonetconnector.features.auth.SecondPageActivity
import com.vinnovateit.autonetconnector.ui.theme.AutoNetConnectorTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    WindowCompat.setDecorFitsSystemWindows(window, false)
    SettingsManager.initialize(applicationContext)
    setContent {
      AutoNetConnectorTheme {
        SettingsScreen(onBackClick = { finish() })
      }
    }
  }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
  val context = LocalContext.current
  // Local states are now managed by SettingsManager, we collect them here
  val autoLogin by SettingsManager.autoLogin.collectAsStateWithLifecycle()
  val speedUnits by SettingsManager.speedUnits.collectAsStateWithLifecycle()
  val theme by SettingsManager.theme.collectAsStateWithLifecycle()
  val dataThreshold by SettingsManager.dataThreshold.collectAsStateWithLifecycle()
  val dataAlertEnabled by SettingsManager.dataAlertEnabled.collectAsStateWithLifecycle()
  val detailedLogs by SettingsManager.detailedLogs.collectAsStateWithLifecycle()

  // Bottom sheet states
  var showSpeedUnitsSheet by remember { mutableStateOf(false) }
  var showThemeSheet by remember { mutableStateOf(false) }
  var showClearStatsSheet by remember { mutableStateOf(false) }
  var showDataThresholdSheet by remember { mutableStateOf(false) }

  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  val configuration = LocalConfiguration.current
  val sidePadding = if (configuration.screenWidthDp > 600) 80.dp else 0.dp

  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.surface,
    topBar = {
      LargeTopAppBar(
        title = {
          Text(
            "Preferences",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        },
        navigationIcon = {
          TooltipHint(tooltipText = "Back") {
            IconButton(
              onClick = onBackClick
            ) {
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", modifier = Modifier.size(24.dp))
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          scrolledContainerColor = MaterialTheme.colorScheme.surface,
          navigationIconContentColor = Color.Unspecified,
          titleContentColor = Color.Unspecified,
          actionIconContentColor = Color.Unspecified
        ),
        scrollBehavior = scrollBehavior
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = sidePadding),
    ) {
      // Account Category
      item {
        PreferenceCategory(title = "Account")
      }
      items(
        listOf(
          PreferenceData("Auto-login on Connect", "Automatically log in to VIT Wi-Fi", Icons.Rounded.Autorenew, trailing = {
            Switch(checked = autoLogin, onCheckedChange = { SettingsManager.setAutoLogin(it) })
          }),
          PreferenceData("Update Credentials", "Change your registration number and password", Icons.Rounded.Key, onClick = {
            context.startActivity(Intent(context, SecondPageActivity::class.java).apply {
              putExtra("editMode", true)
            })
          })
        )
      ) { item ->
        PreferenceItem(item)
      }

      // Display Category
      item {
        PreferenceCategory(title = "Display")
      }
      items(
        listOf(
          PreferenceData("Speed Units", speedUnits, Icons.Rounded.Speed, onClick = { showSpeedUnitsSheet = true }),
          PreferenceData("Theme", theme, Icons.Rounded.DarkMode, onClick = { showThemeSheet = true })
        )
      ) { item ->
        PreferenceItem(item)
      }

      // Data Management Category
      item {
        PreferenceCategory(title = "Data Management")
      }
      item {
        PreferenceItem(PreferenceData("Clear Network Stats", "Reset usage history", Icons.Rounded.SettingsBackupRestore, onClick = { showClearStatsSheet = true }))
      }
      item {
        val thresholdSubtitle = when {
          dataThreshold < 0 -> "Custom"
          else -> "$dataThreshold GB"
        }
        PreferenceItem(
          PreferenceData(
            "Data Alert Threshold",
            subtitle = "Current: $thresholdSubtitle",
            Icons.Rounded.DataUsage,
            onClick = { showDataThresholdSheet = true },
            trailing = {
              Switch(checked = dataAlertEnabled, onCheckedChange = { SettingsManager.setDataAlertEnabled(it) })
            }
          )
        )
      }


      // Advanced Category
      item {
        PreferenceCategory(title = "Advanced")
      }
      item {
        val item = PreferenceData("Detailed Logs", "Enable verbose logging", Icons.Rounded.BugReport, trailing = {
          Switch(checked = detailedLogs, onCheckedChange = { SettingsManager.setDetailedLogs(it) })
        })
        PreferenceItem(item)
      }
    }
  }

  // Bottom Sheets
  if (showSpeedUnitsSheet) {
    SettingsSelectionBottomSheet(
      title = "Speed Units",
      options = listOf(
        SelectionOption("Mbps", Icons.Rounded.Speed),
        SelectionOption("MB/s", Icons.Rounded.Speed)
      ),
      selected = speedUnits,
      onSelect = {
        SettingsManager.setSpeedUnits(it.label)
        showSpeedUnitsSheet = false
      },
      onDismiss = { showSpeedUnitsSheet = false }
    )
  }

  if (showThemeSheet) {
    SettingsSelectionBottomSheet(
      title = "Theme",
      options = listOf(
        SelectionOption("System Default", Icons.Rounded.SettingsSystemDaydream),
        SelectionOption("Light", Icons.Rounded.LightMode),
        SelectionOption("Dark", Icons.Rounded.DarkMode)
      ),
      selected = theme,
      onSelect = {
        SettingsManager.setTheme(it.label)
        showThemeSheet = false
      },
      onDismiss = { showThemeSheet = false }
    )
  }

  if (showDataThresholdSheet) {
    DataThresholdSliderBottomSheet(
      currentThreshold = dataThreshold,
      onThresholdChange = { SettingsManager.setDataThreshold(it) },
      onDismiss = { showDataThresholdSheet = false }
    )
  }

  if (showClearStatsSheet) {
    SettingsActionBottomSheet(
      title = "Clear Network Stats",
      description = "This will reset all usage history. Continue?",
      confirmText = "Clear",
      cancelText = "Cancel",
      onConfirm = {
        SessionRepository.clearHistory()
        showClearStatsSheet = false
      },
      onDismiss = { showClearStatsSheet = false }
    )
  }
}

// Data class for preference items
data class PreferenceData(
  val title: String,
  val subtitle: String,
  val icon: ImageVector,
  val onClick: () -> Unit = {},
  val trailing: @Composable () -> Unit = {}
)

data class SelectionOption(
  val label: String,
  val icon: ImageVector
)

// Custom Composable
@Composable
fun PreferenceCategory(title: String) {
  Text(
    title,
    style = MaterialTheme.typography.labelLarge.copy(
      fontWeight = FontWeight.ExtraBold, // Emphasized
      color = MaterialTheme.colorScheme.primary // Matches homepage
    ),
    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp, start = 16.dp)
  )
}

@Composable
fun PreferenceItem(data: PreferenceData) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(),
        onClick = data.onClick
      )
      .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(data.icon, contentDescription = null, modifier = Modifier.padding(end = 16.dp), tint = MaterialTheme.colorScheme.primary)
    Column(modifier = Modifier.weight(1f)) {
      Text(
        data.title,
        style = MaterialTheme.typography.bodyLarge.copy(
          fontWeight = FontWeight.Bold, // Emphasized
          color = MaterialTheme.colorScheme.onSurface // Matches homepage
        )
      )
      Text(
        data.subtitle,
        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
      )
    }
    Spacer(Modifier.width(16.dp))
    data.trailing()
  }
}

// Bottom Sheet for Dropdowns (simple list selection)
// Bottom Sheet for Dropdowns (simple list selection)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSelectionBottomSheet(
  title: String,
  options: List<SelectionOption>,
  selected: String,
  onSelect: (SelectionOption) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    content = {
      Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
          title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
          modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        options.forEach { option ->
          val isSelected = option.label == selected
          val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = { onSelect(option) }
              )
              .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(option.icon, contentDescription = null, modifier = Modifier.padding(end = 16.dp), tint = if (isSelected) contentColor else LocalContentColor.current)
            Text(
              option.label,
              color = contentColor,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataThresholdSliderBottomSheet(
  currentThreshold: Float,
  onThresholdChange: (Float) -> Unit,
  onDismiss: () -> Unit
) {
  var isCustom by remember { mutableStateOf(currentThreshold < 0) }
  var sliderValue by remember { mutableStateOf(if (isCustom || currentThreshold > 10f || currentThreshold < 1f) 1f else currentThreshold) }
  var customValue by remember { mutableStateOf(if (isCustom) currentThreshold.unaryMinus().toString() else "") }

  LaunchedEffect(isCustom) {
    if (!isCustom) {
      onThresholdChange(sliderValue)
    } else {
      onThresholdChange(customValue.toFloatOrNull()?.unaryMinus() ?: -1f)
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    content = {
      Column(
        modifier = Modifier
          .padding(16.dp)
          .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text("Data Alert Threshold", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
        Text(
          "Get a notification when you're about to exceed your data limit.",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(vertical = 8.dp)
        )

        Slider(
          value = sliderValue,
          onValueChange = {
            sliderValue = ((it * 2).roundToInt() / 2.0f) // Snap to 0.5 steps
            isCustom = false
          },
          valueRange = 1f..10f,
          steps = 17, // (10 - 1) / 0.5 - 1 = 17
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
        )

        Text(
          if (isCustom) "Custom" else "${sliderValue} GB",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(16.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { isCustom = !isCustom }
            .padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Checkbox(checked = isCustom, onCheckedChange = { isCustom = it })
          Text("Custom Threshold")
        }

        if (isCustom) {
          Spacer(Modifier.height(8.dp))
          OutlinedTextField(
            value = customValue,
            onValueChange = {
              customValue = it
              val customFloat = it.toFloatOrNull()
              if(customFloat != null) {
                onThresholdChange(customFloat.unaryMinus()) // Store custom value as negative
              } else {
                onThresholdChange(-1f) // Indicate custom but invalid
              }
            },
            label = { Text("Custom Threshold (GB)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape
          )
        }
      }
    }
  )
}

// Bottom Sheet for Actions (confirm dialogs)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsActionBottomSheet(
  title: String,
  description: String,
  confirmText: String,
  cancelText: String,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    content = {
      Column(
        modifier = Modifier
          .padding(16.dp)
          .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)) // Emphasized
        Spacer(Modifier.height(8.dp))
        Text(description, style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface))
        Spacer(Modifier.height(24.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
            Text(cancelText, fontWeight = FontWeight.Bold)
          }
          Button(
            onClick = onConfirm,
            modifier = Modifier.weight(1f)
          ) {
            Text(confirmText, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  )
}