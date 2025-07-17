package com.vinnovateit.autonetconnector

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.Crossfade
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vinnovateit.autonetconnector.functionality.StatsViewModel
import com.vinnovateit.autonetconnector.functionality.StatsViewModelFactory
import com.vinnovateit.autonetconnector.functionality.UserCredentials
import com.vinnovateit.autonetconnector.functionality.WifiEntry
import com.vinnovateit.autonetconnector.functionality.WifiScanner
import com.vinnovateit.autonetconnector.functionality.getUserCredentials
import com.vinnovateit.autonetconnector.functionality.saveUserCredentials
import com.vinnovateit.autonetconnector.functionality2.background.MyForegroundService
import com.vinnovateit.autonetconnector.functionality2.background.WiFiMonitor
import com.vinnovateit.autonetconnector.functionality2.ui.LoginTestRunner
import com.vinnovateit.autonetconnector.screen.stats.StatsScreen
import com.vinnovateit.autonetconnector.ui.theme.AutoNetConnectorTheme
import kotlinx.coroutines.launch

@Composable
fun AutoLoginTestScreen(onRequestPermission: () -> Unit) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Press the button to run auto-login test.") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = status, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            status = "Running auto-login test..."
            scope.launch {
                LoginTestRunner.run(context.applicationContext)
                status = "Test finished. Check logcat for output."
            }
        }) {
            Text("Run Auto-Login Test")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRequestPermission) {
            Text("Grant Location Permission")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var wifiScanner: WifiScanner

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wifiScanner = WifiScanner(this)

        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allPermissionsGranted = permissions.entries.all { it.value }
            if (!allPermissionsGranted) {
                Toast.makeText(this, "Permissions required for full functionality.", Toast.LENGTH_LONG).show()
            }
        }

        val permissionsNotGranted = permissionsToRequest.filter {
            checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsNotGranted.isNotEmpty()) {
            permissionLauncher.launch(permissionsNotGranted.toTypedArray())
        }

        fun requestLocationPermissionIfNeeded() {
            val permissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (!isGranted) {
                    Toast.makeText(this, "Location permission is required to detect WiFi network.", Toast.LENGTH_LONG).show()
                }
            }

            if (checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        requestLocationPermissionIfNeeded()
        WiFiMonitor.startMonitoring(applicationContext)
        val serviceIntent = Intent(this, MyForegroundService::class.java)
        startForegroundService(serviceIntent)

        setContent {
            AutoNetConnectorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val statsViewModel: StatsViewModel = viewModel(factory = StatsViewModelFactory(application))

                    var selectedTab by remember { mutableIntStateOf(0) }
                    val tabs = listOf("WiFis", "Creds", "Stats", "Login")

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            tabs.forEachIndexed { index, title ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                                    onClick = { selectedTab = index },
                                    selected = selectedTab == index
                                ) {
                                    Text(title)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Crossfade(targetState = selectedTab) { tabIndex ->
                            when (tabIndex) {
                                0 -> WifiScannerScreen(
                                    onRequestPermission = {
                                        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                                    },
                                    onScanRequest = { callback ->
                                        wifiScanner.scanWifiNetworks(callback)
                                    },
                                    wifiScanner = wifiScanner,
                                    modifier = Modifier.fillMaxSize()
                                )
                                1 -> CredentialsScreen(modifier = Modifier.fillMaxSize())
                                2 -> StatsScreen(viewModel = statsViewModel)
                                3 -> AutoLoginTestScreen(
                                    onRequestPermission = {
                                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                                    }
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
                    saveUserCredentials(context, UserCredentials(regNo, password, "DANX5G"))
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