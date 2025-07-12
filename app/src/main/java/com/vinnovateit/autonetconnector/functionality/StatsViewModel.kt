package com.vinnovateit.autonetconnector.functionality

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.random.Random

class StatsViewModel(application: Application) : ViewModel() {

  // --- State Management ---
  private val _showMockData = MutableStateFlow(false)
  val showMockData: StateFlow<Boolean> = _showMockData.asStateFlow()

  // --- Real Data ---
  val liveStatus = WifiStatsManager.liveStatus
  val lastSession = WifiStatsManager.lastSession
  val sessionHistory = WifiStatsManager.sessionSummaries
  val systemStatus = WifiStatsManager.systemStatus // Expose the new status flow

  // --- Mock Data ---
  private val mockSessionFlow = MutableStateFlow(createMockSessionSummary(true))
  private val mockHistoryFlow = MutableStateFlow(createMockHistory())

  // --- Combined Data Exposed to UI ---
  val sessionToShow: StateFlow<SessionSummary?> =
    combine(
      showMockData,
      liveStatus,
      lastSession,
      mockSessionFlow
    ) { isMock, live, last, mock ->
      if (isMock) mock else live?.let {
        SessionSummary(
          ssid = it.ssid,
          startTimestamp = it.startTimeMillis,
          endTimestamp = System.currentTimeMillis(),
          totalData = DataUsage(it.liveData.sumOf { p -> p.usage.rxBytes }, it.liveData.sumOf { p -> p.usage.txBytes }),
          history = it.liveData
        )
      } ?: last
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


  val historyToShow: StateFlow<List<SessionSummary>> =
    combine(
      showMockData,
      sessionHistory,
      mockHistoryFlow
    ) { isMock, real, mock ->
      if (isMock) mock else real
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


  init {
    WifiStatsManager.initialize(application)

    viewModelScope.launch {
      showMockData.collect { isMocking ->
        if (isMocking) {
          mockSessionFlow.value = createMockSessionSummary(true)
          mockHistoryFlow.value = createMockHistory()

          while (_showMockData.value) {
            delay(1000)
            val newPoint = LiveDataPoint(
              timestamp = System.currentTimeMillis(),
              usage = DataUsage(Random.nextLong(50_000, 200_000), Random.nextLong(10_000, 80_000))
            )
            mockSessionFlow.update { prev ->
              prev.copy(
                history = prev.history + newPoint,
                totalData = DataUsage(prev.totalData.rxBytes + newPoint.usage.rxBytes, prev.totalData.txBytes + newPoint.usage.txBytes),
                endTimestamp = System.currentTimeMillis()
              )
            }
            mockHistoryFlow.update { history ->
              history.toMutableList().apply {
                val todayIndex = indexOfFirst { it.isToday() }
                if (todayIndex != -1) {
                  val todaySummary = get(todayIndex)
                  set(todayIndex, todaySummary.copy(
                    totalData = DataUsage(todaySummary.totalData.rxBytes + newPoint.usage.rxBytes, todaySummary.totalData.txBytes + newPoint.usage.txBytes)
                  ))
                }
              }
            }
          }
        }
      }
    }
  }

  fun onToggleMockData(enabled: Boolean) {
    _showMockData.value = enabled
  }

  fun onClearHistory() {
    WifiStatsManager.clearHistory()
  }

  override fun onCleared() {
    super.onCleared()
    WifiStatsManager.cleanup()
  }
}

// ... (ViewModelFactory and mock data functions remain the same)
class StatsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
      @Suppress("UNCHECKED_CAST")
      return StatsViewModel(application) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}

private fun createMockSessionSummary(isLive: Boolean = false): SessionSummary {
  val now = System.currentTimeMillis()
  val history = if (isLive) (0..60).map { i ->
    LiveDataPoint(
      timestamp = now - (60 - i) * 1000L,
      usage = DataUsage(Random.nextLong(50_000, 200_000), Random.nextLong(10_000, 80_000))
    )
  } else emptyList()

  return SessionSummary(
    ssid = "Q-VIT-MOCK",
    startTimestamp = now - 3600 * 1000L,
    endTimestamp = now,
    totalData = DataUsage(1_234_567_890, 123_456_789),
    history = history
  )
}

private fun createMockHistory(): List<SessionSummary> {
  return (0..10).map { i ->
    val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
    val endTime = calendar.timeInMillis
    val startTime = endTime - (1..12).random() * 3600 * 1000L
    SessionSummary(
      ssid = "Mock History ${10 - i}",
      startTimestamp = startTime,
      endTimestamp = endTime,
      totalData = DataUsage((100_000_000L..5_000_000_000L).random(), (50_000_000L..1_000_000_000L).random()),
      history = emptyList()
    )
  }
}

internal fun SessionSummary.isToday(): Boolean {
  val today = Calendar.getInstance()
  val sessionDay = Calendar.getInstance().apply { timeInMillis = startTimestamp }
  return today.get(Calendar.YEAR) == sessionDay.get(Calendar.YEAR) &&
    today.get(Calendar.DAY_OF_YEAR) == sessionDay.get(Calendar.DAY_OF_YEAR)
}
