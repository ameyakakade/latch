package com.vinnovateit.latch.features.onboarding

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.vinnovateit.latch.R
import com.vinnovateit.latch.common.ui.HandsConnectAnimation
import com.vinnovateit.latch.data.StoredCredentials
import com.vinnovateit.latch.features.home.MainActivity
import com.vinnovateit.latch.ui.theme.LatchTheme
import com.vinnovateit.latch.ui.theme.ModernizFontFamily
import com.vinnovateit.latch.ui.theme.SatoshiFontFamily
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)

        setContent {
            LatchTheme {
                OnboardingScreen(
                    onComplete = {
                        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                        prefs.edit { putBoolean("hasSeenOnboarding", true) }

                        startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

data class SlideContent(
    val title: String,
    val description: AnnotatedString,
    val icon: @Composable () -> Unit,
    val icons: ImmutableList<Int> = persistentListOf()
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var credentialsHandled by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var showCredentialsAlert by remember { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    val offsetX = remember { Animatable(0f) }
    val intent = (context as? Activity)?.intent
    val startFromStepOne = intent?.getBooleanExtra("start_from_step_one", false) ?: false

    LaunchedEffect(Unit) {
        if (StoredCredentials.credentialsExist(context)) {
            credentialsHandled = true
        }
    }

    val slides = remember {
        listOf(
            // Welcome slide
            SlideContent(
                title = "Welcome to Latch",
                description = buildAnnotatedString {
                    append("Let's get everything setup for you.")
                },
                icon = {},
                icons = persistentListOf()
            ),
            SlideContent(
                title = "How it Works",
                description = buildAnnotatedString {
                    append("Latch will handle the \"Sign-in to Network\" page for you every time you connect to the network with your credentials.")
                },
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.captive_portal_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(80.dp)
                    )
                }
            ),

            // Allow Permissions
            SlideContent(
                title = "Enable Notifications",
                description = buildAnnotatedString {
                    append("To monitor your connection and provide status updates, Latch runs a service that requires a persistent notification. You can minimize or hide it from your phone's settings at any time.")
                },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(80.dp)
                    )
                }
            ),

            // Set Up Account
            SlideContent(
                title = "Your Account",
                description = buildAnnotatedString {
                    append("Please provide your credentials. And we will handle the magic for you.")
                },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(80.dp)
                    )
                }
            ),
            SlideContent(
                title = "Convenient Access",
                description = buildAnnotatedString {
                    append("For quick access, add the Latch widget to your home screen or the tile to your Quick Settings panel.")
                },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(80.dp)
                    )
                }
            ),

            // Final
            SlideContent(
                title = "You're Ready!",
                description = buildAnnotatedString {
                    append("Your setup is complete. Latch will now handle your Wi-Fi sign-in.")
                },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF1BA83C),
                        modifier = Modifier.size(80.dp)
                    )
                }
            )
        )
    }

    val pagerState = rememberPagerState(
        initialPage = if (startFromStepOne) 1 else 0,
        pageCount = { slides.size }
    )

    // This effect prevents scrolling back to the welcome page if started from step one
    LaunchedEffect(pagerState.targetPage) {
        if (startFromStepOne && pagerState.targetPage == 0) {
            pagerState.scrollToPage(1)
        }
    }

    // This effect handles blocking forward swipe from the credentials page
    LaunchedEffect(pagerState.isScrollInProgress, credentialsHandled, permissionGranted) {
        if (pagerState.isScrollInProgress) {
            if (pagerState.currentPage == 2 && pagerState.targetPage > 2 && !permissionGranted) {
                scope.launch {
                    pagerState.scrollToPage(2)
                }
            }
            if (pagerState.currentPage == 3 && pagerState.targetPage > 3 && !credentialsHandled) {
                scope.launch {
                    pagerState.scrollToPage(3)
                }
            }
        }
    }

    val credentialsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            credentialsHandled = true
            scope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    Scaffold(
        bottomBar = {
            LatchSetupBottomBar(
                pagerState = pagerState,
                isFinishButtonEnabled = when (pagerState.currentPage) {
                    2 -> permissionGranted
                    3 -> credentialsHandled
                    else -> true
                },
                onNextClicked = {
                    scope.launch {
                        if (pagerState.currentPage == 3 && !credentialsHandled) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            offsetX.animateTo(20f, tween(50))
                            offsetX.animateTo(-20f, tween(50))
                            offsetX.animateTo(10f, tween(50))
                            offsetX.animateTo(-10f, tween(50))
                            offsetX.animateTo(0f, tween(50))
                            return@launch
                        }

                        if (pagerState.currentPage < slides.size - 1) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                onFinishClicked = {
                    when (pagerState.currentPage) {
                        slides.size - 1 -> onComplete()
                        else -> {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                },
                modifier = Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) }
            )
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = when (pagerState.currentPage) {
                2 -> permissionGranted
                3 -> credentialsHandled
                else -> true
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { pageIndex ->
            AnimatedContent(
                targetState = pageIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut())
                    }.using(SizeTransform(clip = false))
                },
                label = "OnboardingPageAnimation"
            ) { targetPage ->
                when (targetPage) {
                    0 -> WelcomeToLatchPage()
                    2 -> NotificationPermissionPage(slides[targetPage], onPermissionGranted = { permissionGranted = true })
                    3 -> SetUpAccountPage(slides[targetPage], onCredentialsClick = {
                        credentialsLauncher.launch(
                            Intent(context, SecondPageActivity::class.java).apply {
                                putExtra("fromOnboarding", true)
                            }
                        )
                    })
                    else -> StandardSlidePage(slides[targetPage])
                }
            }
        }
    }
    if (showCredentialsAlert) {
        AlertDialog(
            onDismissRequest = { showCredentialsAlert = false },
            title = { Text("Enter Credentials") },
            text = { Text("Please enter your credentials before proceeding.") },
            confirmButton = {
                TextButton(onClick = { showCredentialsAlert = false }) {
                    Text("OK")
                }
            }
        )
    }
}


