package com.vinnovateit.autonetconnector.common.util

import com.vinnovateit.autonetconnector.domain.model.SessionSummary
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates a CSV report from a list of session summaries and writes it to an OutputStream.
 */
fun generateCsvReport(sessions: List<SessionSummary>, outputStream: OutputStream) {
  val writer = outputStream.bufferedWriter()
  // CSV Header
  writer.write(""""SSID","Start Time","End Time","Duration (Minutes)","Total Data (MB)","Download (MB)","Upload (MB)","Max Download Speed (Mbps)","Max Upload Speed (Mbps)"""")
  writer.newLine()

  val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

  // CSV Rows
  sessions.forEach { session ->
    val startTime = dateFormat.format(Date(session.startTimestamp))
    val endTime = dateFormat.format(Date(session.endTimestamp))
    val durationMinutes = (session.endTimestamp - session.startTimestamp) / 60000.0
    val totalDataMb = (session.totalData.rxBytes + session.totalData.txBytes) / 1048576.0
    val downloadMb = session.totalData.rxBytes / 1048576.0
    val uploadMb = session.totalData.txBytes / 1048576.0
    val maxDownloadMbps = (session.history.maxOfOrNull { it.usage.rxBytes } ?: 0L) * 8 / 1000000.0
    val maxUploadMbps = (session.history.maxOfOrNull { it.usage.txBytes } ?: 0L) * 8 / 1000000.0

    writer.write(
      """"${session.ssid}","$startTime","$endTime","${"%.2f".format(durationMinutes)}","${"%.2f".format(totalDataMb)}","${"%.2f".format(downloadMb)}","${"%.2f".format(uploadMb)}","${"%.2f".format(maxDownloadMbps)}","${"%.2f".format(maxUploadMbps)}""""
    )
    writer.newLine()
  }
  writer.close()
}