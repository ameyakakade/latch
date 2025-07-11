package com.vinnovateit.autonetconnector

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vinnovateit.autonetconnector.functionality.*
import com.vinnovateit.autonetconnector.ui.theme.AutoNetConnectorTheme

class MainActivity : ComponentActivity() {

    private lateinit var wifiScanner: WifiScanner

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wifiScanner = WifiScanner(this)

        setContent {
            AutoNetConnectorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var selectedTab by remember { mutableStateOf(0) }
                    val tabs = listOf("WiFi Scanner", "Credentials Debug")

                    var hasPermission by remember { mutableStateOf(wifiScanner.hasPermission()) }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
                        val nearbyWifiGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissions[android.Manifest.permission.NEARBY_WIFI_DEVICES] == true
                        } else true

                        hasPermission = fineLocationGranted && nearbyWifiGranted
                        Log.d("DEBUG", "Permission result: fineLocation=$fineLocationGranted, nearbyWifi=$nearbyWifiGranted")
                        Toast.makeText(this, "Permissions granted: $hasPermission", Toast.LENGTH_SHORT).show()
                        if (!hasPermission) {
                            Toast.makeText(this, "Permissions required for WiFi scanning", Toast.LENGTH_LONG).show()
                        }
                    }

                    // Update permission state on resume
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                hasPermission = wifiScanner.hasPermission()
                                Log.d("DEBUG", "ON_RESUME: hasPermission=$hasPermission")
                                Toast.makeText(this@MainActivity, "App resumed. Permission: $hasPermission", Toast.LENGTH_SHORT).show()
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

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
                                    onClick = {
                                        selectedTab = index
                                        Log.d("DEBUG", "Tab switched to: $title")
                                        Toast.makeText(this@MainActivity, "Tab: $title", Toast.LENGTH_SHORT).show()
                                    },
                                    text = { Text(title) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(modifier = Modifier.fillMaxSize()) {
                            when (selectedTab) {
                                0 -> WifiScannerScreen(
                                    onRequestPermission = {
                                        val permissions = mutableListOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            permissions.add(android.Manifest.permission.NEARBY_WIFI_DEVICES)
                                        }
                                        Log.d("DEBUG", "Requesting permissions: $permissions")
                                        Toast.makeText(this@MainActivity, "Requesting permissions...", Toast.LENGTH_SHORT).show()
                                        permissionLauncher.launch(permissions.toTypedArray())
                                    },
                                    onScanRequest = { callback ->
                                        Log.d("DEBUG", "Calling scanWifiNetworks()")
                                        Toast.makeText(this@MainActivity, "Triggering WiFi scan...", Toast.LENGTH_SHORT).show()
                                        wifiScanner.scanWifiNetworks(callback)
                                    },
                                    hasPermission = hasPermission,
                                    selectedTab = selectedTab,
                                    modifier = Modifier.fillMaxSize()
                                )
                                1 -> CredentialsScreen(modifier = Modifier.fillMaxSize())
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
    hasPermission: Boolean,
    selectedTab: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var wifiList by remember { mutableStateOf<List<WifiEntry>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var scanCompleted by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Trigger scan on tab switch or app resume
    LaunchedEffect(selectedTab, hasPermission) {
        Log.d("DEBUG", "LaunchedEffect: selectedTab=$selectedTab, hasPermission=$hasPermission")
        if (selectedTab == 0 && hasPermission) {
            isScanning = true
            Toast.makeText(context, "Auto-trigger scan (tab/resume)", Toast.LENGTH_SHORT).show()
            onScanRequest { results ->
                Log.d("DEBUG", "Scan results received: ${results.size}")
                Toast.makeText(context, "Scan results: ${results.size}", Toast.LENGTH_SHORT).show()
                wifiList = results
                isScanning = false
                scanCompleted = true
            }
        }
    }

    // Also trigger scan when app resumes and WiFi tab is selected
    DisposableEffect(lifecycleOwner, selectedTab, hasPermission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && selectedTab == 0 && hasPermission) {
                Log.d("DEBUG", "LifecycleEventObserver: ON_RESUME, triggering scan")
                Toast.makeText(context, "App resumed - triggering scan", Toast.LENGTH_SHORT).show()
                isScanning = true
                onScanRequest { results ->
                    Log.d("DEBUG", "Scan results received (resume): ${results.size}")
                    Toast.makeText(context, "Scan results (resume): ${results.size}", Toast.LENGTH_SHORT).show()
                    wifiList = results
                    isScanning = false
                    scanCompleted = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
                    Log.d("DEBUG", "Button: Permission not granted, requesting")
                    Toast.makeText(context, "Permission not granted, requesting...", Toast.LENGTH_SHORT).show()
                    onRequestPermission()
                } else {
                    Log.d("DEBUG", "Button: Permission granted, scanning")
                    Toast.makeText(context, "Button triggered scan", Toast.LENGTH_SHORT).show()
                    isScanning = true
                    onScanRequest { results ->
                        Log.d("DEBUG", "Button scan results: ${results.size}")
                        Toast.makeText(context, "Button scan results: ${results.size}", Toast.LENGTH_SHORT).show()
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
                    saveUserCredentials(context, UserCredentials(regNo, password))
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