@Composable
fun WelcomeToLatchPage() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Welcome to Latch",
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 32.sp,
                lineHeight = 1.6.em,
                fontFamily = ModernizFontFamily,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Start,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp, start = 25.dp, end = 25.dp)
        )

        HandsConnectAnimation()

        Text(
            text = "Let's get everything set up for you.",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontFamily = SatoshiFontFamily,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Start,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 32.dp, bottom = 30.dp)
        )
    }
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionPage(
    slide: SlideContent,
    onPermissionGranted: () -> Unit
) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else ""

    val notificationPermissionState = rememberPermissionState(
        permission = permission
    ) { granted -> if (granted) onPermissionGranted() }

    val isGranted = notificationPermissionState.status.isGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU

    LaunchedEffect(isGranted) {
        if (isGranted) onPermissionGranted()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = slide.title,
            fontFamily = SatoshiFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(start = 28.dp, top = 28.dp)
                .align(Alignment.TopStart)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) { slide.icon() }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = slide.description.text,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
                textAlign = TextAlign.Center,
                fontFamily = SatoshiFontFamily
            )

            Spacer(modifier = Modifier.height(44.dp))

            Button(
                onClick = {
                    if (!isGranted) {
                        notificationPermissionState.launchPermissionRequest()
                    }
                },
                enabled = !isGranted,
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
            ) {
                if (isGranted) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isGranted) "Permission Granted" else "Grant Permission",
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}


@Composable
fun SetUpAccountPage(
    slide: SlideContent,
    onCredentialsClick: () -> Unit
) {
    val context = LocalContext.current
    var credentialsExist by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        credentialsExist = StoredCredentials.credentialsExist(context)
    }

    Box(modifier = Modifier.fillMaxSize().padding(bottom = 32.dp)) {
        Text(
            text = slide.title,
            fontFamily = SatoshiFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(start = 28.dp, top = 28.dp)
                .align(Alignment.TopStart)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) { slide.icon() }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = slide.description.text,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
                textAlign = TextAlign.Center,
                fontFamily = SatoshiFontFamily
            )

            Spacer(modifier = Modifier.height(44.dp))

            Button(
                onClick = onCredentialsClick,
                enabled = !credentialsExist,
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
            ) {
                if (credentialsExist) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (credentialsExist) "Credentials Set" else "Set Up Credentials",
                    fontFamily = SatoshiFontFamily,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.encrypted_24px),
                    contentDescription = "Security Notice",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = buildAnnotatedString {
                        append("Latch does not collect your data. Your credentials are encrypted and ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("stored securely on your device.")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
fun StandardSlidePage(slide: SlideContent) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = slide.title,
            fontFamily = SatoshiFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(start = 28.dp, top = 30.dp)
                .align(Alignment.TopStart)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                slide.icon()
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = slide.description,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
                textAlign = TextAlign.Center,
                fontFamily = SatoshiFontFamily
            )
        }
    }
}


