@file:OptIn(ExperimentalMaterial3Api::class)

package com.vinnovateit.autonetconnector

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.vinnovateit.autonetconnector.functionality.SessionSummary
import com.vinnovateit.autonetconnector.functionality2.detector.VITWiFiIdentifier
import com.vinnovateit.autonetconnector.functionality2.ui.LoginTestRunner
import com.vinnovateit.autonetconnector.screen.home.components.HomeScreenGraph
import kotlinx.coroutines.launch

// Define Satoshi font family
val SatoshiFontFamily = FontFamily(
    Font(R.font.satoshi_regular, FontWeight.Normal),
    Font(R.font.satoshi_regular, FontWeight.Medium),
    Font(R.font.satoshi_regular, FontWeight.SemiBold),
    Font(R.font.satoshi_regular, FontWeight.Bold)
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A237E))
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
                            .background(Color(0xFF1A237E), RoundedCornerShape(2.dp))
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
                    .weight(0.45f)
                    .background(Color.White, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
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
                                val shadow = Color.Black.copy(alpha = 0.3f)
                                val r = size.minDimension / 2
                                drawCircle(shadow, radius = r, center = Offset(size.width/2 + 6.dp.toPx(), size.height/2 + 10.dp.toPx()))
                            },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A1D6F)),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Canvas(modifier = Modifier.size(56.dp)) {
                            val stroke = 6.dp.toPx()
                            val arcR = size.minDimension / 2.2f
                            val topLeft = Offset((size.width - arcR*2)/2f, (size.height - arcR*2)/2f)
                            drawArc(Color.White, startAngle = -135f, sweepAngle = -270f,
                                useCenter = false, style = Stroke(width = stroke, cap = StrokeCap.Round),
                                size = Size(arcR*2, arcR*2), topLeft = topLeft)
                            val cx = size.width/2
                            val cy = size.height/2
                            drawLine(Color.White,
                                start = Offset(cx, cy - arcR*1.2f),
                                end   = Offset(cx, cy - arcR*0.6f),
                                strokeWidth = stroke, cap = StrokeCap.Round)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = status,
                        color = Color(0xFF1A237E),
                        fontSize = 14.sp,
                        fontFamily = SatoshiFontFamily
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isConnected) "You're Online" else "You're Offline",
                        color = Color(0xFF1A237E),
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
                    .weight(0.55f)
                    .background(Color(0xFF1A237E))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                ) {
                    // Ping button
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { /* Handle ping */ },
                            modifier = Modifier.size(width = 120.dp, height = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Ping", color = Color(0xFF1A237E), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = SatoshiFontFamily)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Spectrum graph
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSpectrumClick() }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Spectrum", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = SatoshiFontFamily)
                                Icon(Icons.Default.ArrowForward, contentDescription = "Expand", tint = Color.White, modifier = Modifier.size(20.dp))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (session != null && session.history.isNotEmpty()) {
                                HomeScreenGraph(
                                    modifier = Modifier.fillMaxWidth().height(96.dp),
                                    rateHistory = session.history
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
                                    Text("No data available for graph", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontFamily = SatoshiFontFamily)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom status bar
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(if (isConnected) Color(0xFF4CAF50) else Color(0xFFE53E3E), CircleShape)
                            )

                            Text(text = ssid, color = Color.White, fontSize = 14.sp, fontFamily = SatoshiFontFamily)

                            Box(
                                modifier = Modifier
                                    .background(if (isConnected) Color(0xFF4CAF50) else Color(0xFFE53E3E), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isConnected) "CONNECTED" else "DISCONNECTED",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SatoshiFontFamily
                                )
                            }
                        }

                        Text(
                            text = networkSpeed,
                            color = Color.White,
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
    HomeScreen(
        isConnected = false,
        networkSpeed = "6 mbps",
        onSpectrumClick = { },
        session = null,
        ssid = "Not Connected",
        onConnectClick = { }        // ← stub lambda for preview
    )
}

@Preview(showBackground = true)
@Composable
fun HomeScreenOnlinePreview() {
    HomeScreen(
        isConnected = true,
        networkSpeed = "12 mbps",
        onSpectrumClick = { },
        session = null,
        ssid = "VIT-WiFi",
        onConnectClick = { }        // ← stub lambda for preview
    )
}

