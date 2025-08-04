package com.vinnovateit.autonetconnector.screen.stats.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@SuppressLint("ConstantLocale")
private val dateFormat = SimpleDateFormat("E, dd MMM yyyy", Locale.getDefault())
fun formatFriendlyDate(timestamp: Long): String {
  val then = Calendar.getInstance().apply { timeInMillis = timestamp }
  val now = Calendar.getInstance()
  val thenStartOfDay = then.clone() as Calendar
  thenStartOfDay.set(Calendar.HOUR_OF_DAY, 0)
  thenStartOfDay.set(Calendar.MINUTE, 0)
  thenStartOfDay.set(Calendar.SECOND, 0)
  thenStartOfDay.set(Calendar.MILLISECOND, 0)
  val nowStartOfDay = now.clone() as Calendar
  nowStartOfDay.set(Calendar.HOUR_OF_DAY, 0)
  nowStartOfDay.set(Calendar.MINUTE, 0)
  nowStartOfDay.set(Calendar.SECOND, 0)
  nowStartOfDay.set(Calendar.MILLISECOND, 0)
  return when {
    thenStartOfDay == nowStartOfDay -> "Today"
    (nowStartOfDay.timeInMillis - thenStartOfDay.timeInMillis).toInt() == 24 * 60 * 60 * 1000 -> "Yesterday"
    else -> dateFormat.format(Date(timestamp))
  }
}

