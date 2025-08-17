package com.vinnovateit.latch.features.settings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinnovateit.latch.common.util.TooltipHint
import com.vinnovateit.latch.domain.model.SessionRepository
import com.vinnovateit.latch.features.auth.SecondPageActivity
import com.vinnovateit.latch.features.settings.manager.SettingsManager
import com.vinnovateit.latch.ui.theme.LatchTheme
import com.vinnovateit.latch.ui.theme.ModernizFontFamily

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    WindowCompat.setDecorFitsSystemWindows(window, false)
    SettingsManager.initialize(applicationContext)
    setContent {
      LatchTheme {
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
  val autoLogin by SettingsManager.autoLogin.collectAsStateWithLifecycle()
  val speedUnits by SettingsManager.speedUnits.collectAsStateWithLifecycle()
  val theme by SettingsManager.theme.collectAsStateWithLifecycle()

  // Bottom sheet states
  var showSpeedUnitsSheet by remember { mutableStateOf(false) }
  var showThemeSheet by remember { mutableStateOf(false) }
  var showClearStatsSheet by remember { mutableStateOf(false) }

  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  val configuration = LocalConfiguration.current
  val sidePadding = if (configuration.screenWidthDp > 600) 80.dp else 0.dp

  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.surface,
    contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
    topBar = {
      LargeTopAppBar(
        title = {
          Text(
            "Preferences",
            fontSize = 23.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            fontFamily = ModernizFontFamily,
            overflow = TextOverflow.Ellipsis
          )
        },
        navigationIcon = {
          TooltipHint(tooltipText = "Back") {
            IconButton(
              onClick = onBackClick
            ) {
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
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
        PreferenceItem(PreferenceData("Clear Stats", "Reset usage history", Icons.Rounded.SettingsBackupRestore, onClick = { showClearStatsSheet = true }))
      }
    }
  }

  // Bottom Sheets
  if (showSpeedUnitsSheet) {
    SettingsSelectionBottomSheet(
      title = "Speed Units",
      options = listOf(
        SelectionOption("bps", Icons.Rounded.Speed),
        SelectionOption("B/s", Icons.Rounded.Speed)
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

  if (showClearStatsSheet) {
    SettingsActionBottomSheet(
      title = "Clear Stats",
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
      fontWeight = FontWeight.ExtraBold,
      color = MaterialTheme.colorScheme.primary
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
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
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
    containerColor = MaterialTheme.colorScheme.surface,
    content = {
      Column(modifier = Modifier.padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
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
            Icon(option.icon, contentDescription = null, modifier = Modifier.padding(end = 16.dp), tint = contentColor)
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
    containerColor = MaterialTheme.colorScheme.surface,
    content = {
      Column(
        modifier = Modifier
          .padding(16.dp)
          .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
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
