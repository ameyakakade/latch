package com.vinnovateit.latch.features.home

import android.app.Activity
import android.content.Intent
import android.graphics.BlurMaskFilter
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinnovateit.latch.R
import com.vinnovateit.latch.common.ui.LeafOverlay
import com.vinnovateit.latch.common.util.TooltipHint
import com.vinnovateit.latch.domain.model.LiveDataPoint
import com.vinnovateit.latch.domain.model.SessionSummary
import com.vinnovateit.latch.features.about.MeetTheTeamActivity
import com.vinnovateit.latch.features.home.components.SpectrumCard
import com.vinnovateit.latch.features.onboarding.OnboardingActivity
import com.vinnovateit.latch.features.settings.SettingsActivity
import com.vinnovateit.latch.features.settings.manager.SettingsManager
import com.vinnovateit.latch.features.wifi.background.ForegroundService
import com.vinnovateit.latch.features.wifi.manager.ConnectionStatus
import com.vinnovateit.latch.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isConnected: Boolean,
    networkSpeed: String,
    session: SessionSummary?,
    connectionStatus: ConnectionStatus,
    speedUnit: String
) {
    val historyForHomeScreen = session?.history?.takeLast(150) ?: emptyList()
    val context = LocalContext.current
    val autoLoginEnabled by SettingsManager.autoLogin.collectAsStateWithLifecycle()

    val view = LocalView.current
    val isDarkTheme = isSystemInDarkTheme()
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDarkTheme
            insetsController.isAppearanceLightNavigationBars = !isDarkTheme
        }
    }

    val smartOnConnectClick = {
        if (autoLoginEnabled) {
            val intent = Intent(context, ForegroundService::class.java).apply {
                action = ForegroundService.ACTION_TRIGGER_LOGOUT
            }
            context.startService(intent)
            SettingsManager.setAutoLogin(false)
        } else {
            val intent = Intent(context, ForegroundService::class.java).apply {
                action = ForegroundService.ACTION_TRIGGER_LOGIN_CHECK
            }
            context.startService(intent)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isPortrait = maxHeight > maxWidth

        if (isPortrait) {
            PortraitHomeScreen(
                isConnected = isConnected,
                networkSpeed = networkSpeed,
                session = session,
                onConnectClick = smartOnConnectClick as () -> Unit,
                historyForHomeScreen = historyForHomeScreen,
                connectionStatus = connectionStatus,
                speedUnit = speedUnit,
            )
        } else {
            LandscapeHomeScreen(
                isConnected = isConnected,
                networkSpeed = networkSpeed,
                session = session,
                onConnectClick = smartOnConnectClick as () -> Unit,
                historyForHomeScreen = historyForHomeScreen,
                connectionStatus = connectionStatus,
                speedUnit = speedUnit,
            )
        }
    }
}

