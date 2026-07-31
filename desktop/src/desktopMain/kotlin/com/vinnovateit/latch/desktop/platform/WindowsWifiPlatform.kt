package com.vinnovateit.latch.desktop.platform

import com.vinnovateit.latch.core.platform.Logger
import com.vinnovateit.latch.core.platform.NetworkHandle
import com.vinnovateit.latch.core.platform.WifiEvent
import com.vinnovateit.latch.core.platform.WifiPlatform
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit

internal data class SimpleNetworkHandle(override val id: String) : NetworkHandle

/**
 * Windows Wi-Fi state via PowerShell.
 *
 * Why PowerShell and not `netsh wlan show interfaces`: netsh's output labels are
 * localized, so any key-based regex over it breaks on non-English Windows.
 * PowerShell cmdlet *property* names are not localized, so Get-NetConnectionProfile
 * and friends are locale-safe.
 *
 * Cost is ~200-400ms per invocation, hence the cache and the 5s poll floor. The
 * eventual upgrade is JNA against wlanapi.dll (WlanQueryInterface +
 * WlanRegisterNotification), which is both faster and push-based.
 */
class WindowsWifiPlatform(private val logger: Logger) : WifiPlatform {

    private companion object {
        const val TAG = "WindowsWifiPlatform"
        const val POLL_INTERVAL_MS = 5_000L
        const val CACHE_TTL_MS = 3_000L
        const val PS_TIMEOUT_SEC = 10L
    }

    private data class WifiSnapshot(
        val adapterName: String?,
        val adapterUp: Boolean,
        val ssid: String?,
        val gateway: String?,
    )

    private var cached: WifiSnapshot? = null
    private var cachedAt: Long = 0

