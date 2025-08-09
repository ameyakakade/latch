package com.vinnovateit.latch.features.wifi.manager

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object AutoLoginManager {

    private const val LOGIN_URL =
        "http://phc.prontonetworks.com/cgi-bin/authlogin?URI=http://example.com"
    private const val LOGOUT_URL = "http://phc.prontonetworks.com/cgi-bin/authlogout"
    // 🔐 Paste the actual working cookie string here
    private const val COOKIES = "initialTrafficSource=utmccn=(not set); _lfa=LF1.1.6760c938859dd81e.1732884047410; intercom-device-id-bvjju1cs=49a3cfec-e4f0-4edd-a4e6-96dd2aac11cb; intercom-id-bvjju1cs=c2f37909-32a6-42b7-8b19-1b33fae2d8ef; _ga_MZLP6C1YKB=GS1.1.1740861389.15.1.1740861417.32.0.0; _ga_S2ZFRTKW03=GS1.1.1740861417.5.0.1740861417.0.0.0; _ga=GA1.1.526847149.1732884043"

    fun attemptLogin(userId: String, password: String): Boolean {
        return try {
            // Step 1: Acknowledge Modal (simulate with a GET)
            val modalUrl = "http://phc.prontonetworks.com/cgi-bin/authlogin?URI=http://example.com"
            val modalConnection = URL(modalUrl).openConnection() as HttpURLConnection
            modalConnection.connectTimeout = 4000
            modalConnection.readTimeout = 4000
            modalConnection.requestMethod = "GET"
            modalConnection.connect()
            modalConnection.inputStream.close()
            Log.d("AutoLoginManager", "Modal acknowledged.")

            // Step 2: Submit Login Form
            val loginUrl = URL(LOGIN_URL)
            val connection = loginUrl.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
            connection.setRequestProperty("Referer", LOGIN_URL)
            Log.d("AutoLoginManager", "Headers → Content-Type: application/x-www-form-urlencoded, Referer: $LOGIN_URL")

            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val postData = "userId=${URLEncoder.encode(userId, "UTF-8")}" +
                    "&password=${URLEncoder.encode(password, "UTF-8")}" +
                    "&serviceName=ProntoAuthentication"

            Log.d("AutoLoginManager", "Target URL: $LOGIN_URL")

            Log.d("AutoLoginManager", "POST Data → $postData")

            connection.outputStream.bufferedWriter().use { it.write(postData) }

            val responseCode = connection.responseCode
            Log.d("AutoLoginManager", "Login response code: $responseCode")

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            Log.d("AutoLoginManager", "Response Body:\n$response")
            val isSuccess = "Access Granted" in response || "You have successfully connected" in response

            if (isSuccess) {
                Log.d("AutoLoginManager", "✅ Login page confirmed success")

                // 🔁 Trigger Android to detect portal is gone by pinging Google's connectivity check
                try {
                    val checkUrl = URL("http://connectivitycheck.gstatic.com/generate_204")
                    val checkConn = checkUrl.openConnection() as HttpURLConnection
                    checkConn.instanceFollowRedirects = false
                    checkConn.connectTimeout = 2000
                    checkConn.readTimeout = 2000
                    val code = checkConn.responseCode
                    Log.d("AutoLoginManager", "ConnectivityCheck response: $code")
                } catch (e: Exception) {
                    Log.e("AutoLoginManager", "ConnectivityCheck failed: ${e.message}")
                }

                return true
            }
            else {
                Log.d("AutoLoginManager", "⚠️ Login response may not be successful")
                false
            }

        } catch (e: Exception) {
            Log.e("AutoLoginManager", "Login failed: ${e.message}")
            false
        }
    }

    /**
     * Attempts to log out by making a GET request to the logout URL.
     */
    fun attemptLogout(): Boolean {
        return try {
            val url = URL(LOGOUT_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()

            val responseCode = connection.responseCode
            Log.d("AutoLoginManager", "Logout response code: $responseCode")
            connection.inputStream.close()
            // Assume success if we get a 200-299 response code.
            responseCode in 200..299
        } catch (e: Exception) {
            Log.e("AutoLoginManager", "Logout failed: ${e.message}")
            false
        }
    }
}
