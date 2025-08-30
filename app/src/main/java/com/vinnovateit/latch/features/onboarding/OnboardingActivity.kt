package com.vinnovateit.latch.features.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults.contentPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinnovateit.latch.R
import com.vinnovateit.latch.features.auth.LandingPageActivity
import com.vinnovateit.latch.ui.theme.LatchTheme

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LatchTheme {
                OnboardingScreen(
                    onSkip = { finishOnboarding() },
                    onNext = { finishOnboarding() }
                )
            }
        }
    }

    private fun finishOnboarding() {
        getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("hasSeenOnboarding", true)
            .apply()
        startActivity(Intent(this, LandingPageActivity::class.java))
        finish()
    }
}

data class SlideContent(
    val title: String,
    val description: AnnotatedString,
    val icon: @Composable (() -> Unit)
)

@Composable
fun LatchProgressBar(
    steps: Int,
    current: Int,
    selectedColor: Color,
    unselectedColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(steps) { index ->
            if (index == current) {
                Box(
                    Modifier
                        .width(8.dp)
                        .height(15.dp)
                        .background(selectedColor, shape = RoundedCornerShape(50))
                )
            } else {
                Box(
                    Modifier
                        .width(8.dp)
                        .height(4.dp)
                        .background(unselectedColor, shape = RoundedCornerShape(50))
                )
            }
            if (index != steps - 1) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    onSkip: () -> Unit = {},
    onNext: () -> Unit = {}
) {
    var step by remember { mutableStateOf(0) }

    val satoshiFont = FontFamily(Font(R.font.satoshi_regular))
    val modernizFont = FontFamily(Font(R.font.moderniz))

    val slides = listOf(
        SlideContent(
            title = "Welcome to LATCH",
            description = buildAnnotatedString {
                append("Auto-login to VIT Wi-Fi effortlessly.")
            },
            icon = {
                Image(
                    painter = painterResource(id = R.drawable.ic_latch_light),
                    contentDescription = "Latch Logo",
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .size(36.dp)
                )
            }
        ),
        SlideContent(
            title = "No Sign-in Hassle",
            description = buildAnnotatedString {
                append("Latch logs you in automatically — no typing.")
            },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.WifiLock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        ),
        SlideContent(
            title = "How it Works",
            description = buildAnnotatedString {
                append("• Enter your VIT credentials once.\n")
                append("• Latch auto-submits on hostel Wi-Fi.\n")
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
                append("Credentials stored securely.")
                pop()
            },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        ),
        SlideContent(
            title = "Set Up Account",
            description = buildAnnotatedString {
                append("Enter VIT ID & password to start auto-login.")
            },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        ),
        SlideContent(
            title = "Widgets & Tile",
            description = buildAnnotatedString {
                append("Add Latch widget or Quick Settings tile for one-tap access.")
            },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Widgets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        ),
        SlideContent(
            title = "You're Ready!",
            description = buildAnnotatedString {
                append("Latch will auto-login whenever in range.")
            },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF1BA83C),
                    modifier = Modifier.size(36.dp)
                )
            }
        )
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                slideInHorizontally(
                    initialOffsetX = { width -> if (targetState > initialState) width else -width },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { width -> if (targetState > initialState) -width else width },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
            },
            label = "onboarding_slide"
        ) { targetStep ->
            val content = slides[targetStep]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top logo, less padding!
                Image(
                    painter = painterResource(id = R.drawable.vinnovate),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .padding(top = 40.dp)
                        .height(80.dp)
                )

                // Slide foreground
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 16.dp)
                ) {
                    content.icon()
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = content.title,
                        fontSize = 18.sp,
                        fontFamily = modernizFont,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = content.description,
                        fontSize = 20.sp,
                        lineHeight = 38.sp,
                        fontFamily = satoshiFont,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxWidth(0.95f)
                    )
                }

                // --- Bottom section with custom horizontal progress bar + buttons ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(32.dp)
                    ) {
                        Text(
                            "Skip",
                            fontFamily = modernizFont,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    LatchProgressBar(
                        steps = slides.size,
                        current = step,
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = Color.Gray.copy(alpha = 0.6f)
                    )

                    Button(
                        onClick = {
                            if (step < slides.size - 1) step++ else onNext()
                        },
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            if (step < slides.size - 1) "Next" else "Finish",
                            fontFamily = modernizFont,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

