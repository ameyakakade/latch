package com.vinnovateit.autonetconnector.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.vinnovateit.autonetconnector.R
import com.vinnovateit.autonetconnector.ui.theme.AutoNetConnectorTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class WidgetConfigureActivity : ComponentActivity() {

  private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Find the widget id from the intent.
    val extras = intent.extras
    if (extras != null) {
      appWidgetId = extras.getInt(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
      )
    }
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
      finish()
      return
    }

    setContent {
      AutoNetConnectorTheme {
        WidgetThemeChooserUI { theme ->
          selectTheme(theme)
        }
      }
    }
  }

  private fun selectTheme(theme: WidgetTheme) {
    MainScope().launch {
      val glanceAppWidgetManager = GlanceAppWidgetManager(this@WidgetConfigureActivity)
      val glanceId = glanceAppWidgetManager.getGlanceIdBy(appWidgetId)

      updateAppWidgetState(this@WidgetConfigureActivity, glanceId) { prefs ->
        prefs[LatchWidget.THEME_PREF_KEY] = theme.key
      }

      LatchWidget().update(this@WidgetConfigureActivity, glanceId)

      val resultValue = Intent()
      resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
      setResult(Activity.RESULT_OK, resultValue)
      finish()
    }
  }
}

@Composable
fun WidgetThemeChooserUI(onThemeSelected: (WidgetTheme) -> Unit) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF1C1B1F))
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "Choose a Widget Theme",
        color = Color.White,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(24.dp))

      // Previews in a Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        WidgetPreview(
          theme = WidgetTheme.DARK,
          modifier = Modifier.weight(1f)
        ) {
          onThemeSelected(WidgetTheme.DARK)
        }
        Spacer(modifier = Modifier.width(16.dp))
        WidgetPreview(
          theme = WidgetTheme.LIGHT,
          modifier = Modifier.weight(1f)
        ) {
          onThemeSelected(WidgetTheme.LIGHT)
        }
      }
    }
  }
}

@Composable
fun WidgetPreview(
  theme: WidgetTheme,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  val isDarkTheme = theme == WidgetTheme.DARK
  // We use colorResource to pull the colors from our colors.xml
  val backgroundColor = colorResource(if (isDarkTheme) R.color.widget_dark_background else R.color.widget_light_background)
  val primaryColor = colorResource(if (isDarkTheme) R.color.widget_dark_primary else R.color.widget_light_primary)
  val onPrimaryColor = colorResource(if (isDarkTheme) R.color.widget_dark_on_primary else R.color.widget_light_on_primary)
  val textColor = colorResource(if (isDarkTheme) R.color.widget_dark_text else R.color.widget_light_text)

  // A miniaturized version of our widget's UI using standard Compose components
  Column(
    modifier = modifier
      .aspectRatio(1.6f) // Maintain the widget's aspect ratio
      .clip(RoundedCornerShape(24.dp)) // **Correctly rounded corners**
      .background(backgroundColor)
      .clickable(onClick = onClick) // **Functionality added here**
      .padding(16.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text("Status:", style = androidx.compose.ui.text.TextStyle(color = textColor, fontSize = 14.sp))
        Text("Connected", style = androidx.compose.ui.text.TextStyle(color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold))
      }
      // In a real app, you'd use an Image composable for the logo here
    }
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text("S-VIT", style = androidx.compose.ui.text.TextStyle(color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium))
      Spacer(Modifier.weight(1f))
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .background(primaryColor)
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text("6 MBPS", style = androidx.compose.ui.text.TextStyle(color = onPrimaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold))
      }
    }
    Spacer(Modifier.weight(1f))
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(primaryColor)
        .padding(vertical = 8.dp),
      contentAlignment = Alignment.Center
    ) {
      Text("Disconnect", style = androidx.compose.ui.text.TextStyle(color = onPrimaryColor, fontSize = 14.sp, fontWeight = FontWeight.Bold))
    }
  }
}