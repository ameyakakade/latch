package com.vinnovateit.autonetconnector.screen.stats.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Formats a timestamp into a user-friendly string like "Today", "Yesterday",
 * or the full date for older entries.
 *
 * @param timestamp The timestamp in milliseconds.
 * @return A formatted, user-friendly date string.
 */
fun formatFriendlyDate(timestamp: Long): String {
  val then = Calendar.getInstance().apply { timeInMillis = timestamp }
  val now = Calendar.getInstance()

  // Normalize to the start of the day for accurate comparison
  val thenStartOfDay = (then.clone() as Calendar).apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }
  val nowStartOfDay = (now.clone() as Calendar).apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }

  return when {
    thenStartOfDay == nowStartOfDay -> "Today"
    (nowStartOfDay.timeInMillis - thenStartOfDay.timeInMillis).toInt() == 24 * 60 * 60 * 1000 -> "Yesterday"
    else -> SimpleDateFormat("E, dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
  }
}
