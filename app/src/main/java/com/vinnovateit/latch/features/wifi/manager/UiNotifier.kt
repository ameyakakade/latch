package com.vinnovateit.latch.features.wifi.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.vinnovateit.latch.R

/**
 * A simple singleton to abstract away UI notifications from
 * data-layer components, ensuring they are shown as system notifications.
 */
object UiNotifier {
    private const val NOTIFICATION_ID = 2002
    private const val CHANNEL_ID = "latch_ui_notifications"

    fun showToast(context: Context, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Status Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows temporary status messages like Connected/Disconnected"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context.applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Adjust if icon is different
            .setContentTitle("Latch Status")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
