package com.vinnovateit.latch.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import com.vinnovateit.latch.core.engine.LatchCommand
import com.vinnovateit.latch.ui.LatchRoot

fun main(args: Array<String>) {
    if (!SingleInstance.acquire()) return

    val startHidden = "--hidden" in args
    val app = LatchApp.create(echoLogsToStdout = System.console() != null || !startHidden)
    app.start()

    application {
        var windowVisible by remember { mutableStateOf(!startHidden) }

        val trayState = rememberTrayState()
        val isLatched by app.engine.isLatched.collectAsState()
        val tooltip by app.notifier.tooltip.collectAsState()
        val updateState by app.updater.state.collectAsState()

        LaunchedEffect(trayState) { app.notifier.trayState = trayState }

        Tray(
            state = trayState,
            icon = remember(isLatched) { LatchTrayIcon(latched = isLatched) },
            tooltip = tooltip,
            onAction = { windowVisible = true },
            menu = {
                if (isLatched) {
                    Item("Disconnect") { app.engine.submit(LatchCommand.Logout) }
                } else {
                    Item("Connect") { app.engine.submit(LatchCommand.CheckAndLogin) }
                }
                Item("Open Latch") { windowVisible = true }
                Separator()
                Item("Quit") {
                    app.shutdown()
                    exitApplication()
                }
            },
        )

        LatchWindow(
            visible = windowVisible,
            onCloseRequest = { windowVisible = false },
        ) {
            val scope = rememberCoroutineScope()
            Surface(modifier = Modifier.fillMaxSize()) {
                LatchRoot(
                    controller = app.engine,
                    sessions = app.sessions,
                    platform = app.platform,
                    updateState = updateState,
                    onCheckForUpdates = { scope.launch { app.updater.check(force = true) } },
                    onDownloadUpdate = { app.installUpdate(onExiting = { exitApplication() }) },
                    onInstallUpdate = { path ->
                        app.updater.installAndExit(path)
                        exitApplication()
                    },
                    onDismissUpdate = { app.updater.dismissUpdate() },
                )
            }
        }
    }
}
