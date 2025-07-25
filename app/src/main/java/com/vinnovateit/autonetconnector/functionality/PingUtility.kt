package com.vinnovateit.autonetconnector.functionality

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.IOException

object PingUtility {

  suspend fun getPing(host: String = "google.com"): String {
    return withContext(Dispatchers.IO) {
      try {
        val command = "/system/bin/ping -c 1 $host"
        val process = Runtime.getRuntime().exec(command)
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String?
        var pingTime: String? = null

        while (reader.readLine().also { line = it } != null) {
          if (line!!.contains("time=")) {
            val start = line!!.indexOf("time=") + 5
            val end = line!!.indexOf(" ms")
            pingTime = line!!.substring(start, end)
            break
          }
        }
        process.waitFor()
        pingTime?.let { "$it ms" } ?: "Failed"
      } catch (e: IOException) {
        "Error"
      } catch (e: InterruptedException) {
        "Error"
      }
    }
  }
}