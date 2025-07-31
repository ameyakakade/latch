package com.vinnovateit.autonetconnector.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.vinnovateit.autonetconnector.MainActivity
import com.vinnovateit.autonetconnector.R
import com.vinnovateit.autonetconnector.functionality2.manager.AutoLoginManager
import com.vinnovateit.autonetconnector.functionality2.manager.SessionRepository
import com.vinnovateit.autonetconnector.functionality2.storage.CredentialDatabase
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class LatchWidgetState(
  val status: String = "Disconnected",
  val ssid: String = "N/A",
  val connectedDuration: String = "-",
  val isConnected: Boolean = false
)

object LatchWidgetColorScheme {
  @SuppressLint("ResourceAsColor")
  val lightColors = lightColorScheme(
    primary = Color(R.color.widget_light_primary),
    onPrimary = Color(R.color.widget_light_on_primary),
    background = Color(R.color.widget_light_background),
    onBackground = Color(R.color.widget_light_text),
  )

  @SuppressLint("ResourceAsColor")
  val darkColors = darkColorScheme(
    primary = Color(R.color.widget_dark_primary),
    onPrimary = Color(R.color.widget_dark_on_primary),
    background = Color(R.color.widget_dark_background),
    onBackground = Color(R.color.widget_dark_text),
  )

  val colors = ColorProviders(light = lightColors, dark = darkColors)
}

class ConnectActionCallback : ActionCallback {
  override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
    updateAppWidgetState(context, glanceId) { prefs ->
      val stateKey = LatchWidget.WIDGET_STATE_PREF_KEY
      val connectingState = LatchWidgetState(
        status = "Connecting...",
        ssid = "Trying...",
        connectedDuration = "...",
        isConnected = false
      )
      prefs[stateKey] = Json.encodeToString(connectingState)
    }
    LatchWidget().update(context, glanceId)
    // Get state from the new repository
    val isConnected = SessionRepository.liveStatus.first() != null
    if (!isConnected) {
      val db = CredentialDatabase.getInstance(context)
      val credentials = db.credentialDao().getCredential()
      if (credentials != null) {
        AutoLoginManager.attemptLogin(
          credentials.registrationNumber,
          credentials.password
        )
      }
      val workRequest = OneTimeWorkRequestBuilder<UpdateWidgetWorker>().build()
      WorkManager.getInstance(context).enqueue(workRequest)
    }
  }
}

class LatchWidget : GlanceAppWidget() {
  companion object {
    val WIDGET_STATE_PREF_KEY = stringPreferencesKey(UpdateWidgetWorker.WIDGET_STATE_KEY)
    private const val TAG = "WidgetThemeDebug"
  }

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    Log.d(TAG, "LatchWidget provideGlance running for glanceId: $id")
    provideContent {
      GlanceTheme(
        colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          GlanceTheme.colors
        } else {
          LatchWidgetColorScheme.colors
        }
      ) {
        val prefs = currentState<Preferences>()
        val stateJson = prefs[WIDGET_STATE_PREF_KEY]
        val json = Json { ignoreUnknownKeys = true }
        val state = json.decodeFromString<LatchWidgetState>(stateJson ?: "{}")
        LatchWidgetContent(state = state)
      }
    }
  }

  @SuppressLint("RestrictedApi", "LocalContextConfigurationRead")
  @Composable
  private fun LatchWidgetContent(state: LatchWidgetState) {
    val context = LocalContext.current
    val isDarkTheme = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val logoRes = if (isDarkTheme) R.drawable.ic_latch_light else R.drawable.ic_latch_dark

    Column(
      modifier = GlanceModifier
        .fillMaxSize()
        .background(GlanceTheme.colors.background)
        .cornerRadius(24.dp)
        .padding(16.dp)
        .clickable(actionStartActivity<MainActivity>()),
      verticalAlignment = Alignment.Vertical.Top,
      horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
      Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = GlanceModifier.defaultWeight()) {
          Text(text = "Status:", style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 16.sp))
          Text(text = state.status, style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold))
        }
        Image(provider = ImageProvider(logoRes), contentDescription = "Latch Logo", modifier = GlanceModifier.size(40.dp))
      }
      Spacer(modifier = GlanceModifier.height(16.dp))
      Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = state.ssid, style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Medium))
        Spacer(modifier = GlanceModifier.defaultWeight())
        Box(modifier = GlanceModifier.background(GlanceTheme.colors.primary).cornerRadius(10.dp).padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
          Text(text = state.connectedDuration, style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold))
        }
      }
      Spacer(modifier = GlanceModifier.defaultWeight())
      Box(
        modifier = GlanceModifier
          .fillMaxWidth()
          .background(GlanceTheme.colors.primary)
          .cornerRadius(12.dp)
          .padding(vertical = 12.dp)
          .clickable(actionRunCallback<ConnectActionCallback>()),
        contentAlignment = Alignment.Center
      ) {
        val buttonText = if (state.isConnected) "Connected" else "Connect"
        Text(
          text = buttonText,
          style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        )
      }
    }
  }
}
