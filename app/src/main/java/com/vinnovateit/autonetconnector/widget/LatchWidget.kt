package com.vinnovateit.autonetconnector.widget

import android.content.Context
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.vinnovateit.autonetconnector.MainActivity
import com.vinnovateit.autonetconnector.R
import com.vinnovateit.autonetconnector.functionality2.background.DirectLoginWorker
import com.vinnovateit.autonetconnector.functionality2.background.DirectLogoutWorker
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Configurable constants for easy customization
private const val WIDGET_CORNER_RADIUS = 24
private const val WIDGET_PADDING = 16
private val STATUS_FONT_SIZE = 24.sp
private val BUTTON_FONT_SIZE = 18.sp

@Serializable
data class AutoNetWidgetState(
  val status: String = "Disconnected",
  val ssid: String = "N/A",
  val connectedDuration: String = "-",
  val isConnected: Boolean = false
)

class AutoNetWidget : GlanceAppWidget() {

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    provideContent {
      val stateJson = currentState<Preferences>()[LatchWidgetUpdater.WIDGET_STATE_PREF_KEY] ?: "{}"
      val state = try {
        Json.decodeFromString<AutoNetWidgetState>(stateJson)
      } catch (e: Exception) {
        AutoNetWidgetState() // Fallback on error
      }
      AutoNetWidgetContent(state)
    }
  }

  @Composable
  private fun AutoNetWidgetContent(state: AutoNetWidgetState) {
    // Custom colors (can be extended for themes)
    val colors = ColorProviders(
      light = lightColorScheme(background = Color.White, onBackground = Color.Black),
      dark = darkColorScheme(background = Color.DarkGray, onBackground = Color.White)
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
            Text(text = stringResource(id = R.string.status) + ":", style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 16.sp))
            Text(text = state.status, style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = STATUS_FONT_SIZE, fontWeight = FontWeight.Bold))
          }
          Image(provider = ImageProvider(R.drawable.ic_latch_dark), contentDescription = stringResource(id = R.string.app_logo_content_description), modifier = GlanceModifier.size(40.dp))
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
            .clickable(actionRunCallback<ConnectAction>()),
          contentAlignment = Alignment.Center
        ) {
          val buttonText = if (state.isConnected) stringResource(id = R.string.widget_disconnect) else stringResource(id = R.string.widget_connect)
          Text(
            text = buttonText,
            style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = BUTTON_FONT_SIZE, fontWeight = FontWeight.Bold)
          )
        }
      }
    }
  }
}

class ConnectAction : ActionCallback {
  override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
    val prefs = getAppWidgetState<Preferences>(
      context,
      glanceId = glanceId,
      definition = PreferencesGlanceStateDefinition,
    )
    val stateJson = prefs[LatchWidgetUpdater.WIDGET_STATE_PREF_KEY] ?: "{}"
    val state = try {
      Json.decodeFromString<AutoNetWidgetState>(stateJson)
    } catch (e: Exception) {
      AutoNetWidgetState()
    }

    val actionRequest = if (state.isConnected) {
      OneTimeWorkRequestBuilder<DirectLogoutWorker>().build()
    } else {
      OneTimeWorkRequestBuilder<DirectLoginWorker>().build()
    }
    WorkManager.getInstance(context).enqueue(actionRequest)

    // Give a small delay for the action to process before updating the widget UI
    delay(1500)
    LatchWidgetUpdater.enqueueOneTimeUpdate(context)
  }
}