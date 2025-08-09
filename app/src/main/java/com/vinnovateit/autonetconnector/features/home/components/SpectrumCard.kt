package com.vinnovateit.autonetconnector.features.home.components

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinnovateit.autonetconnector.R
import com.vinnovateit.autonetconnector.domain.model.LiveDataPoint
import com.vinnovateit.autonetconnector.domain.model.SessionSummary
import com.vinnovateit.autonetconnector.features.stats.StatsActivity
import com.vinnovateit.autonetconnector.ui.theme.ModernizFontFamily
import com.vinnovateit.autonetconnector.ui.theme.SatoshiFontFamily

@Composable
fun SpectrumCard(
  session: SessionSummary?,
  ssid: String,
  historyForHomeScreen: List<LiveDataPoint>
) {
  val context = LocalContext.current
  Card(
    modifier = Modifier
      .padding(top = 105.dp)
      .padding(horizontal = 24.dp, vertical = 24.dp)
      .fillMaxSize(),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxSize()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            val intent = Intent(context, StatsActivity::class.java).apply {
              putExtra("CURRENT_SSID", ssid)
            }
            (context as? Activity)?.startActivity(intent)
          }
          .padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = stringResource(id = R.string.home_network_statistics),
          fontFamily = ModernizFontFamily,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.weight(1f)
        )
        Icon(
          imageVector = Icons.Rounded.ArrowOutward,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier
            .padding(end = 4.dp, top = 0.dp)
        )
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        if (session != null && session.history.isNotEmpty()) {
          HomeScreenGraph(
            modifier = Modifier.fillMaxSize(),
            rateHistory = historyForHomeScreen
          )
        } else {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = stringResource(id = R.string.home_no_data_for_graph),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 14.sp,
              fontFamily = SatoshiFontFamily,
              textAlign = TextAlign.Center
            )
          }
        }
      }
    }
  }
}