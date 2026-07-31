package com.vinnovateit.latch.desktop.platform

import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.TrayState
import com.vinnovateit.latch.core.platform.UserNotifier
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Notifications for the tray app.
 *
 * The split between [showOngoing] and [notifyTransient] is load-bearing. The
 * Android service calls its notification update on every liveStatus emission,
 * i.e. every 2 seconds. Mapping that to real Windows toasts would produce a
 * notification storm, and Windows rate-limits them anyway. So:
 *
 *   showOngoing      -> tray tooltip only. Free, safe every 2s.
 *   notifyTransient  -> real balloon. State transitions ONLY.
 */
class TrayNotifier : UserNotifier {

    /** Bound to the Compose Tray's tooltip. */
    val tooltip = MutableStateFlow("Latch")

    /** Set once the Compose tray exists; balloons are dropped before then. */
    var trayState: TrayState? = null

    override fun showOngoing(title: String, text: String) {
        tooltip.value = if (text.isBlank()) title else "$title\n$text"
    }

    override fun notifyTransient(title: String, text: String, isError: Boolean) {
        trayState?.sendNotification(
            Notification(
                title = title,
                message = text,
                type = if (isError) Notification.Type.Error else Notification.Type.Info,
            )
        )
    }

    override fun hideOngoing() {
        tooltip.value = "Latch"
    }
}