    private fun snapshot(): WifiSnapshot {
        val now = System.currentTimeMillis()
        cached?.let { if (now - cachedAt < CACHE_TTL_MS) return it }

        // One PowerShell round-trip for all four facts, pipe-separated. Selecting
        // explicit properties keeps this independent of display formatting, and
        // cmdlet property names are not localized (unlike netsh's output labels).
        val script = """
            ${'$'}ErrorActionPreference = 'SilentlyContinue'
            ${'$'}a = Get-NetAdapter -Physical | Where-Object { ${'$'}_.PhysicalMediaType -match '802.11' } | Select-Object -First 1
            ${'$'}name = if (${'$'}a) { ${'$'}a.Name } else { '' }
            ${'$'}up = if (${'$'}a -and ${'$'}a.Status -eq 'Up') { '1' } else { '0' }
            ${'$'}ssid = ''
            if (${'$'}name) { ${'$'}p = Get-NetConnectionProfile -InterfaceAlias ${'$'}name; if (${'$'}p) { ${'$'}ssid = ${'$'}p.Name } }
            ${'$'}gw = ''
            if (${'$'}name) {
              ${'$'}r = Get-NetRoute -InterfaceAlias ${'$'}name -DestinationPrefix '0.0.0.0/0' | Select-Object -First 1
              if (${'$'}r) { ${'$'}gw = ${'$'}r.NextHop }
            }
            Write-Output ("RESULT|" + ${'$'}name + "|" + ${'$'}up + "|" + ${'$'}ssid + "|" + ${'$'}gw)
        """.trimIndent()

        val result = runPowerShell(script)
        val snap = if (result == null) {
            WifiSnapshot(null, false, null, null)
        } else {
            // Drop the RESULT marker, keeping field indices aligned.
            val parts = result.removePrefix("RESULT|").split('|')
            WifiSnapshot(
                adapterName = parts.getOrNull(0)?.trim()?.takeIf { it.isNotEmpty() },
                adapterUp = parts.getOrNull(1)?.trim() == "1",
                ssid = parts.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() },
                gateway = parts.getOrNull(3)?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
        cached = snap
        cachedAt = now
        return snap
    }

    /**
     * Runs a script via -EncodedCommand.
     *
     * Passing a multi-line script through -Command does not survive
     * ProcessBuilder: Windows command-line assembly mangles the newlines and
     * quoting, and the observed failure mode is silent -- the process exits 0
     * having done nothing, so every field comes back empty. -EncodedCommand takes
     * base64 UTF-16LE and is immune to all of that.
     */
    private fun runPowerShell(script: String): String? {
        return try {
            val encoded = java.util.Base64.getEncoder()
                .encodeToString(script.toByteArray(Charsets.UTF_16LE))

            val process = ProcessBuilder(
                "powershell.exe", "-NoProfile", "-NonInteractive",
                "-ExecutionPolicy", "Bypass", "-EncodedCommand", encoded,
            ).redirectErrorStream(true).start()

            // Close stdin so the child never blocks waiting on input.
            process.outputStream.close()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            if (!process.waitFor(PS_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                logger.w(TAG, "PowerShell query timed out")
                return null
            }
            val line = output.lineSequence()
                .map { it.trim() }
                .firstOrNull { it.startsWith("RESULT|") }
            if (line == null) {
                logger.w(TAG, "PowerShell returned no RESULT line. Raw output: ${output.take(400)}")
            }
            line
        } catch (e: Throwable) {
            logger.e(TAG, "PowerShell query failed", e)
            null
        }
    }

    override fun isWifiEnabled(): Boolean = snapshot().adapterName != null

    override fun isConnectedToWifi(): Boolean {
        val snap = snapshot()
        return snap.adapterUp && snap.ssid != null
    }

    override fun currentSsid(): String? = snapshot().ssid

    override fun gatewayIp(): String? = snapshot().gateway

    override fun activeHandle(): NetworkHandle? =
        snapshot().takeIf { it.adapterUp }?.adapterName?.let { SimpleNetworkHandle(it) }

    /** The Wi-Fi adapter's interface name, used by the OSHI counter source. */
    fun wifiInterfaceName(): String? = snapshot().adapterName

    /**
     * The local IPv4 address of the Wi-Fi interface. Needed for the eventual
     * bound-socket transport that fixes multi-homed routing.
     */
    fun wifiLocalAddress(): java.net.InetAddress? {
        val name = wifiInterfaceName() ?: return null
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .firstOrNull { it.displayName == name || it.name == name }
                ?.inetAddresses?.toList()
                ?.firstOrNull { !it.isLoopbackAddress && it is java.net.Inet4Address }
        } catch (e: Throwable) {
            logger.e(TAG, "Failed to resolve Wi-Fi local address", e)
            null
        }
    }

    /**
     * Windows has no ConnectivityManager.NetworkCallback, so events are
     * synthesised by polling and diffing. Upgrade path is WlanRegisterNotification.
     */
    override val events: Flow<WifiEvent> = flow {
        // Baseline from the current state, not null: otherwise the first poll
        // synthesises an Available for a network that was already connected
        // before we started listening. That event races the startup
        // CheckAndLogin command (see LatchApp.start) into a duplicate credential
        // POST -- the "first connection always fails" bug. The startup command
        // already probes this case, so nothing is lost by seeding the baseline.
        val seed = snapshot()
        var lastKey = if (seed.adapterUp && seed.ssid != null) {
            "${seed.adapterName}::${seed.ssid}"
        } else {
            null
        }

        while (true) {
            val snap = snapshot()
            val key = if (snap.adapterUp && snap.ssid != null) {
                "${snap.adapterName}::${snap.ssid}"
            } else {
                null
            }

            if (key != lastKey) {
                if (lastKey != null) {
                    emit(WifiEvent.Lost(SimpleNetworkHandle(lastKey.substringBefore("::"))))
                }
                if (key != null) {
                    emit(WifiEvent.Available(SimpleNetworkHandle(snap.adapterName!!)))
                }
                lastKey = key
            }
            delay(POLL_INTERVAL_MS)
        }
    }
}
