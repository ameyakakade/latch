package com.vinnovateit.latch.desktop.updater

import com.vinnovateit.latch.core.platform.BuildInfo
import com.vinnovateit.latch.core.platform.Logger
import com.vinnovateit.latch.core.updater.UpdateState
import com.vinnovateit.latch.desktop.AppPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val GITHUB_API = "https://api.github.com/repos/vinnovateit/auto-net-connector/releases/latest"
private const val MSI_PATTERN = "Latch-"
private const val PIPE = 32 * 1024

// HttpURLConnection defaults to *no* timeout. On a captive-portal network a
// half-open socket would otherwise park the UI on "Downloading... 0%" forever,
// with no way out but killing the app.
private const val CONNECT_TIMEOUT_MS = 15_000
private const val READ_TIMEOUT_MS = 30_000

private const val USER_AGENT = "Latch-Updater"

@Serializable
private data class GithubRelease(
    val tag_name: String,
    val body: String,
    val assets: List<GithubAsset>,
)

@Serializable
private data class GithubAsset(
    val name: String,
    val browser_download_url: String,
    val size: Long,
)

private data class Semver(val major: Int, val minor: Int, val patch: Int) : Comparable<Semver> {
    override fun compareTo(other: Semver): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
}

class GithubUpdater(
    private val buildInfo: BuildInfo,
    private val logger: Logger,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var lastCheckMs: Long = 0
    private val cooldownMs = 3600_000L

    @Volatile
    private var cancelRequested = false

    /**
     * @param force Bypasses the 1h cooldown. The manual "Check for Updates"
     * button passes true; the silent startup check leaves it false.
     */
    suspend fun check(force: Boolean = false) = withContext(Dispatchers.IO) {
        if (!force && System.currentTimeMillis() - lastCheckMs < cooldownMs) {
            logger.d("GithubUpdater", "Skipping check — within cooldown")
            return@withContext
        }
        _state.value = UpdateState.Checking
        try {
            val release = fetchLatestRelease()
            val msiAsset = release.assets.find { it.name.startsWith(MSI_PATTERN) && it.name.endsWith(".msi") }
            if (msiAsset == null) {
                logger.d("GithubUpdater", "No MSI asset in latest release")
                _state.value = UpdateState.UpToDate
                lastCheckMs = System.currentTimeMillis()
                return@withContext
            }
            val latestTag = release.tag_name.removePrefix("v")
            if (compareVersions(latestTag, buildInfo.versionName) <= 0) {
                logger.d("GithubUpdater", "Already up to date ($latestTag)")
                _state.value = UpdateState.UpToDate
                lastCheckMs = System.currentTimeMillis()
                return@withContext
            }
            _state.value = UpdateState.UpdateAvailable(
                version = latestTag,
                downloadUrl = msiAsset.browser_download_url,
                releaseNotes = release.body,
            )
            lastCheckMs = System.currentTimeMillis()
        } catch (e: Exception) {
            logger.e("GithubUpdater", "Update check failed", e)
            _state.value = UpdateState.Error("Check failed: ${e.message ?: "Unknown error"}")
        }
    }

    /**
     * Fetches the MSI and stops. Installing is deliberately a separate, explicit
     * step: downloading 50-odd MB must not also decide on the user's behalf that
     * now is a good moment to close the app.
     */
    suspend fun download() = withContext(Dispatchers.IO) {
        val current = _state.value
        if (current !is UpdateState.UpdateAvailable) return@withContext
        cancelRequested = false
        _state.value = UpdateState.Downloading(0f)

        val dest = File(AppPaths.updatesDir, "Latch-${current.version}.msi")
        try {
            val conn = open(URL(current.downloadUrl))
            if (conn.responseCode !in 200..299) {
                throw IOException("Download returned HTTP ${conn.responseCode}")
            }
            val total = conn.contentLengthLong
            var written = 0L
            var cancelled = false

            conn.inputStream.use { input ->
                FileOutputStream(dest).use { output ->
                    val buf = ByteArray(PIPE)
                    while (true) {
                        if (cancelRequested) {
                            cancelled = true
                            break
                        }
                        val n = input.read(buf)
                        if (n <= 0) break
                        output.write(buf, 0, n)
                        written += n
                        if (total > 0) {
                            _state.value =
                                UpdateState.Downloading((written.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
            }

            // Deleted only after both streams are closed -- Windows refuses to
            // unlink a file that is still open.
            if (cancelled) {
                dest.delete()
                _state.value = current
                logger.d("GithubUpdater", "Download cancelled")
                return@withContext
            }

            // A connection dropped mid-transfer ends the read loop normally, so
            // without this a truncated MSI would be offered up as installable.
            if (total > 0 && written != total) {
                throw IOException("Incomplete download: got $written of $total bytes")
            }

            _state.value = UpdateState.Downloaded(current.version, dest.absolutePath)
            logger.d("GithubUpdater", "Downloaded update to ${dest.absolutePath}")
        } catch (e: Exception) {
            dest.delete()
            logger.e("GithubUpdater", "Download failed", e)
            _state.value = UpdateState.Error("Download failed: ${e.message ?: "Unknown error"}")
        }
    }

    fun cancelDownload() {
        cancelRequested = true
    }

    /**
     * @return true if msiexec actually started, and therefore whether the caller
     * should exit. Quitting regardless would discard the [UpdateState.Error] set
     * here along with the process that was going to display it.
     */
    fun installAndExit(msiPath: String): Boolean = try {
        ProcessBuilder("msiexec", "/i", msiPath, "/qn").inheritIO().start()
        true
    } catch (e: Exception) {
        logger.e("GithubUpdater", "Failed to launch installer", e)
        _state.value = UpdateState.Error("Failed to launch installer: ${e.message ?: "Unknown error"}")
        false
    }

    /**
     * Removes MSIs left by an earlier run -- a postponed install, or a download
     * whose install never happened. Startup is the only safe moment to do this:
     * at any other point a file here may be one msiexec is mid-way through
     * reading. Each one is ~50 MB, so leaving them to accumulate is not an option.
     */
    fun cleanStaleDownloads() {
        runCatching {
            AppPaths.updatesDir.listFiles()?.forEach { file ->
                if (file.isFile && file.delete()) {
                    logger.d("GithubUpdater", "Removed stale download ${file.name}")
                }
            }
        }
    }

    fun dismissUpdate() {
        _state.value = when (val current = _state.value) {
            is UpdateState.UpdateAvailable -> UpdateState.Dismissed(current.version)
            // The MSI stays on disk and is swept on next launch; re-checking
            // offers it again, at the cost of downloading it a second time.
            is UpdateState.Downloaded -> UpdateState.Dismissed(current.version)
            else -> UpdateState.Idle
        }
    }

    private fun open(url: URL): HttpURLConnection =
        (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
        }

    private fun fetchLatestRelease(): GithubRelease {
        val conn = open(URL(GITHUB_API))
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        val code = conn.responseCode
        if (code != 200) {
            val body = conn.errorStream?.bufferedReader()?.readText() ?: ""
            throw RuntimeException("GitHub API returned $code: $body")
        }
        return json.decodeFromString(conn.inputStream.bufferedReader().readText())
    }

    private fun compareVersions(a: String, b: String): Int {
        val sa = parseSemver(a) ?: return a.compareTo(b)
        val sb = parseSemver(b) ?: return a.compareTo(b)
        return sa.compareTo(sb)
    }

    private fun parseSemver(v: String): Semver? {
        val parts = v.split('.')
        if (parts.size != 3) return null
        val major = parts[0].toIntOrNull() ?: return null
        val minor = parts[1].toIntOrNull() ?: return null
        val patch = parts[2].toIntOrNull() ?: return null
        return Semver(major, minor, patch)
    }
}
