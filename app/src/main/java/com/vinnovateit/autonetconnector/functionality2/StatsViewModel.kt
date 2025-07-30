package com.vinnovateit.autonetconnector.functionality2

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinnovateit.autonetconnector.functionality2.manager.DataUsage
import com.vinnovateit.autonetconnector.functionality2.manager.SessionSummary
import com.vinnovateit.autonetconnector.functionality2.manager.WifiStatsManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

class StatsViewModel(application: Application) : ViewModel() {

  val liveStatus = WifiStatsManager.liveStatus
  val lastSession = WifiStatsManager.lastSession
  val sessionHistory = WifiStatsManager.sessionSummaries
  val systemStatus = WifiStatsManager.systemStatus

  val sessionToShow: StateFlow<SessionSummary?> =
    combine(
      liveStatus,
      lastSession,
    ) { live, last ->
      live?.let {
        SessionSummary(
          ssid = it.ssid,
          startTimestamp = it.startTimeMillis,
          endTimestamp = System.currentTimeMillis(),
          totalData = DataUsage(
            it.liveData.sumOf { p -> p.usage.rxBytes },
            it.liveData.sumOf { p -> p.usage.txBytes }),
          history = it.liveData
        )
      } ?: last
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val historyToShow: StateFlow<List<SessionSummary>> = sessionHistory

  init {
    WifiStatsManager.initialize(application)
  }

  fun onClearHistory() {
    WifiStatsManager.clearHistory()
  }

  override fun onCleared() {
    super.onCleared()
  }
}

class StatsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
      @Suppress("UNCHECKED_CAST")
      return StatsViewModel(application) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}

internal fun SessionSummary.isToday(): Boolean {
  val today = Calendar.getInstance()
  val sessionDay = Calendar.getInstance().apply { timeInMillis = startTimestamp }
  return today.get(Calendar.YEAR) == sessionDay.get(Calendar.YEAR) &&
    today.get(Calendar.DAY_OF_YEAR) == sessionDay.get(Calendar.DAY_OF_YEAR)
}