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
    isConnected: Boolean = false,
    networkName: String = "",
    networkSpeed: String = "6 mbps",
    onSpectrumClick: () -> Unit = {},
    session: SessionSummary?
)
{
    val context = LocalContext.current
    val resolvedNetworkName = remember { VITWiFiIdentifier.getCurrentSSID(context)?.toString() ?: "Not Connected" }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Press the button to run auto-login test.") }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A237E)) // Deep blue background
    ) {
        // Top hamburger menu
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
            // Top white section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
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
                    Spacer(modifier = Modifier.height(120.dp))

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
                            .size(140.dp)
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
                        Canvas(modifier = Modifier.size(64.dp)) {
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




                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = status,
                        color = Color(0xFF1A237E),
                        fontSize = 14.sp,
                        fontFamily = SatoshiFontFamily
                    )

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

            // Bottom blue section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .background(Color(0xFF1A237E))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Ping button aligned to the right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { /* Handle ping */ },
                            modifier = Modifier
                                .width(120.dp)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                text = "Ping",
                                color = Color(0xFF1A237E),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = SatoshiFontFamily
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // FIX: Using a simple .clickable modifier now that the child graph is not scrollable.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                try {
                                    Log.d("HomeScreen", "Spectrum card tapped, navigating...")
                                    onSpectrumClick()
                                } catch (e: Exception) {
                                    Log.e("HomeScreen", "Error during navigation", e)
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = "Expand",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (session != null && session.history.isNotEmpty()) {
                                HomeScreenGraph(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    rateHistory = session.history
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
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
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Bottom status bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