@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun LatchSetupBottomBar(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    onNextClicked: () -> Unit,
    onFinishClicked: () -> Unit,
    isFinishButtonEnabled: Boolean
) {
    val morphAnimationSpec = tween<Float>(durationMillis = 600, easing = FastOutSlowInEasing)
    val rotationAnimationSpec = tween<Float>(durationMillis = 900, easing = FastOutSlowInEasing)

    val targetShapeValues = when (pagerState.currentPage % 3) {
        0 -> listOf(50f, 50f, 50f, 50f) // Circle
        1 -> listOf(26f, 26f, 26f, 26f) // Rounded square
        else -> listOf(18f, 50f, 18f, 50f) // Leaf shape
    }

    val animatedTopStart by animateFloatAsState(targetShapeValues[0], morphAnimationSpec, label = "TopStart")
    val animatedTopEnd by animateFloatAsState(targetShapeValues[1], morphAnimationSpec, label = "TopEnd")
    val animatedBottomStart by animateFloatAsState(targetShapeValues[2], morphAnimationSpec, label = "BottomStart")
    val animatedBottomEnd by animateFloatAsState(targetShapeValues[3], morphAnimationSpec, label = "BottomEnd")

    val animatedRotation by animateFloatAsState(
        targetValue = pagerState.currentPage * 360f,
        animationSpec = rotationAnimationSpec,
        label = "Rotation"
    )

    Surface(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp), clip = true),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(
            topStart = 36.dp,
            topEnd = 36.dp,
            bottomStart = 36.dp,
            bottomEnd = 36.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    AnimatedContent(
                        targetState = pagerState.currentPage,
                        transitionSpec = {
                            // Fade transition for image <-> text
                            if (initialState == 0 || targetState == 0) {
                                (fadeIn(animationSpec = tween(220, delayMillis = 90)) togetherWith
                                        fadeOut(animationSpec = tween(90)))
                                    .using(SizeTransform(clip = false))
                            } else { // Vertical slide for text <-> text
                                (slideInVertically { height -> height } + fadeIn())
                                    .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                                    .using(SizeTransform(clip = false))
                            }
                        },
                        label = "LogoVsStepText"
                    ) { currentPage ->
                        if (currentPage == 0) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_vinnovateit),
                                contentDescription = "VinnovateIT Logo",
                                modifier = Modifier
                                    .size(110.dp)
                                    .offset(x = 10.dp)
                            )
                        } else {
                            Text(
                                text = "Step $currentPage of ${pagerState.pageCount - 1}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = SatoshiFontFamily,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }


                // Next / Finish Button
                val isLastPage = pagerState.currentPage == pagerState.pageCount - 1

                val containerColor = if (!isFinishButtonEnabled) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }

                val contentColor = if (!isFinishButtonEnabled) {
                    MaterialTheme.colorScheme.onSurface.copy()
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }


                MediumFloatingActionButton(
                    onClick = {
                        if (!isFinishButtonEnabled) return@MediumFloatingActionButton
                        if (isLastPage) {
                            onFinishClicked()
                        } else {
                            onNextClicked()
                        }
                    },
                    shape = RoundedCornerShape(
                        topStart = animatedTopStart.toInt().dp,
                        topEnd = animatedTopEnd.toInt().dp,
                        bottomStart = animatedBottomStart.toInt().dp,
                        bottomEnd = animatedBottomEnd.toInt().dp
                    ),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    containerColor = containerColor,
                    contentColor = contentColor,
                    modifier = Modifier
                        .rotate(animatedRotation)
                        .padding(end = 0.dp)
                ) {
                    AnimatedContent(
                        modifier = Modifier.rotate(-animatedRotation),
                        targetState = pagerState.currentPage < pagerState.pageCount - 1,
                        transitionSpec = {
                            ContentTransform(
                                targetContentEnter = fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                        scaleIn(initialScale = 0.9f, animationSpec = tween(220, delayMillis = 90)),
                                initialContentExit = fadeOut(animationSpec = tween(90)) +
                                        scaleOut(targetScale = 0.9f, animationSpec = tween(90))
                            ).using(SizeTransform(clip = false))
                        },
                        label = "AnimatedFabIcon"
                    ) { isNextPage ->
                        if (isNextPage) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = "Next")
                        } else {
                            if (isFinishButtonEnabled) {
                                Icon(Icons.Rounded.Check, contentDescription = "Finish")
                            } else {
                                Icon(Icons.Rounded.Close, contentDescription = "Finish")
                            }
                        }
                    }
                }
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun PreviewFirstPage() {
    WelcomeToLatchPage()
}
