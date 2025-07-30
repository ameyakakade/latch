package com.vinnovateit.autonetconnector

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import com.vinnovateit.autonetconnector.functionality2.manager.SessionSummary
import com.vinnovateit.autonetconnector.ui.components.TooltipHint
import com.vinnovateit.autonetconnector.ui.theme.AutoNetConnectorTheme
import com.vinnovateit.autonetconnector.ui.theme.ColorPowerButtonShadow
import com.vinnovateit.autonetconnector.ui.theme.ColorStatusDisconnected

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
fun HomeRedCanvasBackground(buttonSizePx: Float) {
    val cutoutRatio = 1.2f
    val cutoutDiameter = buttonSizePx * cutoutRatio
    val cutoutRadius = cutoutDiameter / 2f

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(alpha = 0.99f)
    ) {
        val canvasWidth = size.width
        val circleTopLeft = Offset(
            x = (canvasWidth - cutoutDiameter) / 2f,
            y = -cutoutRadius
        )

        drawRect(
            color = Color(0xFFC8102E),
            size = size
        )

        drawArc(
            color = Color.Transparent,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = circleTopLeft,
            size = Size(cutoutDiameter, cutoutDiameter),
            blendMode = BlendMode.Clear
        )
    }
}

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
    val historyForHomeScreen = session?.history?.takeLast(150) ?: emptyList()

    val density = LocalDensity.current
    val screenWidthDp = LocalContext.current.resources.displayMetrics.widthPixels / density.density
    val buttonDiameterDp = (screenWidthDp * 0.5f).dp
    val cutoutRatio = 1.2f
    val cutoutDiameterDp = (screenWidthDp * 0.6f).dp
    val spacingDp = ((screenWidthDp * 0.1f) / 2f).dp
    val screenHeightDp = LocalContext.current.resources.displayMetrics.heightPixels / density.density

    val buttonDiameterPx = with(density) { buttonDiameterDp.toPx() }
    val logoRes = R.drawable.ic_latch_dark

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.54f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.latch_background_home),
                    contentDescription = "Background Pattern",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = (screenHeightDp * 0.06f).dp, start = 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(50.dp), contentAlignment = Alignment.CenterStart) {
                            Icon(
                                painter = painterResource(id = logoRes),
                                contentDescription = "LATCH Logo",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(width = 49.33.dp, height = 36.dp)
                            )
                        }

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

                        Box(modifier = Modifier.width(50.dp), contentAlignment = Alignment.CenterEnd) {
                            TooltipHint(tooltipText = "Menu") {
                                Box {
                                    Icon(
                                        imageVector = Icons.Rounded.Menu,
                                        contentDescription = "Menu",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable { showMenu = true }
                                    )
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false },
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surface)
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
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .padding(bottom = (screenHeightDp * 0.2f).dp)
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.46f),
                contentAlignment = Alignment.BottomCenter
            ) {
                HomeRedCanvasBackground(buttonSizePx = buttonDiameterPx)
                SpectrumCard(session, ssid, historyForHomeScreen)
            }
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val screenHeight = constraints.maxHeight.toFloat()
            val screenWidth = constraints.maxWidth.toFloat()

            val cutoutDiameter = screenWidth * 0.6f
            val cutoutCenterY = screenHeight * 0.54f
            val cutoutBottomY = cutoutCenterY + (cutoutDiameter / 2f)

            val spacing = screenWidth * 0.05f
            val buttonDiameter = screenWidth * 0.5f

            val buttonTopY = cutoutBottomY - spacing - buttonDiameter

            Box(
                modifier = Modifier
                    .absoluteOffset(y = with(LocalDensity.current) { buttonTopY.toDp() })
                    .align(Alignment.TopCenter)
                    .size(with(LocalDensity.current) { buttonDiameter.toDp() })
                    .drawBehind {
                        val shadowColor = ColorPowerButtonShadow
                        val radius = size.minDimension / 2
                        val paint = Paint().asFrameworkPaint().apply {
                            isAntiAlias = true
                            color = shadowColor.toArgb()
                            maskFilter = android.graphics.BlurMaskFilter(20f, android.graphics.BlurMaskFilter.Blur.NORMAL)
                        }
                        drawContext.canvas.nativeCanvas.drawCircle(center.x, center.y + 20f, radius, paint)
                    },
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onConnectClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    val powerIconColor = MaterialTheme.colorScheme.onPrimary
                    Canvas(modifier = Modifier.size(with(LocalDensity.current) { (buttonDiameter / 3).toDp() })) {
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