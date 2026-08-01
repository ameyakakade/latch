package com.vinnovateit.latch.desktop.platform

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import com.vinnovateit.latch.core.platform.Logger
import com.vinnovateit.latch.core.platform.SystemActions
import java.awt.Desktop
import java.net.URI

/**
 * Windows implementations of the OS actions Latch needs.
 */
class WindowsSystemActions(private val logger: Logger) : SystemActions {

    private companion object {
        const val TAG = "WindowsSystemActions"
        const val RUN_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run"
        const val RUN_VALUE_NAME = "Latch"
        const val EXE_NAME = "Latch.exe"
    }

    /**
     * Opens the Wi-Fi network flyout, which is the closest analogue of Android's
     * Settings.Panel.ACTION_WIFI. Falls back to the full settings page, then to
     * a shell invocation, because Desktop.browse on an ms-* URI throws on some
     * configurations.
     */
    override fun openWifiSettings() {
        val targets = listOf("ms-availablenetworks:", "ms-settings:network-wifi")
        for (target in targets) {
            if (runCatching { Desktop.getDesktop().browse(URI(target)) }.isSuccess) return
        }
        runCatching {
            ProcessBuilder("cmd", "/c", "start", "", "ms-settings:network-wifi").start()
        }.onFailure { logger.e(TAG, "Could not open Wi-Fi settings", it) }
    }

    override fun openUrl(url: String) {
        runCatching { Desktop.getDesktop().browse(URI(url)) }
            .onFailure { logger.e(TAG, "Could not open URL: $url", it) }
    }

    /**
     * Autostart via the HKCU Run key using JNA rather than shelling out to
     * `reg add`. The registry value must be `"<path>" --hidden` *including* the
     * inner quotes, and Java's Windows command-line assembly mangles embedded
     * quotes in ways that vary by JDK -- JNA avoids that layer entirely.
     */
    override fun setAutostart(enabled: Boolean) {
        val exe = appExePath()
        if (exe == null) {
            logger.w(TAG, "Not running from an installed $EXE_NAME; refusing to set autostart.")
            return
        }
        try {
            if (enabled) {
                Advapi32Util.registrySetStringValue(
                    WinReg.HKEY_CURRENT_USER, RUN_KEY, RUN_VALUE_NAME, "\"$exe\" --hidden",
                )
                logger.d(TAG, "Autostart enabled -> $exe")
            } else if (isAutostartEnabled()) {
                Advapi32Util.registryDeleteValue(
                    WinReg.HKEY_CURRENT_USER, RUN_KEY, RUN_VALUE_NAME,
                )
                logger.d(TAG, "Autostart disabled")
            }
        } catch (e: Throwable) {
            logger.e(TAG, "Failed to update autostart", e)
        }
    }

    /** Reads actual OS state rather than a mirrored flag that could drift. */
    override fun isAutostartEnabled(): Boolean = try {
        Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, RUN_KEY, RUN_VALUE_NAME)
    } catch (e: Throwable) {
        logger.e(TAG, "Failed to read autostart state", e)
        false
    }

    /**
     * Resolves the path to register for autostart, or null if this process must
     * not register itself. See [InstalledBuild] for why build-output locations
     * are rejected explicitly.
     */
    private fun appExePath(): String? {
        val exe = InstalledBuild.path
        if (exe == null) {
            logger.w(TAG, "Not running from an installed $EXE_NAME; refusing to set autostart.")
        }
        return exe
    }
}
