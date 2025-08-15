package com.vinnovateit.latch.features.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinnovateit.latch.R
import com.vinnovateit.latch.features.auth.LandingPageActivity
import com.vinnovateit.latch.ui.theme.LatchTheme

data class OnboardingPage(
    val title: String,
    val description: AnnotatedString,
    val foregroundIcon: ImageVector? = null
)

class OnboardingActivity : ComponentActivity() {

    @Composable
    private fun createOnboardingPages(): List<OnboardingPage> {
        return listOf(
            OnboardingPage(
                title = "Welcome to LATCH",
                description = buildAnnotatedString { append("Auto-login to VIT Wi-Fi effortlessly.") },
                foregroundIcon = ImageVector.vectorResource(id = R.drawable.ic_latch)
            ),
            OnboardingPage(
                title = "No Sign-in Hassle",
                description = buildAnnotatedString { append("Latch logs you in automatically — no typing.") },
                foregroundIcon = Icons.Rounded.WifiLock
            ),
            OnboardingPage(
                title = "How it Works",
                description = buildAnnotatedString {
                    append("• Enter your VIT credentials once.\n• Latch auto-submits on hostel Wi-Fi.\n")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Credentials stored securely.") }
                },
                foregroundIcon = Icons.Rounded.Key
            ),
            OnboardingPage(
                title = "Set Up Account",
                description = buildAnnotatedString { append("Enter VIT ID & password to start auto-login.") },
                foregroundIcon = Icons.Rounded.Person
            ),
            OnboardingPage(
                title = "Widgets & Tile",
                description = buildAnnotatedString { append("Add Latch widget or Quick Settings tile for one-tap access.") },
                foregroundIcon = Icons.Rounded.Widgets
            ),
            OnboardingPage(
                title = "You’re Ready!",
                description = buildAnnotatedString { append("Latch will auto-login whenever in range.") },
                foregroundIcon = Icons.Rounded.CheckCircle
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LatchTheme {
                OnboardingEntry(
                    pages = createOnboardingPages(),
                    onFinish = { finishOnboarding() }
                )
            }
        }
    }

    private fun finishOnboarding() {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("hasSeenOnboarding", true)
            .apply()
        startActivity(Intent(this, LandingPageActivity::class.java))
        finish()
    }
}

@Composable
fun OnboardingEntry(
    pages: List<OnboardingPage>,
    onFinish: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        OnboardingScreen(pages = pages, onFinish = onFinish)
    }
}

@Composable
fun OnboardingScreen(
    pages: List<OnboardingPage>,
    onFinish: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    val satoshiFont = FontFamily(Font(R.font.satoshi_regular))
    val modernizFont = FontFamily(Font(R.font.moderniz))

    Surface(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                (slideInHorizontally(
                    initialOffsetX = { width -> if (targetState > initialState) width else -width },
                    animationSpec = tween(550, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(500)) + scaleIn(initialScale = 0.95f, animationSpec = tween(550))) togetherWith
                        (slideOutHorizontally(
                            targetOffsetX = { width -> if (targetState > initialState) -width else width },
                            animationSpec = tween(550)
                        ) + fadeOut(tween(500)) + scaleOut(targetScale = 1.05f, animationSpec = tween(550)))
            },
            label = "onboarding_slide"
        ) { targetStep ->
            OnboardingPageContent(
                page = pages[targetStep],
                currentStep = targetStep,
                totalSteps = pages.size,
                satoshiFont = satoshiFont,
                modernizFont = modernizFont,
                onNext = {
                    if (currentStep < pages.size - 1) {
                        currentStep++
                    } else {
                        onFinish()
                    }
                },
                onSkip = onFinish
            )
        }
    }
}

@Composable
fun OnboardingPageContent(
    page: OnboardingPage,
    currentStep: Int,
    totalSteps: Int,
    satoshiFont: FontFamily,
    modernizFont: FontFamily,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                page.foregroundIcon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = if (page.title == "You’re Ready!") Color(0xFF1BA83C) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(60.dp)
                            .padding(bottom = 30.dp)
                    )
                }

                Text(
                    text = page.title,
                    fontSize = 20.sp,
                    fontFamily = modernizFont,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                )

                Text(
                    text = page.description,
                    fontSize = 18.sp,
                    lineHeight = 30.sp,
                    fontFamily = satoshiFont,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .padding(horizontal = 16.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSkip) {
                    Text(
                        "Skip",
                        fontFamily = modernizFont,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row {
                    repeat(totalSteps) { index ->
                        val isSelected = index == currentStep
                        val dotSize by animateDpAsState(
                            targetValue = if (isSelected) 12.dp else 8.dp,
                            animationSpec = tween(300),
                            label = "dotSize"
                        )
                        val dotColor by animateColorAsState(
                            targetValue = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            animationSpec = tween(300),
                            label = "dotColor"
                        )
                        Box(
                            modifier = Modifier
                                .size(dotSize)
                                .padding(horizontal = 3.dp)
                                .background(dotColor, CircleShape)
                        )
                    }
                }

                Button(onClick = onNext) {
                    Text(
                        if (currentStep < totalSteps - 1) "Next" else "Get Started",
                        fontFamily = modernizFont
                    )
                }
            }
        }
    }
}
