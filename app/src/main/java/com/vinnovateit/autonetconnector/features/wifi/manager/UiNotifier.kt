package com.vinnovateit.autonetconnector.features.wifi.manager

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * A simple singleton to abstract away UI notifications (Toasts) from
 * data-layer components, ensuring they are always shown on the main thread.
 */
object UiNotifier {
  private val mainHandler = Handler(Looper.getMainLooper())

  fun showToast(context: Context, message: String) {
    mainHandler.post {
      Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
    }
  }
}
