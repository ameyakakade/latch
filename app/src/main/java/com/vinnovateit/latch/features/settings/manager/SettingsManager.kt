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
  private const val KEY_USE_DYNAMIC_COLORS = "use_dynamic_colors"

  // Default Values
  private const val DEFAULT_AUTO_LOGIN = true
  private const val DEFAULT_SPEED_UNITS = "bps"
  private const val DEFAULT_THEME = "System Default"
  private const val DEFAULT_USE_DYNAMIC_COLORS = true

  // StateFlows to observe changes
  private val _autoLogin = MutableStateFlow(DEFAULT_AUTO_LOGIN)
  val autoLogin: StateFlow<Boolean> = _autoLogin

  private val _speedUnits = MutableStateFlow(DEFAULT_SPEED_UNITS)
  val speedUnits: StateFlow<String> = _speedUnits

  private val _theme = MutableStateFlow(DEFAULT_THEME)
  val theme: StateFlow<String> = _theme

  private val _useDynamicColors = MutableStateFlow(DEFAULT_USE_DYNAMIC_COLORS)
  val useDynamicColors: StateFlow<Boolean> = _useDynamicColors

  fun initialize(context: Context) {
    sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    loadSettings()
  }

  private fun loadSettings() {
    _autoLogin.value = sharedPreferences.getBoolean(KEY_AUTO_LOGIN, DEFAULT_AUTO_LOGIN)
    _speedUnits.value = sharedPreferences.getString(KEY_SPEED_UNITS, DEFAULT_SPEED_UNITS) ?: DEFAULT_SPEED_UNITS
    _theme.value = sharedPreferences.getString(KEY_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
    _useDynamicColors.value = sharedPreferences.getBoolean(KEY_USE_DYNAMIC_COLORS, DEFAULT_USE_DYNAMIC_COLORS)
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

  fun setUseDynamicColors(enabled: Boolean) {
    _useDynamicColors.value = enabled
    sharedPreferences.edit { putBoolean(KEY_USE_DYNAMIC_COLORS, enabled) }
  }

}