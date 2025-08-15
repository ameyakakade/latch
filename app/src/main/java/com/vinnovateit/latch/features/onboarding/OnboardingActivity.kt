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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
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

@Composable
fun OnboardingScreen(
    onSkip: () -> Unit = {},
    onNext: () -> Unit = {}
) {
    var step by remember { mutableStateOf(0) }

    val satoshiFont = FontFamily(Font(R.font.satoshi_regular))
    val modernizFont = FontFamily(Font(R.font.moderniz))

    val slides: List<Any> = listOf(
        buildAnnotatedString {
            append("Welcome to Latch!\t")
            appendInlineContent("logo_inline", "[logo]")
            append("\n\nYour gateway to smarter, faster WiFi at VIT.")
        },
        "Step 1: Sign in with your VIT WiFi credentials:\n\n• Username: Your Registration Number\n\n• Password: Your WiFi Password",
        "Step 2: Allow all requested permissions.\n\nStep 3: Manually select your WiFi network\n\n(don't tap \"Sign In\" yet!)",
        "Step 4: Switch off mobile data.\n\nStep 5: Hit \"Connect\" in Latch.\n\nYou're now ready to browse!\n\n\n(Pro tip: If connection fails, repeat these steps.)"
    )

    val inlineContent = mapOf(
        "logo_inline" to InlineTextContent(
            Placeholder(
                width = 20.sp,
                height = 20.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_latch_light),
                contentDescription = "Inline Logo"
            )
        }
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.vinnovate),
                contentDescription = "App Logo",
                modifier = Modifier
                    .padding(top = 48.dp)
                    .height(80.dp)
            )

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { width ->
                            if (targetState > initialState) width else -width
                        },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300)) togetherWith
                            slideOutHorizontally(
                                targetOffsetX = { width ->
                                    if (targetState > initialState) -width else width
                                },
                                animationSpec = tween(300)
                            ) + fadeOut(animationSpec = tween(300))
                },
                label = "onboarding_slide"
            ) { targetStep ->
                when (val content = slides[targetStep]) {
                    is AnnotatedString -> {
                        Text(
                            text = content,
                            inlineContent = inlineContent,
                            fontSize = 18.sp,
                            lineHeight = 30.sp,
                            fontFamily = satoshiFont,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .padding(top = 32.dp, bottom = 32.dp)
                        )
                    }
                    is String -> {
                        Text(
                            text = content,
                            fontSize = 18.sp,
                            lineHeight = 30.sp,
                            fontFamily = satoshiFont,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .padding(top = 32.dp, bottom = 32.dp)
                        )
                    }
                }
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
                Button(
                    onClick = {
                        if (step < slides.size - 1) {
                            step++
                        } else {
                            onNext()
                        }
                    }
                ) {
                    Text(
                        if (step < slides.size - 1) "Next" else "Finish",
                        fontFamily = modernizFont
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingPreview() {
    LatchTheme {
        OnboardingScreen()
    }
}
