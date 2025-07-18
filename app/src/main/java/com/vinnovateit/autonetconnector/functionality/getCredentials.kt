package com.vinnovateit.autonetconnector.functionality

import android.content.Context
import android.util.Log

private const val PREFS_NAME = "user_credentials_cache"
private const val KEY_REGISTRATION = "registrationNumber"
private const val KEY_PASSWORD = "password"
private const val KEY_WIFI_NAME = "wifiName"

// gets from cache
fun getUserCredentials(context: Context): UserCredentials? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val reg = prefs.getString(KEY_REGISTRATION, null)
    val pass = prefs.getString(KEY_PASSWORD, null)

    return if (reg != null && pass != null) {
        UserCredentials(registrationNumber = reg, password = pass)
    } else {
        Log.d("CredentialsStorage", "No credentials found in cache or wifiName missing")
        null
    }
}
