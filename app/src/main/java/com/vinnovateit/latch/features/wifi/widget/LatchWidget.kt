package com.vinnovateit.latch.features.wifi.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
import androidx.glance.appwidget.state.getAppWidgetState
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
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.vinnovateit.latch.features.home.MainActivity
import com.vinnovateit.latch.R
import com.vinnovateit.latch.features.wifi.background.ForegroundService
import com.vinnovateit.latch.ui.theme.DarkColorScheme
import com.vinnovateit.latch.ui.theme.LightColorScheme
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Configurable constants for easy customization
private const val WIDGET_CORNER_RADIUS = 28
private const val WIDGET_PADDING = 16
private val STATUS_FONT_SIZE = 24.sp
private val BUTTON_FONT_SIZE = 18.sp

@Serializable
data class LatchWidgetState(
  val status: String = "Disconnected",
  val connectedDuration: String = "-",
  val isConnected: Boolean = false,
  val isLightTheme: Boolean = true  // Flag for theme mode
)

class LatchWidget : GlanceAppWidget() {

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    provideContent {
      val stateJson = currentState<Preferences>()[LatchWidgetUpdater.WIDGET_STATE_PREF_KEY] ?: "{}"
      val state = try {
        Json.decodeFromString<LatchWidgetState>(stateJson)
      } catch (e: Exception) {
        LatchWidgetState() // Fallback on error
      }

      LatchWidgetContent(state)
    }
  }
}

@Composable
private fun LatchWidgetContent(state: LatchWidgetState) {
  val colors = ColorProviders(
    light = LightColorScheme,
    dark = DarkColorScheme
  )

  GlanceTheme(colors = colors) {

    Column(
      modifier = GlanceModifier
        .fillMaxSize()
        .background(GlanceTheme.colors.background)
        .cornerRadius(WIDGET_CORNER_RADIUS.dp)
        .padding(WIDGET_PADDING.dp)
        .clickable(actionStartActivity<MainActivity>()),
      verticalAlignment = Alignment.Top,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = GlanceModifier.defaultWeight()) {
          Text(text = stringResource(R.string.widget_status), style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 16.sp, fontFamily = FontFamily.Monospace))
          Text(text = state.status, style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = STATUS_FONT_SIZE, fontWeight = FontWeight.Bold))
        }
        Image(ImageProvider(R.drawable.ic_latch), contentDescription = stringResource(R.string.widget_app_logo), modifier = GlanceModifier.size(40.dp))
      }

      Spacer(modifier = GlanceModifier.height(16.dp))

      // Centered duration
      Box(modifier = GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(modifier = GlanceModifier.background(GlanceTheme.colors.primary).cornerRadius(10.dp).padding(horizontal = 12.dp, vertical = 6.dp)) {
          Text(text = state.connectedDuration, style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 14.sp))
        }
      }

      Spacer(modifier = GlanceModifier.defaultWeight())
      Box(
        modifier = GlanceModifier
          .fillMaxWidth()
          .background(GlanceTheme.colors.primary)
          .cornerRadius(16.dp)
          .padding(vertical = 12.dp)
          .clickable(actionRunCallback<ConnectAction>()),
        contentAlignment = Alignment.Center
      ) {
        val buttonText = if (state.isConnected) stringResource(R.string.widget_disconnect) else stringResource(R.string.widget_connect)
        Text(
          text = buttonText,
          style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = BUTTON_FONT_SIZE, fontWeight = FontWeight.Bold)
        )
      }
    }
  }
}

class ConnectAction : ActionCallback {
  override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
    val prefs = getAppWidgetState(
      context,
      glanceId = glanceId,
      definition = PreferencesGlanceStateDefinition,
    )

    val stateJson = prefs[LatchWidgetUpdater.WIDGET_STATE_PREF_KEY] ?: "{}"
    val state = try {
      Json.decodeFromString<LatchWidgetState>(stateJson)
    } catch (_: Exception) {
      LatchWidgetState()
    }

    val intent = Intent(context, ForegroundService::class.java).apply {
      action = if (state.isConnected) {
        ForegroundService.ACTION_TRIGGER_LOGOUT
      } else {
        ForegroundService.ACTION_TRIGGER_LOGIN_CHECK
      }
    }
    context.startService(intent)

    // Give the service a moment to act before updating the widget
    delay(1500)
    LatchWidgetUpdater.enqueueOneTimeUpdate(context)
  }
}
