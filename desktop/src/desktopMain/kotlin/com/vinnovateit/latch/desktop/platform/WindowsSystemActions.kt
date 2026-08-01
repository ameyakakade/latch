package com.vinnovateit.latch.desktop.platform

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.Shell32
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
        const val SW_SHOWNORMAL = 1
        const val SHELL_EXECUTE_ERROR_MAX = 32L
    }

    /**
     * Opens the Wi-Fi network flyout, which is the closest analogue of Android's
     * Settings.Panel.ACTION_WIFI. Falls back to the full settings page.
     *
     * These are shell protocol URIs, not web URLs, so they must go through
     * ShellExecute. Desktop.browse() hands anything it is given to the *default
     * browser*, which launches Chrome on "ms-availablenetworks:" and then sits
     * there -- and reports success while doing it, so a browse-first attempt
     * also swallows the fallback.
     */
    override fun openWifiSettings() {
        val targets = listOf("ms-availablenetworks:", "ms-settings:network-wifi")
        for (target in targets) {
            if (shellExecute(target)) return
        }
        logger.e(TAG, "Could not open Wi-Fi settings", null)
    }

    /**
     * @return whether the shell accepted the target. ShellExecute returns a
     * pseudo-HINSTANCE that is an error code when <= 32; anything above that is
     * a real launch.
     */
    private fun shellExecute(target: String): Boolean = runCatching {
        val code = Shell32.INSTANCE
            .ShellExecute(null, "open", target, null, null, SW_SHOWNORMAL)
            .toLong()
        if (code <= SHELL_EXECUTE_ERROR_MAX) {
            logger.w(TAG, "ShellExecute('$target') failed with code $code")
            false
        } else {
            true
        }
    }.getOrElse {
        logger.e(TAG, "ShellExecute('$target') threw", it)
        false
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
