package com.vinnovateit.autonetconnector.funtionality

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
    val wifi = prefs.getString(KEY_WIFI_NAME, null)  // get stored wifiName

    return if (reg != null && pass != null && wifi != null) {
        UserCredentials(registrationNumber = reg, password = pass, wifiName = wifi)
    } else {
        Log.d("CredentialsStorage", "No credentials found in cache or wifiName missing")
        null
    }
}
