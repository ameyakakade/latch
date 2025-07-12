package com.vinnovateit.autonetconnector

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vinnovateit.autonetconnector.functionality.StatsViewModel
import com.vinnovateit.autonetconnector.functionality.UserCredentials
import com.vinnovateit.autonetconnector.functionality.WifiEntry
import com.vinnovateit.autonetconnector.functionality.WifiScanner
import com.vinnovateit.autonetconnector.functionality.WifiStatsManager
import com.vinnovateit.autonetconnector.functionality.getUserCredentials
import com.vinnovateit.autonetconnector.functionality.saveUserCredentials
import com.vinnovateit.autonetconnector.screen.stats.StatsScreen
import com.vinnovateit.autonetconnector.ui.theme.AutoNetConnectorTheme

// this is just a sample ui for debugging purposes

class MainActivity : ComponentActivity() {

    private lateinit var wifiScanner: WifiScanner
    private lateinit var wifiStatsManager: WifiStatsManager

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize helpers
        wifiScanner = WifiScanner(this)
        wifiStatsManager = WifiStatsManager

        // --- Permission Handling ---
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allPermissionsGranted = permissions.entries.all { it.value }
            if (!allPermissionsGranted) {
                Toast.makeText(this, "Permissions required for full functionality.", Toast.LENGTH_LONG).show()
            }
        }
        val permissionsNotGranted = permissionsToRequest.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsNotGranted.isNotEmpty()) {
            permissionLauncher.launch(permissionsNotGranted.toTypedArray())
        }

        setContent {
            AutoNetConnectorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val statsViewModel: StatsViewModel = viewModel(factory = StatsViewModelFactory(application))

                    var selectedTab by remember { mutableIntStateOf(0) }
                    val tabs = listOf("WiFi Scanner", "Credentials Debug", "Network Stats")

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TabRow(selectedTabIndex = selectedTab) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = { Text(title) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(modifier = Modifier.fillMaxSize()) {
                            when (selectedTab) {
                                0 -> WifiScannerScreen(
                                    onRequestPermission = {
                                        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
                                        }
                                        permissionLauncher.launch(permissions.toTypedArray())
                                    },
                                    onScanRequest = { callback ->
                                        wifiScanner.scanWifiNetworks(callback)
                                    },
                                    wifiScanner = wifiScanner,
                                    modifier = Modifier.fillMaxSize()
                                )
                                1 -> CredentialsScreen(modifier = Modifier.fillMaxSize())
                                2 -> StatsScreen(viewModel = statsViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

class StatsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun WifiScannerScreen(
    onRequestPermission: () -> Unit,
    onScanRequest: (onResult: (List<WifiEntry>) -> Unit) -> Unit,
    wifiScanner: WifiScanner,
    modifier: Modifier = Modifier
) {
    var wifiList by remember { mutableStateOf<List<WifiEntry>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(wifiScanner.hasPermission()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var scanCompleted by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        hasPermission = wifiScanner.hasPermission()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Button(
            onClick = {
                errorMessage = null
                scanCompleted = false
                wifiList = emptyList()

                if (!hasPermission) {
                    onRequestPermission()
                    hasPermission = wifiScanner.hasPermission()
                } else {
                    isScanning = true
                    onScanRequest { results ->
                        wifiList = results
                        isScanning = false
                        scanCompleted = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isScanning
        ) {
            if (isScanning) {
                Row {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scanning...")
                }
            } else {
                Text(if (hasPermission) "Scan WiFi" else "Grant Permission & Scan")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (scanCompleted) {
            Text(
                text = "Scan complete.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        when {
            isScanning -> {
                Box(modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator(modifier = Modifier.wrapContentSize())
                }
            }
            wifiList.isEmpty() -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (hasPermission) "No WiFi networks found" else "Permission required to scan WiFi",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            else -> {
                Text(
                    text = "Found ${wifiList.size} WiFi network(s):",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(wifiList) { entry ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = entry.ssid, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Signal: ${entry.level} dBm",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CredentialsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var regNo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var debugText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var wifiName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        getUserCredentials(context)?.let {
            regNo = it.registrationNumber
            password = it.password
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        OutlinedTextField(
            value = regNo,
            onValueChange = { regNo = it },
            label = { Text("Registration Number") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (regNo.isNotBlank() && password.isNotBlank()) {
                    saveUserCredentials(context, UserCredentials(regNo, password, "D-ANX-VIT")) // this is hardcoded for debugging... to be changed later
                    message = "Credentials saved!"
                } else {
                    message = "Please enter registration number, password."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Credentials")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val credentials = getUserCredentials(context)
                if (credentials != null) {
                    debugText = "Cached Credentials:\nReg No: ${credentials.registrationNumber}\nPassword: ${credentials.password}"
                    message = "Credentials loaded from cache!"
                } else {
                    debugText = ""
                    message = "No credentials found in cache."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Credentials")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (message.isNotEmpty()) {
            Text(text = message, color = MaterialTheme.colorScheme.primary)
        }

        if (debugText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = debugText,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
