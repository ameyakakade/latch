package com.vinnovateit.latch.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vinnovateit.latch.core.domain.SessionRepository
import com.vinnovateit.latch.core.engine.LatchController
import com.vinnovateit.latch.core.platform.PlatformServices
import com.vinnovateit.latch.core.updater.UpdateState
import com.vinnovateit.latch.ui.screens.CredentialsScreen
import com.vinnovateit.latch.ui.screens.HomeScreen
import com.vinnovateit.latch.ui.theme.LatchTheme

@Composable
fun LatchRoot(
    controller: LatchController,
    sessions: SessionRepository,
    platform: PlatformServices,
    updateState: UpdateState,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: (String) -> Unit,
    onDismissUpdate: () -> Unit,
) {
    LatchTheme {
        var hasCredentials by remember { mutableStateOf(platform.credentials.exists()) }

        if (!hasCredentials) {
            CredentialsScreen(
                onSave = { userId, password ->
                    platform.credentials.save(userId, password)
                    hasCredentials = true
                }
            )
        } else {
            HomeScreen(
                controller = controller,
                sessions = sessions,
                platform = platform,
                updateState = updateState,
                onCheckForUpdates = onCheckForUpdates,
                onDownloadUpdate = onDownloadUpdate,
                onInstallUpdate = onInstallUpdate,
                onDismissUpdate = onDismissUpdate,
                onEditCredentials = { hasCredentials = false },
            )
        }
    }
}
