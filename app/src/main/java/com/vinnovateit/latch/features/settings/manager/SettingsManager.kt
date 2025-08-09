package com.vinnovateit.latch.features.settings.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SettingsManager {

  private const val PREFS_NAME = "app_settings"
  private lateinit var sharedPreferences: SharedPreferences

  // Keys
  private const val KEY_AUTO_LOGIN = "auto_login"
  private const val KEY_SPEED_UNITS = "speed_units"
  private const val KEY_THEME = "theme"
  private const val KEY_DATA_ALERT_ENABLED = "data_alert_enabled"
  private const val KEY_DATA_THRESHOLD = "data_threshold"
  private const val KEY_DETAILED_LOGS = "detailed_logs"

  // Default Values
  private const val DEFAULT_AUTO_LOGIN = true
  private const val DEFAULT_SPEED_UNITS = "bps"
  private const val DEFAULT_THEME = "System Default"
  private const val DEFAULT_DATA_ALERT_ENABLED = true
  private const val DEFAULT_DATA_THRESHOLD = 1.0f
  private const val DEFAULT_DETAILED_LOGS = false

  // StateFlows to observe changes
  private val _autoLogin = MutableStateFlow(DEFAULT_AUTO_LOGIN)
  val autoLogin: StateFlow<Boolean> = _autoLogin

  private val _speedUnits = MutableStateFlow(DEFAULT_SPEED_UNITS)
  val speedUnits: StateFlow<String> = _speedUnits

  private val _theme = MutableStateFlow(DEFAULT_THEME)
  val theme: StateFlow<String> = _theme

  private val _dataAlertEnabled = MutableStateFlow(DEFAULT_DATA_ALERT_ENABLED)
  val dataAlertEnabled: StateFlow<Boolean> = _dataAlertEnabled

  private val _dataThreshold = MutableStateFlow(DEFAULT_DATA_THRESHOLD)
  val dataThreshold: StateFlow<Float> = _dataThreshold

  private val _detailedLogs = MutableStateFlow(DEFAULT_DETAILED_LOGS)
  val detailedLogs: StateFlow<Boolean> = _detailedLogs


  fun initialize(context: Context) {
    sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    loadSettings()
  }

  private fun loadSettings() {
    _autoLogin.value = sharedPreferences.getBoolean(KEY_AUTO_LOGIN, DEFAULT_AUTO_LOGIN)
    _speedUnits.value = sharedPreferences.getString(KEY_SPEED_UNITS, DEFAULT_SPEED_UNITS) ?: DEFAULT_SPEED_UNITS
    _theme.value = sharedPreferences.getString(KEY_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
    _dataAlertEnabled.value = sharedPreferences.getBoolean(KEY_DATA_ALERT_ENABLED, DEFAULT_DATA_ALERT_ENABLED)
    _dataThreshold.value = sharedPreferences.getFloat(KEY_DATA_THRESHOLD, DEFAULT_DATA_THRESHOLD)
    _detailedLogs.value = sharedPreferences.getBoolean(KEY_DETAILED_LOGS, DEFAULT_DETAILED_LOGS)
  }

  fun setAutoLogin(enabled: Boolean) {
    _autoLogin.value = enabled
    sharedPreferences.edit { putBoolean(KEY_AUTO_LOGIN, enabled) }
  }

  fun setSpeedUnits(units: String) {
    _speedUnits.value = units
    sharedPreferences.edit { putString(KEY_SPEED_UNITS, units) }
  }

  fun setTheme(themeValue: String) {
    _theme.value = themeValue
    sharedPreferences.edit { putString(KEY_THEME, themeValue) }
  }

  fun setDataAlertEnabled(enabled: Boolean) {
    _dataAlertEnabled.value = enabled
    sharedPreferences.edit { putBoolean(KEY_DATA_ALERT_ENABLED, enabled) }
  }

  fun setDataThreshold(threshold: Float) {
    _dataThreshold.value = threshold
    sharedPreferences.edit { putFloat(KEY_DATA_THRESHOLD, threshold) }
  }

  fun setDetailedLogs(enabled: Boolean) {
    _detailedLogs.value = enabled
    sharedPreferences.edit { putBoolean(KEY_DETAILED_LOGS, enabled) }
  }
}