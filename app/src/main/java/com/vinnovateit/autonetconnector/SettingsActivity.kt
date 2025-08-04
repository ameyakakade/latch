package com.vinnovateit.autonetconnector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.vinnovateit.autonetconnector.functionality2.manager.SessionRepository
import com.vinnovateit.autonetconnector.ui.components.TooltipHint
import com.vinnovateit.autonetconnector.ui.theme.AutoNetConnectorTheme
import java.time.Year

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    WindowCompat.setDecorFitsSystemWindows(window, false)
    setContent {
      AutoNetConnectorTheme {
        SettingsScreen(onBackClick = { finish() })
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
  // Local states
  var autoLogin by remember { mutableStateOf(true) }
  var speedUnits by remember { mutableStateOf("Mbps") }
  var darkMode by remember { mutableStateOf("System Default") }
  var dataThreshold by remember { mutableStateOf(1) } // Integral 1-10 GB
  var dataAlertEnabled by remember { mutableStateOf(true) } // Switch for Data Alert
  var detailedLogs by remember { mutableStateOf(false) }

  // Bottom sheet states
  var showSpeedUnitsSheet by remember { mutableStateOf(false) }
  var showDarkModeSheet by remember { mutableStateOf(false) }
  var showUpdateCredentialsSheet by remember { mutableStateOf(false) }
  var showClearStatsSheet by remember { mutableStateOf(false) }

  // Credential editing states
  var registrationNumber by remember { mutableStateOf("") }
  var registrationNumberError by remember { mutableStateOf<String?>(null) }
  var password by remember { mutableStateOf("") }
  var showPassword by remember { mutableStateOf(false) }

  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  val configuration = LocalConfiguration.current
  val sidePadding = if (configuration.screenWidthDp > 600) 80.dp else 16.dp // Padding for wide screens

  val currentYear = Year.now().value % 100
  fun validateRegistrationNumber(input: String): String? {
    if (input.length != 9) return "Must be exactly 9 characters (YYAAAXXXX)"
    val yy = input.substring(0, 2).toIntOrNull() ?: return "Invalid year format"
    if (yy > currentYear) return "Year cannot be in the future"
    val aaa = input.substring(2, 5)
    if (!aaa.all { it.isLetter() }) return "Branch must be 3 letters"
    val xxxx = input.substring(5)
    if (!xxxx.all { it.isDigit() }) return "Last 4 must be digits"
    return null
  }

  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      LargeTopAppBar(
        title = {
          Text(
            "Preferences",
            fontSize = if (scrollBehavior.state.collapsedFraction > 0.5f) 24.sp else 32.sp,
            fontWeight = FontWeight.ExtraBold, // Emphasized font
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        },
        navigationIcon = {
          TooltipHint(tooltipText = "Back") {
            FilledIconButton(
              onClick = onBackClick,
              modifier = Modifier.size(48.dp).padding(8.dp)
            ) {
              Icon(Icons.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(24.dp))
            }
          }
        },
        colors = TopAppBarDefaults.largeTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          scrolledContainerColor = MaterialTheme.colorScheme.surface
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
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      // Account Category
      item {
        PreferenceCategory(title = "Account")
      }
      itemsIndexed(listOf(
        PreferenceData("Auto-login on Connect", "Automatically log in to VIT Wi-Fi", Icons.Outlined.Autorenew, trailing = {
          Switch(checked = autoLogin, onCheckedChange = { autoLogin = it /* TODO: Save */ })
        }),
        PreferenceData("Update Credentials", "Change your registration number and password", Icons.Filled.Key, onClick = { showUpdateCredentialsSheet = true })
      )) { index, item ->
        val shape = when {
          index == 0 -> MaterialTheme.shapes.medium.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
          index == 1 -> MaterialTheme.shapes.medium.copy(topStart = CornerSize(0.dp), topEnd = CornerSize(0.dp))
          else -> MaterialTheme.shapes.small
        }
        PreferenceItem(item, shape)
      }

      // Display Category
      item {
        PreferenceCategory(title = "Display")
      }
      itemsIndexed(listOf(
        PreferenceData("Speed Units", speedUnits, Icons.Outlined.Speed, onClick = { showSpeedUnitsSheet = true }),
        PreferenceData("Dark Mode", darkMode, Icons.Outlined.DarkMode, onClick = { showDarkModeSheet = true })
      )) { index, item ->
        val shape = when {
          index == 0 -> MaterialTheme.shapes.medium.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
          index == 1 -> MaterialTheme.shapes.medium.copy(topStart = CornerSize(0.dp), topEnd = CornerSize(0.dp))
          else -> MaterialTheme.shapes.small
        }
        PreferenceItem(item, shape)
      }

      // Data Management Category
      item {
        PreferenceCategory(title = "Data Management")
      }
      itemsIndexed(listOf(
        PreferenceData("Clear Network Stats", "Reset usage history", Icons.Filled.SettingsBackupRestore, onClick = { showClearStatsSheet = true }),
        PreferenceData("Data Alert Threshold (GB)", "${dataThreshold} GB", Icons.Outlined.DataUsage, trailing = {
          Switch(checked = dataAlertEnabled, onCheckedChange = { dataAlertEnabled = it /* TODO: Save */ })
        })
      )) { index, item ->
        val shape = when {
          index == 0 -> MaterialTheme.shapes.medium.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
          index == 1 -> MaterialTheme.shapes.medium.copy(topStart = CornerSize(0.dp), topEnd = CornerSize(0.dp))
          else -> MaterialTheme.shapes.small
        }
        if (index == 1) {
          Column(modifier = Modifier.clip(shape)) {
            PreferenceItem(item, shape)
            Slider(
              value = dataThreshold.toFloat(),
              onValueChange = { dataThreshold = it.toInt() /* TODO: Save */ },
              valueRange = 1f..10f,
              steps = 9, // Integral steps 1-10
              enabled = dataAlertEnabled, // Disable if switch is off
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
            )
          }
        } else {
          PreferenceItem(item, shape)
        }
      }

      // Advanced Category
      item {
        PreferenceCategory(title = "Advanced")
      }
      item {
        val item = PreferenceData("Detailed Logs", "Enable verbose logging", Icons.Filled.BugReport, trailing = {
          Switch(checked = detailedLogs, onCheckedChange = { detailedLogs = it /* TODO: Save */ })
        })
        PreferenceItem(item, MaterialTheme.shapes.medium) // Single item, full rounding
      }
    }
  }

  // Bottom Sheets
  if (showSpeedUnitsSheet) {
    SettingsDropdownBottomSheet(
      title = "Speed Units",
      options = listOf("Mbps", "MB/s"),
      selected = speedUnits,
      onSelect = { speedUnits = it /* TODO: Save */ },
      onDismiss = { showSpeedUnitsSheet = false }
    )
  }

  if (showDarkModeSheet) {
    SettingsDropdownBottomSheet(
      title = "Dark Mode",
      options = listOf("System Default", "Light", "Dark"),
      selected = darkMode,
      onSelect = { darkMode = it /* TODO: Save */ },
      onDismiss = { showDarkModeSheet = false }
    )
  }

  if (showUpdateCredentialsSheet) {
    UpdateCredentialsBottomSheet(
      registrationNumber = registrationNumber,
      onRegistrationNumberChange = {
        val newValue = it.uppercase()
        registrationNumber = newValue
        registrationNumberError = validateRegistrationNumber(newValue)
      },
      registrationNumberError = registrationNumberError,
      password = password,
      onPasswordChange = { password = it },
      showPassword = showPassword,
      onShowPasswordChange = { showPassword = it },
      onSave = {
        if (registrationNumber.isBlank() || password.isBlank()) {
          // Notify user (e.g., set error)
          if (registrationNumber.isBlank()) registrationNumberError = "Required field"
          // Similar for password if needed
        } else if (registrationNumberError == null) {
          // TODO: Save to database
          showUpdateCredentialsSheet = false
        }
      },
      onDismiss = { showUpdateCredentialsSheet = false }
    )
  }

  if (showClearStatsSheet) {
    SettingsActionBottomSheet(
      title = "Clear Network Stats",
      description = "This will reset all usage history. Continue?",
      confirmText = "Clear",
      cancelText = "Cancel",
      onConfirm = { SessionRepository.clearHistory() },
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

// Custom Composables
@Composable
fun PreferenceCategory(title: String) {
  Text(
    title,
    style = MaterialTheme.typography.labelLarge.copy(
      fontWeight = FontWeight.ExtraBold, // Emphasized
      color = MaterialTheme.colorScheme.onSurface // Matches homepage
    ),
    modifier = Modifier.padding(vertical = 16.dp)
  )
}

@Composable
fun PreferenceItem(data: PreferenceData, shape: Shape) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(shape),
    onClick = data.onClick,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(data.icon, contentDescription = null, modifier = Modifier.padding(end = 16.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          data.title,
          style = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.Medium, // Emphasized
            color = MaterialTheme.colorScheme.onSurface // Matches homepage
          )
        )
        Text(
          data.subtitle,
          style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
      }
      data.trailing()
    }
  }
}

// Bottom Sheet for Dropdowns (simple list selection)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdownBottomSheet(
  title: String,
  options: List<String>,
  selected: String,
  onSelect: (String) -> Unit,
  onDismiss: () -> Unit
) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)) // Emphasized
      Spacer(Modifier.height(8.dp))
      options.forEach { option ->
        TextButton(
          onClick = {
            onSelect(option)
            onDismiss()
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(option, color = if (option == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
      }
    }
  }
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
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)) // Emphasized
      Spacer(Modifier.height(8.dp))
      Text(description, style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface))
      Spacer(Modifier.height(16.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDismiss) {
          Text(cancelText)
        }
        TextButton(onClick = {
          onConfirm()
          onDismiss()
        }) {
          Text(confirmText)
        }
      }
    }
  }
}

// Bottom Sheet for Update Credentials
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateCredentialsBottomSheet(
  registrationNumber: String,
  onRegistrationNumberChange: (String) -> Unit,
  registrationNumberError: String?,
  password: String,
  onPasswordChange: (String) -> Unit,
  showPassword: Boolean,
  onShowPasswordChange: (Boolean) -> Unit,
  onSave: () -> Unit,
  onDismiss: () -> Unit
) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text("Update Credentials", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(
        value = registrationNumber,
        onValueChange = onRegistrationNumberChange,
        label = { Text("Registration Number (YYAAAXXXX)") },
        isError = registrationNumberError != null,
        supportingText = { if (registrationNumberError != null) Text(registrationNumberError, color = MaterialTheme.colorScheme.error) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium // M3 rounded corners
      )
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Password") },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
          IconButton(onClick = { onShowPasswordChange(!showPassword) }) {
            Icon(if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = "Toggle visibility")
          }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium // M3 rounded corners
      )
      Spacer(Modifier.height(16.dp))
      Button(
        onClick = onSave,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large, // M3 shape for expressive motion
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp) // Flatten on press for motion
      ) {
        Text("Save", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
      }
    }
  }
}