@Composable
fun PortraitHomeScreen(
    isConnected: Boolean,
    networkSpeed: String,
    session: SessionSummary?,
    onConnectClick: () -> Unit,
    historyForHomeScreen: List<LiveDataPoint>,
    connectionStatus: ConnectionStatus,
    speedUnit: String,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalResources.current.displayMetrics.widthPixels.toFloat() }
    val buttonDiameterPx = screenWidthPx * 0.6f // Increased from 0.5f
    val colorScheme = MaterialTheme.colorScheme

    Box(modifier = Modifier.fillMaxSize()) {
        LeafOverlay(
            contentDescription = "Background Pattern",
            modifier = Modifier.fillMaxWidth(),
            alignment = Alignment.TopCenter,
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // --- TOP 50% SECTION ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .statusBarsPadding()
            ) {
                TopBarSection(
                    onPreferencesClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) },
                    onHowItWorksClick = { context.startActivity(Intent(context, OnboardingActivity::class.java).apply { putExtra("start_from_step_one", true) }) },
                    onMeetTheTeamClick = { context.startActivity(Intent(context, MeetTheTeamActivity::class.java)) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                NetworkStatusRow(isConnected = isConnected, networkSpeed = networkSpeed)
                Spacer(modifier = Modifier.height(60.dp))
            }

            // --- BOTTOM 50% SECTION ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .graphicsLayer(alpha = 0.99f)
                    .drawBehind {
                        drawRect(color = colorScheme.primaryContainer, size = size)
                        val cutoutRatio = 0.9f
                        val cutoutDiameter = buttonDiameterPx * cutoutRatio
                        val cutoutRadius = cutoutDiameter / 2f
                        val circleTopLeft = Offset(x = (size.width - cutoutDiameter) / 2f, y = -cutoutRadius)
                        drawArc(
                            color = Color.Transparent, startAngle = 0f, sweepAngle = 180f,
                            useCenter = true, topLeft = circleTopLeft, size = Size(cutoutDiameter, cutoutDiameter),
                            blendMode = BlendMode.Clear
                        )
                    }
                    .navigationBarsPadding(),
                contentAlignment = Alignment.BottomCenter
            ) {
                SpectrumCard(session, historyForHomeScreen, connectionStatus, speedUnit, false)
            }
        }
        // --- CENTRAL OVERLAY ---
        PowerButtonOverlay(
            onConnectClick = onConnectClick,
            isConnected = isConnected,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun LandscapeHomeScreen(
    isConnected: Boolean,
    networkSpeed: String,
    session: SessionSummary?,
    onConnectClick: () -> Unit,
    historyForHomeScreen: List<LiveDataPoint>,
    connectionStatus: ConnectionStatus,
    speedUnit: String,
) {
    val context = LocalContext.current

    LeafOverlay(
        contentDescription = "Background Pattern",
        modifier = Modifier.fillMaxSize(),
        alignment = Alignment.TopCenter,
        contentScale = ContentScale.Crop
    )
    Row(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        // --- LEFT SECTION (CONTROLS) ---
        Box(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TopBarSection(
                    onPreferencesClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) },
                    onHowItWorksClick = { context.startActivity(Intent(context, OnboardingActivity::class.java).apply { putExtra("start_from_step_one", true) }) },
                    onMeetTheTeamClick = { context.startActivity(Intent(context, MeetTheTeamActivity::class.java)) },
                )
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.fillMaxWidth().weight(0.3f), contentAlignment = Alignment.Center) {
                        NetworkStatusRow(isConnected, networkSpeed)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().weight(0.7f).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                        LandscapePowerButton(
                            modifier = Modifier.fillMaxSize(),
                            onConnectClick = onConnectClick,
                            isConnected = isConnected
                        )
                    }
                }
            }
        }
        // --- RIGHT SECTION (GRAPH) ---
        Box(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            SpectrumCard(
                session = session,
                historyForHomeScreen,
                connectionStatus = connectionStatus,
                speedUnit = speedUnit,
                isLandscape = true,
            )
        }
    }
}

@Composable
fun NetworkStatusRow(isConnected: Boolean, networkSpeed: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(if (isConnected) ColorBoxConnected else ColorBoxDisconnected, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isConnected) "CONNECTED" else "DISCONNECTED",
                color = if (isConnected) ColorStatusConnected else ColorStatusDisconnected,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SatoshiFontFamily
            )
        }
        Text(
            text = networkSpeed,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SatoshiFontFamily
        )
    }
}

@Composable
fun PowerButtonOverlay(
    onConnectClick: () -> Unit,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val buttonDiameterDp = with(density) {
        (LocalResources.current.displayMetrics.widthPixels * 0.48f).toDp()
    }

    val rotation by animateFloatAsState(
        targetValue = if (isConnected) 0f else 180f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "powerIconRotation"
    )

    Box(
        modifier = modifier
            .size(buttonDiameterDp)
            .drawBehind {
                val shadowColor = ColorPowerButtonShadow
                val radius = size.minDimension / 2
                val paint = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = shadowColor.toArgb()
                    maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
                }
                drawContext.canvas.nativeCanvas.drawCircle(center.x, center.y + 20f, radius, paint)
            },
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onConnectClick,
            modifier = Modifier.fillMaxSize().clip(CircleShape),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.PowerSettingsNew,
                contentDescription = "Power Button",
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
    }
}

