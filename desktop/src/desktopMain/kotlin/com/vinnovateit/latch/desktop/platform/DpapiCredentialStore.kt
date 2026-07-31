package com.vinnovateit.latch.desktop.platform

import com.sun.jna.platform.win32.Crypt32Util
import com.vinnovateit.latch.core.platform.CredentialStore
import com.vinnovateit.latch.core.platform.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
private data class StoredCreds(val userId: String, val password: String)

/**
 * Credential storage backed by Windows DPAPI.
 *
 * DPAPI derives its key from the logged-in Windows user's credentials, so the
 * blob is useless on another machine or under another Windows account. This is
 * the closest desktop equivalent to Android's EncryptedSharedPreferences, which
 * has no desktop port.
 *
 * Rejected alternatives: a Java KeyStore has a chicken-and-egg problem (the
 * keystore password would itself have to be stored somewhere), and Credential
 * Manager is more JNA surface for no security gain.
 *
 * THREAT MODEL, stated plainly: any code running as the same Windows user can
 * decrypt this. That is the same guarantee Chrome gives saved passwords. It
 * protects against disk theft and other user accounts on the machine; it does
 * not protect against malware already running as you.
 */
class DpapiCredentialStore(
    private val file: File,
    private val logger: Logger,
) : CredentialStore {

    private companion object {
        const val TAG = "DpapiCredentialStore"
    }

    private val json = Json { ignoreUnknownKeys = true }

    // Cached so a latch attempt doesn't hit the disk and DPAPI twice.
    private var cache: StoredCreds? = null

    override fun save(userId: String, password: String) {
        try {
            val plain = json.encodeToString(StoredCreds(userId, password)).toByteArray(Charsets.UTF_8)
            val encrypted = Crypt32Util.cryptProtectData(plain)
            file.parentFile?.mkdirs()
            file.writeBytes(encrypted)
            cache = StoredCreds(userId, password)
        } catch (e: Throwable) {
            logger.e(TAG, "Failed to save credentials", e)
        }
    }

    private fun read(): StoredCreds? {
        cache?.let { return it }
        if (!file.exists()) return null
        return try {
            val decrypted = Crypt32Util.cryptUnprotectData(file.readBytes())
            json.decodeFromString<StoredCreds>(decrypted.toString(Charsets.UTF_8))
                .also { cache = it }
        } catch (e: Throwable) {
            // Mirrors Android's corruption recovery: a blob we cannot decrypt
            // (migrated Windows profile, changed account) is deleted so the user
            // is routed back to the credentials screen rather than being stuck.
            logger.e(TAG, "Credential blob unreadable; clearing it", e)
            runCatching { file.delete() }
            null
        }
    }

    override fun userId(): String? = read()?.userId

    override fun password(): String? = read()?.password

    override fun exists(): Boolean = read() != null

    override fun clear() {
        cache = null
        runCatching { file.delete() }
    }
}
