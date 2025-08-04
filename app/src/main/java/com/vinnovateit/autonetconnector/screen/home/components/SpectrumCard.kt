package com.vinnovateit.autonetconnector

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinnovateit.autonetconnector.functionality2.manager.LiveDataPoint
import com.vinnovateit.autonetconnector.functionality2.manager.SessionSummary
import com.vinnovateit.autonetconnector.screen.home.components.HomeScreenGraph
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
      .padding(top = 105.dp) // Pushes content below the power button
      .padding(horizontal = 24.dp, vertical = 24.dp)
      .fillMaxSize(),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background), // Use theme color
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // No shadow
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
    ) {
      // Box to align the button to the top-left
      Box(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
        OutlinedButton(
          onClick = {
            val intent = Intent(context, StatsActivity::class.java).apply {
              putExtra("CURRENT_SSID", ssid)
            }
            val activity = context as? Activity
            activity?.startActivity(intent)
          },
          modifier = Modifier
            .height(40.dp) // Reduced height
            .align(Alignment.TopStart), // Positioned top-left
          shape = CircleShape, // Fully rounded
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
          )
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(id = R.string.home_network_statistics),
              fontWeight = FontWeight.Bold
            )
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = stringResource(id = R.string.home_go_to_statistics)
            )
          }
        }
      }

      // Graph Box now fills the remaining space without horizontal padding
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        if (session != null && session.history.isNotEmpty()) {
          HomeScreenGraph(
            modifier = Modifier.fillMaxSize(),
            rateHistory = historyForHomeScreen,
          )
        } else {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = stringResource(id = R.string.home_no_data_for_graph),
              color = MaterialTheme.colorScheme.onSurfaceVariant, // Use theme color
              fontSize = 14.sp,
              fontFamily = SatoshiFontFamily
            )
          }
        }
      }
    }
  }
}