@Composable
fun LandscapePowerButton(
    modifier: Modifier = Modifier,
    onConnectClick: () -> Unit,
    isConnected: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressedFromInteraction by interactionSource.collectIsPressedAsState()
    var pressedManual by remember { mutableStateOf(false) }
    val isPressed = pressedFromInteraction || pressedManual

    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 24.dp else 50.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cornerRadiusAnim"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isConnected) 0f else 180f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "powerIconRotation"
    )

    Button(
        onClick = onConnectClick,
        interactionSource = interactionSource,
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onPress = {
                pressedManual = true
                try { awaitRelease() } finally { pressedManual = false }
            })
        },
        shape = RoundedCornerShape(cornerRadius),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PowerSettingsNew,
                contentDescription = "Power Button",
                modifier = Modifier
                    .fillMaxSize(fraction = 0.5f)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarSection(
    onPreferencesClick: () -> Unit,
    onHowItWorksClick: () -> Unit,
    onMeetTheTeamClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    var menuExpanded by remember { mutableStateOf(false) }
    CenterAlignedTopAppBar(
        title = {
            Text(modifier = Modifier.padding(top = 5.dp), text = stringResource(R.string.app_name_uppercase), color = MaterialTheme.colorScheme.primary, fontSize = 23.sp, fontFamily = ModernizFontFamily, fontWeight = FontWeight.Normal, textAlign = TextAlign.Center)
        },
        navigationIcon = {
            Icon(painter = if (isDark) painterResource(id = R.drawable.ic_latch_dark) else painterResource(id = R.drawable.ic_latch_light), contentDescription = "LATCH Logo", tint = Color.Unspecified, modifier = Modifier.size(48.dp).padding(start = 12.dp))
        },
        actions = {
            TooltipHint(tooltipText = "More options") {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(imageVector = Icons.Rounded.Menu, contentDescription = "More options", tint = MaterialTheme.colorScheme.primary)
                }
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, shape = RoundedCornerShape(12.dp), containerColor = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.width(200.dp)) {
                DropdownMenuItem(text = { Text("How It Works", fontSize = 16.sp, fontFamily = SatoshiFontFamily) }, onClick = { menuExpanded = false; onHowItWorksClick() }, leadingIcon = { Icon(Icons.Rounded.QuestionMark, contentDescription = "How It Works") })
                DropdownMenuItem(text = { Text("Settings", fontSize = 16.sp, fontFamily = SatoshiFontFamily) }, onClick = { menuExpanded = false; onPreferencesClick() }, leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") })
                DropdownMenuItem(text = { Text("Meet The Team", fontSize = 16.sp, fontFamily = SatoshiFontFamily) }, onClick = { menuExpanded = false; onMeetTheTeamClick() }, leadingIcon = { Icon(Icons.Rounded.Groups, contentDescription = "Meet The Team") })
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent, navigationIconContentColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.primary, actionIconContentColor = MaterialTheme.colorScheme.primary)
    )
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun HomeScreenPortraitPreview() {
    LatchTheme {
        HomeScreen(isConnected = false, networkSpeed = "6 mbps", session = null, connectionStatus = ConnectionStatus.Idle, "B/s")
    }
}

@Preview(showBackground = true, device = "spec:width=891dp,height=411dp")
@Composable
fun HomeScreenLandscapePreview() {
    LatchTheme {
        HomeScreen(isConnected = true, networkSpeed = "12 mbps", session = null, connectionStatus = ConnectionStatus.Idle, speedUnit = "B/s")
    }
}