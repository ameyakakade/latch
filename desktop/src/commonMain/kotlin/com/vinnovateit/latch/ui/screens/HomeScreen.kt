package com.vinnovateit.latch.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinnovateit.latch.core.domain.SessionRepository
import com.vinnovateit.latch.core.engine.LatchCommand
import com.vinnovateit.latch.core.engine.LatchController
import com.vinnovateit.latch.core.platform.PlatformServices
import com.vinnovateit.latch.core.settings.SettingsManager
import com.vinnovateit.latch.core.stats.formatBitsPerSecond
import com.vinnovateit.latch.core.stats.formatBytes
import com.vinnovateit.latch.core.stats.formatClockTime
import com.vinnovateit.latch.core.stats.formatDurationDynamic
import com.vinnovateit.latch.core.updater.UpdateState
import com.vinnovateit.latch.core.wifi.ConnectionStatus
import com.vinnovateit.latch.ui.displayText
import com.vinnovateit.latch.ui.theme.ColorGraphDownload
import com.vinnovateit.latch.ui.theme.ColorGraphUpload
import com.vinnovateit.latch.ui.theme.ColorStatusConnected
import com.vinnovateit.latch.ui.theme.ColorStatusDisconnected
import com.vinnovateit.latch.ui.theme.modernizFontFamily
import com.vinnovateit.latch.desktop.resources.Res
import com.vinnovateit.latch.desktop.resources.app_name_uppercase
import com.vinnovateit.latch.desktop.resources.status_connected
import com.vinnovateit.latch.desktop.resources.status_not_connected
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    controller: LatchController,
    sessions: SessionRepository,
    platform: PlatformServices,
    updateState: UpdateState,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: (String) -> Unit,
    onDismissUpdate: () -> Unit,
    onEditCredentials: () -> Unit,
) {
    val isLatched by controller.isLatched.collectAsStateWithLifecycle()
    val status by controller.status.collectAsStateWithLifecycle()
    val liveStatus by sessions.liveStatus.collectAsStateWithLifecycle()
    val speedUnit by SettingsManager.speedUnits.collectAsStateWithLifecycle()

    val latest = liveStatus?.liveData?.lastOrNull()?.usage

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.app_name_uppercase),
            style = MaterialTheme.typography.titleLarge,
            fontFamily = modernizFontFamily(),
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(28.dp))

        // Status dot + label
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (isLatched) ColorStatusConnected else ColorStatusDisconnected
                    )
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = if (isLatched) {
                    stringResource(Res.string.status_connected)
                } else {
                    stringResource(Res.string.status_not_connected)
                },
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(Modifier.height(12.dp))

        // Transient status line
        Box(modifier = Modifier.height(40.dp), contentAlignment = Alignment.Center) {
            when (val current = status) {
                is ConnectionStatus.Connecting -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    LoadingIndicator()
                    Spacer(Modifier.size(12.dp))
                    Text(current.displayText(), style = MaterialTheme.typography.bodyMedium)
                }

                is ConnectionStatus.Idle -> Unit

                else -> Text(
                    text = current.displayText(),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
        ) {
            Column(Modifier.padding(16.dp)) {
                val (rxValue, rxUnit) = formatBitsPerSecond(latest?.rxBps ?: 0L, speedUnit)
                val (txValue, txUnit) = formatBitsPerSecond(latest?.txBps ?: 0L, speedUnit)

                SpeedRow("Download", "$rxValue $rxUnit", ColorGraphDownload)
                Spacer(Modifier.height(8.dp))
                SpeedRow("Upload", "$txValue $txUnit", ColorGraphUpload)

                liveStatus?.let { live ->
                    Spacer(Modifier.height(16.dp))
                    val totalRx = live.liveData.sumOf { it.usage.rxBytes }
                    val totalTx = live.liveData.sumOf { it.usage.txBytes }
                    val (usedValue, usedUnit) = formatBytes(totalRx + totalTx, "B/s")
                    Text(
                        text = "Session: $usedValue $usedUnit • " +
                            formatDurationDynamic(System.currentTimeMillis() - live.startTimeMillis) +
                            " • since ${formatClockTime(live.startTimeMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                controller.submit(
                    if (isLatched) LatchCommand.Logout else LatchCommand.CheckAndLogin
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isLatched) "Disconnect" else "Connect")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { platform.systemActions.openWifiSettings() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open Wi-Fi settings")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                platform.credentials.clear()
                onEditCredentials()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Change credentials")
        }

        Spacer(Modifier.height(8.dp))

        // Update section
        UpdateSection(
            updateState = updateState,
            onCheckForUpdates = onCheckForUpdates,
            onDownloadUpdate = onDownloadUpdate,
            onInstallUpdate = onInstallUpdate,
            onDismissUpdate = onDismissUpdate,
        )

        if (platform.capabilities.supportsAutostart) {
            Spacer(Modifier.height(16.dp))
            var startAtLogin by remember {
                mutableStateOf(platform.systemActions.isAutostartEnabled())
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Start at login", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Run hidden in the tray when Windows starts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = startAtLogin,
                    onCheckedChange = {
                        platform.systemActions.setAutostart(it)
                        startAtLogin = platform.systemActions.isAutostartEnabled()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateSection(
    updateState: UpdateState,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: (String) -> Unit,
    onDismissUpdate: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(12.dp)) {
            when (updateState) {
                is UpdateState.Idle -> {
                    OutlinedButton(
                        onClick = onCheckForUpdates,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Check for Updates")
                    }
                }

                is UpdateState.Checking -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LoadingIndicator(modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(12.dp))
                        Text("Checking for updates...", style = MaterialTheme.typography.bodySmall)
                    }
                }

                is UpdateState.UpdateAvailable -> {
                    Text(
                        text = "Update v${updateState.version} available",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = updateState.releaseNotes.take(200),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onDownloadUpdate,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Download Update")
                    }
                }

                is UpdateState.Downloading -> {
                    Text("Downloading update...", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { updateState.progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    )
                }

                is UpdateState.Downloaded -> {
                    Text(
                        text = "Update downloaded",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onInstallUpdate(updateState.filePath) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Install & Restart")
                        }
                        OutlinedButton(
                            onClick = onDismissUpdate,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Later")
                        }
                    }
                }

                is UpdateState.Error -> {
                    Text(
                        text = "Update failed",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = updateState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onCheckForUpdates,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedRow(label: String, value: String, accent: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.size(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
