@file:OptIn(ExperimentalMaterial3Api::class)

package com.vinnovateit.autonetconnector

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinnovateit.autonetconnector.functionality.LiveDataPoint
import com.vinnovateit.autonetconnector.functionality.SessionSummary
import com.vinnovateit.autonetconnector.screen.home.components.HomeScreenGraph
import com.vinnovateit.autonetconnector.functionality.PingUtility
import com.vinnovateit.autonetconnector.ui.theme.AutoNetConnectorTheme
import com.vinnovateit.autonetconnector.ui.theme.ShadowColor
import com.vinnovateit.autonetconnector.ui.theme.StatusConnected
import com.vinnovateit.autonetconnector.ui.theme.StatusDisconnected
import kotlinx.coroutines.launch

// Define Satoshi font family
val SatoshiFontFamily = FontFamily(
  Font(R.font.satoshi_regular, FontWeight.Normal),
  Font(R.font.satoshi_medium, FontWeight.Medium),
  Font(R.font.satoshi_bold, FontWeight.Bold)
)

@Composable
fun HomeScreen(
  isConnected: Boolean,
  networkSpeed: String,
  onSpectrumClick: () -> Unit,
  session: SessionSummary?,
  ssid: String,
  onConnectClick: () -> Unit
) {
  val context = LocalContext.current
  var status by remember { mutableStateOf("Press the button to run auto-login test.") }
  var pingResult by remember { mutableStateOf("Ping") }
  val coroutineScope = rememberCoroutineScope()

  // --- State Lifted Up for the Graph ---
  val historyToShow = session?.history ?: emptyList()
  val historyForHomeScreen = historyToShow.takeLast(150) // Take last 5 minutes (150 points at 2s interval)


  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Top hamburger menu
    Box(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(top = 24.dp, end = 24.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) {
          Box(
            modifier = Modifier
              .width(24.dp)
              .height(3.dp)
              .background(MaterialTheme.colorScheme.background, RoundedCornerShape(2.dp))
          )
        }
      }
    }

    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // White top section
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(0.5325f)
          .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Spacer(modifier = Modifier.height(80.dp))

          // Power button
          Button(
            onClick = {
              status = "Authenticating..."
              onConnectClick()
            },
            modifier = Modifier
              .size(120.dp)
              .graphicsLayer { clip = true; shape = CircleShape }
              .drawBehind {
                val r = size.minDimension / 2
                drawCircle(
                  ShadowColor,
                  radius = r,
                  center = Offset(size.width / 2 + 6.dp.toPx(), size.height / 2 + 10.dp.toPx())
                )
              },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface),
            elevation = ButtonDefaults.buttonElevation(0.dp)
          ) {
            // Hoist the color outside the Canvas
            val powerIconColor = MaterialTheme.colorScheme.surface
            Canvas(modifier = Modifier.size(56.dp)) {
              val stroke = 6.dp.toPx()
              val arcR = size.minDimension / 2.2f
              val topLeft = Offset((size.width - arcR * 2) / 2f, (size.height - arcR * 2) / 2f)
              drawArc(
                color = powerIconColor, startAngle = -135f, sweepAngle = -270f,
                useCenter = false, style = Stroke(width = stroke, cap = StrokeCap.Round),
                size = Size(arcR * 2, arcR * 2), topLeft = topLeft
              )
              val cx = size.width / 2
              val cy = size.height / 2
              drawLine(
                color = powerIconColor,
                start = Offset(cx, cy - arcR * 1.2f),
                end = Offset(cx, cy - arcR * 0.6f),
                strokeWidth = stroke, cap = StrokeCap.Round
              )
            }
          }

          Spacer(modifier = Modifier.height(20.dp))

          Text(
            text = status,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontFamily = SatoshiFontFamily
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = if (isConnected) "You're Online" else "You're Offline",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = SatoshiFontFamily
          )
        }
      }

      // Blue bottom section
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(0.4675f)
          .background(MaterialTheme.colorScheme.background)
      ) {
        // Content of the blue section
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // Ping button
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.End
          ) {
            Button(
              onClick = {
                coroutineScope.launch {
                  pingResult = "Pinging..."
                  pingResult = PingUtility.getPing()
                }
              },
              modifier = Modifier.size(width = 140.dp, height = 48.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
              shape = RoundedCornerShape(24.dp),
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = "Ping Refresh",
                  tint = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  pingResult,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.SemiBold,
                  fontFamily = SatoshiFontFamily
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Spectrum Title and Navigation
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Spectrum",
              color = MaterialTheme.colorScheme.onBackground,
              fontSize = 18.sp,
              fontWeight = FontWeight.SemiBold,
              fontFamily = SatoshiFontFamily
            )
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onSpectrumClick),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Navigate to Spectrum",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(24.dp)
              )
            }
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(0.8f)
              .padding(bottom = 24.dp)
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
                  text = "No data available for graph",
                  color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                  fontSize = 14.sp,
                  fontFamily = SatoshiFontFamily
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Bottom status bar
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(12.dp)
                  .background(
                    if (isConnected) StatusConnected else StatusDisconnected,
                    CircleShape
                  )
              )
              Text(
                text = ssid,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                fontFamily = SatoshiFontFamily
              )
              Box(
                modifier = Modifier
                  .background(
                    if (isConnected) StatusConnected else StatusDisconnected,
                    RoundedCornerShape(4.dp)
                  )
                  .padding(horizontal = 8.dp, vertical = 2.dp)
              ) {
                Text(
                  text = if (isConnected) "CONNECTED" else "DISCONNECTED",
                  color = MaterialTheme.colorScheme.onBackground,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = SatoshiFontFamily
                )
              }
            }
            Text(
              text = networkSpeed,
              color = MaterialTheme.colorScheme.onBackground,
              fontSize = 16.sp,
              fontWeight = FontWeight.Medium,
              fontFamily = SatoshiFontFamily
            )
          }
        }
      }
    }
  }
}


@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
  AutoNetConnectorTheme() {
    HomeScreen(
      isConnected = false,
      networkSpeed = "6 mbps",
      onSpectrumClick = { },
      session = null,
      ssid = "Not Connected",
      onConnectClick = { }
    )
  }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenOnlinePreview() {
  AutoNetConnectorTheme() {
    HomeScreen(
      isConnected = true,
      networkSpeed = "12 mbps",
      onSpectrumClick = { },
      session = null,
      ssid = "VIT-WiFi",
      onConnectClick = { }
    )
  }
}
