

package com.vinnovateit.autonetconnector


import android.content.Intent
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image

import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinnovateit.autonetconnector.functionality.SessionSummary
import com.vinnovateit.autonetconnector.screen.home.components.HomeScreenGraph

val SatoshiFontFamily = FontFamily(
    Font(R.font.satoshi_regular, FontWeight.Normal),
    Font(R.font.satoshi_regular, FontWeight.Medium),
    Font(R.font.satoshi_regular, FontWeight.SemiBold),
    Font(R.font.satoshi_bold, FontWeight.Bold)
)



val SakingFontFamily = FontFamily(
    Font(R.font.saking_regular, FontWeight.Normal)
)




@Composable
fun HomeScreen(
    isConnected: Boolean,
    networkSpeed: String,
    onSpectrumClick: () -> Unit,
    session: SessionSummary?,
    ssid: String,
    onConnectClick: () -> Unit,
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Press the button to run auto-login test.") }
    var showMenu by remember { mutableStateOf(false) }



    val density = context.resources.displayMetrics.density

    val widthDp = (406.4951171875f / density).dp
    val heightDp = (531f / density).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDF0D5))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Beige top section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .background(Color(0xFFFDF0D5))
            ) {
                // Background pattern
                Image(
                    painter = painterResource(id = R.drawable.latch_background_home),
                    contentDescription = "Background Pattern",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 49.dp, start = 23.dp, end = 23.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo
                    Icon(
                        painter = painterResource(id = R.drawable.latch_logo),
                        contentDescription = "LATCH Logo",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .size(width = 49.33.dp, height = 36.dp)
                    )

                    // LATCH Text centered
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LATCH",
                            color = Color(0xFFC01221),
                            fontSize = 28.97.sp,
                            fontFamily = SakingFontFamily,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = (28.97 * 0.02).sp,
                            lineHeight = (28.97 * 1.0).sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Hamburger Menu
                    Box(
                        modifier = Modifier
                            .size(width = 27.dp, height = 24.dp)
                            .clickable { showMenu = !showMenu }
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 0.dp)
                        ) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .height(2.8.dp)
                                        .background(Color(0xFFC01221))
                                )
                            }
                        }

                        // Dropdown Menu
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .background(Color.White, shape = RoundedCornerShape(8.dp))
                                .padding(vertical = 4.dp)
                                .width(200.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Change Credentials") },
                                onClick = {
                                    showMenu = false
                                    val intent = Intent(context, SecondPageActivity::class.java)
                                    intent.putExtra("editMode", true)
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }


                // Connection status and speed
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .padding(top = 200.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = ssid,
                            color = Color(0xFFC01221),
                            fontSize = 16.sp,
                            fontFamily = SatoshiFontFamily,
                            fontWeight = FontWeight.Bold
                        )

                    }

                    Text(
                        text = networkSpeed,
                        color = Color(0xFFC01221),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SatoshiFontFamily
                    )
                }
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(top = 170.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically)
                {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isConnected) Color(0xFFC01221) else Color(0xFFE53E3E),
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
            }

            // Red bottom section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.home_red_bg),
                    contentDescription = "Red Background",
                    modifier = Modifier
                        .width(widthDp)
                        .scale(2.7f)
                        .offset(y = (-35).dp)
                        .height(heightDp)
                        .align(Alignment.BottomCenter),
                    contentScale = ContentScale.FillBounds
                )




                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 0.dp)
                ) {
                    Spacer(modifier = Modifier.height(0.dp))

                    // Spectrum graph
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFDF0D5))
                            .clickable { onSpectrumClick() }
                    ) {
                        Column(modifier = Modifier
                            .padding(16.dp)
                            ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = "Expand",
                                    tint = Color(0xFFC01221),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (session != null && session.history.isNotEmpty()) {
                                HomeScreenGraph(
                                    modifier = Modifier.fillMaxWidth().height(140.dp),
                                    rateHistory = session.history
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(140.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No data available for graph",
                                        color = Color(0xFFC01221).copy(alpha = 0.7f),
                                        fontSize = 14.sp,
                                        fontFamily = SatoshiFontFamily
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Power button
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-7).dp)
                .size(210.dp)
                .drawBehind {
                    val shadowColor = Color.Black.copy(alpha = 0.7f)
                    val radius = size.minDimension / 2

                    val paint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        color = shadowColor.toArgb()
                        maskFilter = android.graphics.BlurMaskFilter(20f, android.graphics.BlurMaskFilter.Blur.NORMAL)
                    }

                    drawContext.canvas.nativeCanvas.drawCircle(
                        center.x,
                        center.y + 20f,
                        radius,
                        paint
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    status = "Authenticating..."
                    onConnectClick()
                },
                modifier = Modifier
                    .size(210.dp)
                    .clip(CircleShape),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC01221)),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Canvas(modifier = Modifier.size(66.dp)) {
                    val stroke = 7.dp.toPx()
                    val arcR = size.minDimension / 2.2f
                    val topLeft = Offset((size.width - arcR * 2) / 2f, (size.height - arcR * 2) / 2f)

                    drawArc(
                        Color.White,
                        startAngle = -135f,
                        sweepAngle = -270f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                        size = Size(arcR * 2, arcR * 2),
                        topLeft = topLeft
                    )

                    val cx = size.width / 2
                    val cy = size.height / 2
                    drawLine(
                        Color.White,
                        start = Offset(cx, cy - arcR * 1.2f),
                        end = Offset(cx, cy - arcR * 0.6f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
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
        ssid = "AndroidWifi",
        onConnectClick = { }
    )
}

@Preview(showBackground = true)
@Composable
fun HomeScreenOnlinePreview() {
    HomeScreen(
        isConnected = true,
        networkSpeed = "6 mbps",
        onSpectrumClick = { },
        session = null,
        ssid = "Vit S-block 2.4",
        onConnectClick = { }
    )
}
