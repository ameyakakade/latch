package com.vinnovateit.autonetconnector

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinnovateit.autonetconnector.functionality2.manager.SessionSummary
import com.vinnovateit.autonetconnector.ui.components.TooltipHint
import com.vinnovateit.autonetconnector.ui.theme.AutoNetConnectorTheme
import com.vinnovateit.autonetconnector.ui.theme.ColorPowerButtonShadow
import com.vinnovateit.autonetconnector.ui.theme.ColorStatusDisconnected

val SatoshiFontFamily = FontFamily(
    Font(R.font.satoshi_regular, FontWeight.Normal),
    Font(R.font.satoshi_medium, FontWeight.Medium),
    Font(R.font.satoshi_regular, FontWeight.SemiBold),
    Font(R.font.satoshi_bold, FontWeight.Bold)
)

val SakingFontFamily = FontFamily(
    Font(R.font.saking_regular, FontWeight.Normal)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    isConnected: Boolean,
    networkSpeed: String,
    session: SessionSummary?,
    ssid: String,
    onConnectClick: () -> Unit,
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Press the button to run auto-login test.") }
    var showMenu by remember { mutableStateOf(false) }

    // --- State Lifted Up for the Graph ---
    val historyForHomeScreen = session?.history?.takeLast(150) ?: emptyList()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Beige Section (54% height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.54f)
            ) {
                // Background pattern
                Image(
                    painter = painterResource(id = R.drawable.latch_background_home),
                    contentDescription = "Background Pattern",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Column {
                    // Top Bar with Logo and Menu
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 49.dp, start = 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Start item (Logo) - in a fixed-width box for symmetry
                        Box(
                            modifier = Modifier.width(50.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.latch_logo),
                                contentDescription = "LATCH Logo",
                                tint = Color.Unspecified,
                                modifier = Modifier
                                    .size(width = 49.33.dp, height = 36.dp)
                            )
                        }

                        // Middle item (Title) - weighted to take up remaining space
                        Text(
                            text = "LATCH",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 28.97.sp,
                            fontFamily = SakingFontFamily,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = (28.97 * 0.02).sp,
                            lineHeight = (28.97 * 1.0).sp,
                            textAlign = TextAlign.Center
                        )

                        // End item (Menu) - in a fixed-width box for symmetry
                        Box(
                            modifier = Modifier
                                .size(32.dp) // visual wrapper size
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(
                                        bounded = false,      // allows ripple to expand beyond bounds
                                        radius = 24.dp,       // bigger ripple radius for fuller effect
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    onClick = { showMenu = true }
                                ),
                            contentAlignment = Alignment.Center
                        ){
                            TooltipHint(tooltipText = "Menu") {
                                Icon(
                                    imageVector = Icons.Rounded.Menu,
                                    contentDescription = "Menu",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    modifier = Modifier
                                        .width(200.dp),
                                    containerColor = MaterialTheme.colorScheme.surface,          // menu background :contentReference[oaicite:1]{index=1}
                                    tonalElevation = MenuDefaults.TonalElevation,
                                    shadowElevation = MenuDefaults.ShadowElevation,
                                    shape = MenuDefaults.shape
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Change Credentials",
                                                fontSize = 16.sp,
                                                fontFamily = SatoshiFontFamily,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            val intent =
                                                Intent(context, SecondPageActivity::class.java)
                                            intent.putExtra("editMode", true)
                                            context.startActivity(intent)
                                        },
                                        leadingIcon = null,
                                        trailingIcon = null,
                                        colors = MenuDefaults.itemColors(
                                            textColor = MaterialTheme.colorScheme.primary,
                                            disabledTextColor = MaterialTheme.colorScheme.surfaceDim
                                        ),
                                        contentPadding = MenuDefaults.DropdownMenuItemContentPadding
                                    )
                                }
                            }
                        }
                    }


                    Spacer(modifier = Modifier.weight(1f))

                    // Connection Status & Speed
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .padding(bottom = 180.dp) // Adjusted padding
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isConnected) MaterialTheme.colorScheme.primary else ColorStatusDisconnected,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isConnected) "CONNECTED" else "DISCONNECTED",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SatoshiFontFamily
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ssid,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 16.sp,
                                    fontFamily = SatoshiFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = networkSpeed,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SatoshiFontFamily
                                )
                            }
                        }
                    }
                }
            }

            // Red bottom section (46% height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.46f),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Red background image
                Image(
                    painter = painterResource(id = R.drawable.home_red_bg),
                    contentDescription = "Red Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )

                SpectrumCard(session, ssid, historyForHomeScreen)
            }
        }

        // Power button - Overlays everything, centered on the screen
        Box(
            modifier = Modifier
                .align(Alignment.Center) // Centered alignment
                .size(210.dp)
                .drawBehind {
                    val shadowColor = ColorPowerButtonShadow
                    val radius = size.minDimension / 2
                    val paint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        color = shadowColor.toArgb()
                        maskFilter =
                            android.graphics.BlurMaskFilter(20f, android.graphics.BlurMaskFilter.Blur.NORMAL)
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                // Hoist the color outside the Canvas
                val powerIconColor = MaterialTheme.colorScheme.onPrimary
                Canvas(modifier = Modifier.size(66.dp)) {
                    val stroke = 7.dp.toPx()
                    val arcR = size.minDimension / 2.2f
                    val topLeft = Offset((size.width - arcR * 2) / 2f, (size.height - arcR * 2) / 2f)

                    drawArc(
                        color = powerIconColor,
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
                        color = powerIconColor,
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
    AutoNetConnectorTheme {
        HomeScreen(
            isConnected = false,
            networkSpeed = "6 mbps",
            session = null,
            ssid = "Not Connected",
            onConnectClick = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenOnlinePreview() {
    AutoNetConnectorTheme {
        HomeScreen(
            isConnected = true,
            networkSpeed = "12 mbps",
            session = null,
            ssid = "VIT-WiFi",
            onConnectClick = { }
        )
    }
}
