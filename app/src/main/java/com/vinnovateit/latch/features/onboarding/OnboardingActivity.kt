package com.vinnovateit.latch.features.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import com.vinnovateit.latch.features.home.MainActivity
import com.vinnovateit.latch.features.onboarding.components.OnboardingScreen
import com.vinnovateit.latch.ui.theme.LatchTheme

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

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