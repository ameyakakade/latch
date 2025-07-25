@file:OptIn(ExperimentalMaterial3Api::class)

package com.vinnovateit.autonetconnector

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
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
import com.vinnovateit.autonetconnector.functionality.PingUtility
import com.vinnovateit.autonetconnector.functionality.SessionSummary
import com.vinnovateit.autonetconnector.functionality2.detector.VITWiFiIdentifier
import com.vinnovateit.autonetconnector.functionality2.ui.LoginTestRunner
import com.vinnovateit.autonetconnector.screen.home.components.HomeScreenGraph
import com.vinnovateit.autonetconnector.screen.stats.ui.Tag
import kotlinx.coroutines.launch

// Define Satoshi font family
val SatoshiFontFamily = FontFamily(
    Font(R.font.satoshi_regular, FontWeight.Normal),
    Font(R.font.satoshi_medium, FontWeight.Medium),
    Font(R.font.satoshi_bold, FontWeight.Bold)
)

@Composable
fun HomeScreen(
    isConnected: Boolean = false,
    networkName: String = "",
    networkSpeed: String = "6 mbps",
    onSpectrumClick: () -> Unit = {},
    session: SessionSummary?
) {
    val context = LocalContext.current
    val resolvedNetworkName = remember { VITWiFiIdentifier.getCurrentSSID(context)?.toString() ?: "Not Connected" }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Press the button to run auto-login test.") }
    var pingStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        pingStatus = "Pinging..."
        pingStatus = PingUtility.getPing()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A237E)) // Deep blue background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top white section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f)
                    .background(
                        Color.White,
                        RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(80.dp)) // Reduced from 120.dp

                    // Large power button with shadow
                    Button(
                        onClick = {
                            status = "Running auto-login test..."
                            scope.launch {
                                LoginTestRunner.run(context.applicationContext)
                                status = "Test finished. Check logcat for output."
                            }
                        },
                        modifier = Modifier
                            .size(120.dp) // Reduced from 140.dp
                            .graphicsLayer {
                                clip = true
                                shape = CircleShape
                            }
                            .drawBehind {
                                // Shadow
                                val shadowColor = Color.Black.copy(alpha = 0.3f)
                                val radius = size.minDimension / 2
                                drawCircle(
                                    color = shadowColor,
                                    radius = radius,
                                    center = Offset(
                                        x = size.width / 2 + 6.dp.toPx(),
                                        y = size.height / 2 + 10.dp.toPx()
                                    )
                                )
                            },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A1D6F)),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Canvas(modifier = Modifier.size(56.dp)) { // Reduced from 64.dp
                            val strokeWidth = 6.dp.toPx()
                            val arcRadius = size.minDimension / 2.2f
                            val arcTopLeft = Offset(
                                (size.width - arcRadius * 2) / 2f,
                                (size.height - arcRadius * 2) / 2f
                            )

                            // Inverted Arc (curves upward)
                            drawArc(
                                color = Color.White,
                                startAngle = -135f,         // Inverted start
                                sweepAngle = -270f,         // Sweep in negative direction
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                                size = Size(arcRadius * 2, arcRadius * 2),
                                topLeft = arcTopLeft
                            )

                            // Line pointing downward from arc center
                            val centerX = size.width / 2
                            val centerY = size.height / 2
                            drawLine(
                                color = Color.White,
                                start = Offset(centerX, centerY - arcRadius * 1.2f),
                                end = Offset(centerX, centerY - arcRadius * 0.6f),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp)) // Reduced from 32.dp

                    Text(
                        text = status,
                        color = Color(0xFF1A237E),
                        fontSize = 14.sp,
                        fontFamily = SatoshiFontFamily
                    )

                    Spacer(modifier = Modifier.height(8.dp)) // Added small spacer

                    // Status text
                    Text(
                        text = if (isConnected) "You're Online" else "You're Offline",
                        color = Color(0xFF1A237E),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SatoshiFontFamily
                    )
                }
            }

            // Bottom blue section - increased weight from 0.4f to 0.55f
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
                    // Ping Pill
                    pingStatus?.let {
                        Tag(
                            text = it,
                            color = Color.White,
                            modifier = Modifier.padding(top = 20.dp),
                            onClick = {
                                scope.launch {
                                    pingStatus = "Pinging..."
                                    pingStatus = PingUtility.getPing()
                                }
                            }
                        )
                    }

                    // Spectrum Title and Navigation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Spectrum",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SatoshiFontFamily
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onSpectrumClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Navigate to Spectrum",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }


                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (session != null && session.history.isNotEmpty()) {
                            HomeScreenGraph(
                                modifier = Modifier
                                    .fillMaxSize(),
                                rateHistory = session.history,
                                scrollState = rememberScrollState()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No data available for graph",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp,
                                    fontFamily = SatoshiFontFamily
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp)) // Added fixed spacer

                    // Bottom status bar - now has guaranteed space
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Status dot - changes color based on connection
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        if (isConnected) Color(0xFF4CAF50) else Color(0xFFE53E3E),
                                        CircleShape
                                    )
                            )

                            Text(
                                text = resolvedNetworkName,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = SatoshiFontFamily
                            )

                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isConnected) Color(0xFF4CAF50) else Color(0xFFE53E3E),
                                        RoundedCornerShape(4.dp)
                                    )
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
        networkName = "Vit S-block 2.4",
        networkSpeed = "6 mbps",
        onSpectrumClick = { },
        session = null
    )
}

@Preview(showBackground = true)
@Composable
fun HomeScreenOnlinePreview() {
    HomeScreen(
        isConnected = true,
        networkName = "Vit S-block 2.4",
        networkSpeed = "12 mbps",
        onSpectrumClick = { },
        session = null
    )
}