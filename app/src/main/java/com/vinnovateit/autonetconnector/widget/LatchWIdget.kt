package com.vinnovateit.autonetconnector.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.vinnovateit.autonetconnector.MainActivity
import com.vinnovateit.autonetconnector.R
import com.vinnovateit.autonetconnector.functionality2.manager.WifiStatsManager
import com.vinnovateit.autonetconnector.functionality2.manager.AutoLoginManager
import com.vinnovateit.autonetconnector.functionality2.storage.CredentialDatabase
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LatchWidgetState(
  val status: String = "Disconnected",
  val ssid: String = "N/A",
  val speed: String = "...",
  val isConnected: Boolean = false
)

enum class WidgetTheme(val key: String) { LIGHT("light"), DARK("dark") }

// This is now a top-level class, which fixes the ClassNotFoundException
class ConnectActionCallback : ActionCallback {
  override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
    val isConnected = WifiStatsManager.liveStatus.first() != null
    if (!isConnected) {
      val db = CredentialDatabase.getInstance(context)
      val credentials = db.credentialDao().getCredential()
      if (credentials != null) {
        AutoLoginManager.attemptLogin(credentials.registrationNumber, credentials.password)
      }
    }
    val workRequest = OneTimeWorkRequestBuilder<UpdateWidgetWorker>().build()
    WorkManager.getInstance(context).enqueue(workRequest)
  }
}

class LatchWidget : GlanceAppWidget() {
  companion object {
    val THEME_PREF_KEY = stringPreferencesKey("widget_theme")
    val WIDGET_STATE_PREF_KEY = stringPreferencesKey(UpdateWidgetWorker.WIDGET_STATE_KEY)
  }

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    provideContent {
      val prefs = currentState<Preferences>()
      val themeKey = prefs[THEME_PREF_KEY] ?: WidgetTheme.DARK.key
      val stateJson = prefs[WIDGET_STATE_PREF_KEY]
      val state = if (stateJson.isNullOrBlank()) {
        LatchWidgetState()
      } else {
        Json.decodeFromString<LatchWidgetState>(stateJson)
      }
      LatchWidgetContent(themeKey = themeKey, state = state)
    }
  }
}

@SuppressLint("RestrictedApi")
@Composable
private fun LatchWidgetContent(themeKey: String, state: LatchWidgetState) {
  val isDarkTheme = themeKey == WidgetTheme.DARK.key
  val backgroundColor = if (isDarkTheme) R.color.widget_dark_background else R.color.widget_light_background
  val primaryColor = if (isDarkTheme) R.color.widget_dark_primary else R.color.widget_light_primary
  val onPrimaryColor = if (isDarkTheme) R.color.widget_dark_on_primary else R.color.widget_light_on_primary
  val textColor = if (isDarkTheme) R.color.widget_dark_text else R.color.widget_light_text

  Column(
    modifier = GlanceModifier
      .fillMaxSize()
      .background(ImageProvider(backgroundColor))
      .cornerRadius(24.dp)
      .padding(16.dp)
      .clickable(actionStartActivity<MainActivity>()),
    verticalAlignment = Alignment.Vertical.Top,
    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
  ) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = GlanceModifier.defaultWeight()) {
        Text(text = "Status:", style = TextStyle(color = ColorProvider(textColor), fontSize = 16.sp))
        Text(text = state.status, style = TextStyle(color = ColorProvider(textColor), fontSize = 24.sp, fontWeight = FontWeight.Bold))
      }
      Image(provider = ImageProvider(R.drawable.latchlogo), contentDescription = "Latch Logo", modifier = GlanceModifier.size(40.dp))
    }
    Spacer(modifier = GlanceModifier.height(16.dp))
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text(text = state.ssid, style = TextStyle(color = ColorProvider(textColor), fontSize = 20.sp, fontWeight = FontWeight.Medium))
      Spacer(modifier = GlanceModifier.defaultWeight())
      Box(modifier = GlanceModifier.background(ImageProvider(primaryColor)).cornerRadius(10.dp).padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        Text(text = state.speed, style = TextStyle(color = ColorProvider(onPrimaryColor), fontSize = 14.sp, fontWeight = FontWeight.Bold))
      }
    }
    Spacer(modifier = GlanceModifier.defaultWeight())
    Box(
      modifier = GlanceModifier
        .fillMaxWidth()
        .background(ImageProvider(primaryColor))
        .cornerRadius(12.dp)
        .padding(vertical = 12.dp)
        .clickable(actionRunCallback<ConnectActionCallback>()),
      contentAlignment = Alignment.Center
    ) {
      val buttonText = if (state.isConnected) "Connected" else "Connect"
      Text(
        text = buttonText,
        style = TextStyle(color = ColorProvider(onPrimaryColor), fontSize = 18.sp, fontWeight = FontWeight.Bold)
      )
    }
  }
}