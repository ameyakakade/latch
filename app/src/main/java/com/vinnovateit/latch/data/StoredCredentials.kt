package com.vinnovateit.latch.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object StoredCredentials {

    private const val PREFS_NAME = "latch_encrypted_credentials"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_PASSWORD = "password"

    private fun getEncryptedPrefs(context: Context): EncryptedSharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }

    fun saveCredentials(context: Context, userId: String, password: String) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    fun getUserId(context: Context): String? {
        val prefs = getEncryptedPrefs(context)
        return prefs.getString(KEY_USER_ID, null)
    }

    fun getPassword(context: Context): String? {
        val prefs = getEncryptedPrefs(context)
        return prefs.getString(KEY_PASSWORD, null)
    }

    fun credentialsExist(context: Context): Boolean {
        return getUserId(context) != null
    }

    fun clearCredentials(context: Context) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit().clear().apply()
    }
}