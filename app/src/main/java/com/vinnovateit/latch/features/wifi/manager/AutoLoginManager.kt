package com.vinnovateit.latch.features.wifi.manager

import android.net.Network
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

sealed class LoginResult {
    object Success : LoginResult()
    object Failure : LoginResult()
    object UnsupportedNetwork : LoginResult()
}
object AutoLoginManager {

    private const val LOGIN_URL =
        "http://phc.prontonetworks.com/cgi-bin/authlogin?URI=http://example.com"
    private const val LOGOUT_URL = "http://phc.prontonetworks.com/cgi-bin/authlogout"

    fun isTargetCaptivePortal(network: Network?): Boolean {
        return try {
            val url = URL(LOGIN_URL)
            val connection = (network?.openConnection(url) ?: url.openConnection()) as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 3000 // Short timeout for a quick check
            connection.readTimeout = 3000
            connection.requestMethod = "GET"
            connection.connect()
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode == HttpURLConnection.HTTP_OK // Success is ONLY a 200 response
        } catch (e: Exception) {
            Log.d("AutoLoginManager", "Target portal check failed: ${e.message}")
            false
        }
    }

    fun attemptLogin(userId: String, password: String, network: Network? = null): LoginResult {
        val openConnection: (URL) -> HttpURLConnection = { url ->
            (network?.openConnection(url) ?: url.openConnection()) as HttpURLConnection
        }

        return try {
            val loginUrl = URL(LOGIN_URL)
            val connection = openConnection(loginUrl)
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.instanceFollowRedirects = false // Do NOT follow redirects
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val postData = "userId=${URLEncoder.encode(userId, "UTF-8")}" +
              "&password=${URLEncoder.encode(password, "UTF-8")}" +
              "&serviceName=ProntoAuthentication"

            connection.outputStream.bufferedWriter().use { it.write(postData) }

            when (val responseCode = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> { // 200
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val isSuccess = "Access Granted" in response || "You have successfully connected" in response || "already logged in" in response.lowercase()
                    if (isSuccess) LoginResult.Success else LoginResult.Failure
                }
                HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP -> { // 301, 302
                    Log.d("AutoLoginManager", "Login resulted in a redirect ($responseCode). Assuming unsupported network.")
                    LoginResult.UnsupportedNetwork
                }
                else -> {
                    Log.w("AutoLoginManager", "Login failed with unexpected response code: $responseCode")
                    LoginResult.Failure
                }
            }
        } catch (e: Exception) {
            Log.e("AutoLoginManager", "Login failed with exception", e)
            LoginResult.Failure
        }
    }

    fun attemptLogout(): Boolean {
        return try {
            val url = URL(LOGOUT_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"               // CHANGED from POST -> GET
            connection.instanceFollowRedirects = false     // Don't auto-follow; we just care that it responded
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()

            val code = connection.responseCode
            Log.d("AutoLoginManager", "Logout response code: $code")

            // Drain response to avoid leaked connections
            try {
                (if (code >= 400) connection.errorStream else connection.inputStream)
                    ?.buffered()?.use { it.readBytes() }
            } catch (_: Exception) { /* ignore */ }

            connection.disconnect()
            code in 200..399
        } catch (e: Exception) {
            Log.e("AutoLoginManager", "Logout failed: ${e.message}")
            false
        }
    }

}