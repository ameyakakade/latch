package com.vinnovateit.latch.features.wifi.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ConnectionStatus {
  object Idle : ConnectionStatus()
  data class Connecting(val message: String) : ConnectionStatus()
  object Success : ConnectionStatus()
  data class Failed(val message: String) : ConnectionStatus()
}

object ConnectionStatusManager {
  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
  private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Idle)
  val status = _status.asStateFlow()

  fun postStatus(newStatus: ConnectionStatus) {
    _status.value = newStatus
    // If the status is a final one (Success/Failed), start a timer to reset it to Idle.
    if (newStatus is ConnectionStatus.Success || newStatus is ConnectionStatus.Failed) {
      scope.launch {
        delay(1500)
        // Only reset if the status hasn't changed to a new "Connecting" state in the meantime
        if (_status.value == newStatus) {
          _status.value = ConnectionStatus.Idle
        }
      }
    }
  }
}