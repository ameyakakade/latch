package com.vinnovateit.autonetconnector.functionality

import android.content.Context
import android.util.Log

private const val PREFS_NAME = "user_credentials_cache"
private const val KEY_REGISTRATION = "registrationNumber"
private const val KEY_PASSWORD = "password"
private const val KEY_WIFI_NAME = "wifiName"

fun saveUserCredentials(context: Context, credentials: UserCredentials) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().apply {
        putString(KEY_REGISTRATION, credentials.registrationNumber)
        putString(KEY_PASSWORD, credentials.password)
        putString(KEY_WIFI_NAME, credentials.wifiName)  // Hardcoded WiFi name saved here
        apply()
    }
    Log.d("CredentialsStorage", "Credentials saved: $credentials")
}