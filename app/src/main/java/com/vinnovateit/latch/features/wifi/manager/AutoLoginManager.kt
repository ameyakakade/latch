package com.vinnovateit.latch.features.wifi.manager

import android.net.Network
import android.util.Log
import com.vinnovateit.latch.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

sealed class LoginResult {
    object Success : LoginResult()
    object Failure : LoginResult()
    object UnsupportedNetwork : LoginResult()
}

object AutoLoginManager {

    private const val LOGIN_URL = "http://phc.prontonetworks.com/cgi-bin/authlogin?URI=http://example.com"
    private const val LOGOUT_URL = "http://phc.prontonetworks.com/cgi-bin/authlogout"

    private const val SECURE_LOGIN_URL = "https://phc.prontonetworks.com/cgi-bin/authlogin?URI=http://example.com"
    private const val SECURE_LOGOUT_URL = "https://phc.prontonetworks.com/cgi-bin/authlogout"

    private const val TAG = "AutoLoginManager"

    // Helper functions for conditional logging
    private fun logDebug(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    private fun logError(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (throwable != null) Log.e(TAG, message, throwable) else Log.e(TAG, message)
        }
    }

    private fun logWarning(message: String) {
        if (BuildConfig.DEBUG) Log.w(TAG, message)
    }

    fun isTargetCaptivePortal(network: Network?): Boolean {
        logDebug("Starting target captive portal check...")
        return try {
            val url = URL(LOGIN_URL)
            logDebug("Opening HTTP connection to: $LOGIN_URL")
            val connection = (network?.openConnection(url) ?: url.openConnection()) as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.requestMethod = "GET"

            logDebug("Connecting to captive portal check endpoint...")
            connection.connect()

            val responseCode = connection.responseCode
            logDebug("Captive portal check returned HTTP response code: $responseCode")
            connection.disconnect()

            val isTarget = responseCode == HttpURLConnection.HTTP_OK
            logDebug("Is target captive portal? $isTarget")
            isTarget
        } catch (e: Exception) {
            logError("Target portal check failed with exception: ${e.message}", e)
            false
        }
    }

    fun attemptLogin(userId: String, password: String, network: Network? = null, useAlternate: Boolean = false): LoginResult {
        logDebug("Initiating login attempt for user: $userId (useAlternate=$useAlternate)")
        val openConnection: (URL) -> HttpURLConnection = { url ->
            (network?.openConnection(url) ?: url.openConnection()) as HttpURLConnection
        }

        return try {
            val targetUrl = if (useAlternate) SECURE_LOGIN_URL else LOGIN_URL
            val loginUrl = URL(targetUrl)
            logDebug("Preparing POST request to: $targetUrl")

            val connection = openConnection(loginUrl)
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.instanceFollowRedirects = false

            logDebug("Setting connection headers and timeouts...")
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            logDebug("Encoding credentials and building POST payload...")
            val postData = "userId=${URLEncoder.encode(userId, "UTF-8")}" +
                    "&password=${URLEncoder.encode(password, "UTF-8")}" +
                    "&serviceName=ProntoAuthentication"

            logDebug("Writing POST payload to output stream...")
            connection.outputStream.bufferedWriter().use { it.write(postData) }

            logDebug("Awaiting response from portal...")
            when (val responseCode = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    logDebug("Received 200 OK. Reading response body...")
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    logDebug("Response body length: ${response.length} chars")

                    val isSuccess = "Access Granted" in response || "You have successfully connected" in response || "already logged in" in response.lowercase()
                    logDebug("Login success evaluation string match: $isSuccess")

                    if (isSuccess) LoginResult.Success else LoginResult.Failure
                }
                HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP -> {
                    logWarning("Login resulted in a redirect ($responseCode). Assuming unsupported network.")
                    LoginResult.UnsupportedNetwork
                }
                else -> {
                    logWarning("Login failed with unexpected response code: $responseCode")
                    LoginResult.Failure
                }
            }
        } catch (e: Exception) {
            logError("Login failed with exception: ${e.message}", e)
            LoginResult.Failure
        }
    }

    fun attemptLogout(useAlternate: Boolean = false): Boolean {
        logDebug("Initiating logout attempt (useAlternate=$useAlternate)")
        return try {
            val targetUrl = if (useAlternate) SECURE_LOGOUT_URL else LOGOUT_URL
            val url = URL(targetUrl)
            logDebug("Opening connection to: $targetUrl")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            logDebug("Connecting to logout endpoint...")
            connection.connect()

            val code = connection.responseCode
            logDebug("Logout returned response code: $code")

            logDebug("Draining response streams to prevent connection leaks...")
            try {
                (if (code >= 400) connection.errorStream else connection.inputStream)
                    ?.buffered()?.use { it.readBytes() }
            } catch (e: Exception) {
                logDebug("Stream drain exception (ignored): ${e.message}")
            }

            connection.disconnect()
            val success = code in 200..399
            logDebug("Logout success evaluation: $success")
            success
        } catch (e: Exception) {
            logError("Logout failed with exception: ${e.message}", e)
            false
        }
    }
}