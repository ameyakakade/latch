package com.vinnovateit.latch.features.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.vinnovateit.latch.features.home.MainActivity
import com.vinnovateit.latch.data.StoredCredentials
import com.vinnovateit.latch.features.settings.manager.SettingsManager

class LandingPageActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        SettingsManager.initialize(this)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val hasSeenOnboarding = prefs.getBoolean("hasSeenOnboarding", false)

        if (!hasSeenOnboarding) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        lifecycleScope.launch {
            if (StoredCredentials.credentialsExist(this@LandingPageActivity)) {
                startActivity(Intent(this@LandingPageActivity, MainActivity::class.java))
            } else {
                startActivity(Intent(this@LandingPageActivity, SecondPageActivity::class.java))
            }
            finish()
        }
    }
}