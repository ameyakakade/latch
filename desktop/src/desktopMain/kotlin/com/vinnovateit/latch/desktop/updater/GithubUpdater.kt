package com.vinnovateit.latch.desktop.updater

import com.vinnovateit.latch.core.platform.BuildInfo
import com.vinnovateit.latch.core.platform.Logger
import com.vinnovateit.latch.core.updater.UpdateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

private const val GITHUB_API = "https://api.github.com/repos/vinnovateit/auto-net-connector/releases/latest"
private const val MSI_PATTERN = "Latch-"
private const val PIPE = 32 * 1024

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

    suspend fun download() = withContext(Dispatchers.IO) {
        val current = _state.value
        if (current !is UpdateState.UpdateAvailable) return@withContext
        _state.value = UpdateState.Downloading(0f)
        try {
            val dest = File.createTempFile("latch-update-", ".msi").apply { deleteOnExit() }
            val conn = URL(current.downloadUrl).openConnection() as HttpURLConnection
            conn.connect()
            val total = conn.contentLengthLong
            val input = conn.inputStream
            val raf = RandomAccessFile(dest, "rw")
            raf.setLength(total.coerceAtLeast(0))
            val buf = ByteArray(PIPE)
            var written = 0L
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                raf.write(buf, 0, n)
                written += n
                if (total > 0) {
                    _state.value = UpdateState.Downloading((written.toFloat() / total).coerceIn(0f, 1f))
                }
            }
            raf.close()
            input.close()
            _state.value = UpdateState.Downloaded(dest.absolutePath)
            logger.d("GithubUpdater", "Downloaded update to ${dest.absolutePath}")
        } catch (e: Exception) {
            logger.e("GithubUpdater", "Download failed", e)
            _state.value = UpdateState.Error("Download failed: ${e.message ?: "Unknown error"}")
        }
    }

    fun installAndExit(msiPath: String) {
        try {
            ProcessBuilder("msiexec", "/i", msiPath, "/qn").inheritIO().start()
        } catch (e: Exception) {
            logger.e("GithubUpdater", "Failed to launch installer", e)
            _state.value = UpdateState.Error("Failed to launch installer: ${e.message ?: "Unknown error"}")
        }
    }

    fun dismissUpdate() {
        _state.value = UpdateState.Idle
    }

    private fun fetchLatestRelease(): GithubRelease {
        val conn = URL(GITHUB_API).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.connect()
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
