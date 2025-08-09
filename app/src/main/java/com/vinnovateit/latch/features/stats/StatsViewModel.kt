package com.vinnovateit.latch.features.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vinnovateit.latch.domain.model.DataUsage
import com.vinnovateit.latch.domain.model.SessionRepository
import com.vinnovateit.latch.domain.model.SessionSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

class StatsViewModel(application: Application) : AndroidViewModel(application) {

  // Data now comes from the single source of truth: SessionRepository
  val liveStatus = SessionRepository.liveStatus
  val lastSession = SessionRepository.lastSession
  val sessionHistory = SessionRepository.sessionSummaries

  // This flow combines live and last sessions to decide what to show in the UI.
  val sessionToShow: StateFlow<SessionSummary?> =
    combine(
      liveStatus,
      lastSession,
    ) { live, last ->
      live?.let {
        // Create a temporary summary for the UI from the live data
        SessionSummary(
          ssid = it.ssid,
          startTimestamp = it.startTimeMillis,
          endTimestamp = System.currentTimeMillis(), // It's ongoing
          totalData = DataUsage(
            it.liveData.sumOf { p -> p.usage.rxBytes },
            it.liveData.sumOf { p -> p.usage.txBytes }),
          history = it.liveData
        )
      } ?: last // If not live, show the last completed session
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val historyToShow: StateFlow<List<SessionSummary>> = sessionHistory

  init {
    // Ensure the repository is initialized.
    SessionRepository.initialize(application)
  }

  fun onClearHistory() {
    SessionRepository.clearHistory()
  }

  override fun onCleared() {
    super.onCleared()
  }
}

internal fun SessionSummary.isToday(): Boolean {
  val today = Calendar.getInstance()
  val sessionDay = Calendar.getInstance().apply { timeInMillis = startTimestamp }
  return today.get(Calendar.YEAR) == sessionDay.get(Calendar.YEAR) &&
    today.get(Calendar.DAY_OF_YEAR) == sessionDay.get(Calendar.DAY_OF_YEAR)
}
