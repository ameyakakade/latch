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
import com.sun.jna.platform.win32.Shell32
import com.sun.jna.WString
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import com.vinnovateit.latch.core.engine.LatchCommand
import com.vinnovateit.latch.ui.LatchRoot

private const val APP_DISPLAY_NAME = "LATCH by VinnovateIT"

private fun configureWindowsAppUserModelId() {
    if (!System.getProperty("os.name").contains("Windows", ignoreCase = true)) return
    runCatching {
        Shell32.INSTANCE.SetCurrentProcessExplicitAppUserModelID(WString(APP_DISPLAY_NAME))
    }
}

fun main(args: Array<String>) {
    if (!SingleInstance.acquire()) return

    val startHidden = "--hidden" in args
    val app = LatchApp.create(echoLogsToStdout = System.console() != null || !startHidden)
    app.start()
    configureWindowsAppUserModelId()

    application {
        var windowVisible by remember { mutableStateOf(!startHidden) }

        val trayState = rememberTrayState()
        val isLatched by app.engine.isLatched.collectAsState()
        val tooltip by app.notifier.tooltip.collectAsState()
        val updateState by app.updater.state.collectAsState()

        LaunchedEffect(trayState) { app.notifier.trayState = trayState }

        Tray(
            state = trayState,
            icon = remember(isLatched) { LatchIcon.forTray(latched = isLatched) },
            tooltip = tooltip,
            onAction = { windowVisible = true },
            menu = {
                // Windows renders this menu with plain java.awt.MenuItem/PopupMenu --
                // no icons, colors, rounding or mnemonics are available through that
                // API at all (java.awt.Menu throws UnsupportedOperationException for
                // a mnemonic), regardless of app theming. This status line is the
                // actual ceiling for what can be polished here.
                Item(
                    text = if (isLatched) "● Connected" else "○ Not connected",
                    enabled = false,
                    onClick = {},
                )
                Separator()
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
                    onDownloadUpdate = { app.downloadUpdate() },
                    onCancelDownload = { app.cancelUpdateDownload() },
                    // Leave only if the installer really started; on a failure
                    // installAndExit has an error for the user to read, which
                    // exiting unconditionally would take down with the process.
                    onInstallUpdate = { path ->
                        if (app.updater.installAndExit(path)) exitApplication()
                    },
                    onDismissUpdate = { app.updater.dismissUpdate() },
                )
            }
        }
    }
}
