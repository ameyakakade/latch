package com.vinnovateit.latch.features.auth

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.vinnovateit.latch.R
import com.vinnovateit.latch.features.home.MainActivity
import com.vinnovateit.latch.data.StoredCredentials
import com.vinnovateit.latch.features.onboarding.OnboardingActivity
import com.vinnovateit.latch.features.settings.manager.SettingsManager
import com.vinnovateit.latch.ui.theme.LatchTheme
import com.vinnovateit.latch.ui.theme.ModernizFontFamily
import com.vinnovateit.latch.ui.theme.SatoshiFontFamily

class LandingPageActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        askNotificationPermission()
        // Initialize SettingsManager to apply the correct theme immediately
        SettingsManager.initialize(this)

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hasSeenOnboarding = prefs.getBoolean("hasSeenOnboarding", false)

        if (!hasSeenOnboarding) {
            // Show onboarding
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        lifecycleScope.launch {
            if (StoredCredentials.credentialsExist(this@LandingPageActivity)) {
                // Credentials exist, go straight to MainActivity
                val intent = Intent(this@LandingPageActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                setContent {
                    LatchTheme {
                        LandingPageScreen(onGetStarted = {
                            val intent = Intent(this@LandingPageActivity, SecondPageActivity::class.java)
                            startActivity(intent)
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun LandingPageScreen(onGetStarted: () -> Unit) {
    val logoRes = if (isSystemInDarkTheme()) R.drawable.ic_latch_dark else R.drawable.ic_latch_light



    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Centered column
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(id = logoRes),
                contentDescription = stringResource(R.string.landing_latch_logo_content_description),
                modifier = Modifier
                    .size(120.dp)
            )
            Text(
                text = stringResource(R.string.app_name_uppercase),
                fontSize = 30.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = ModernizFontFamily
            )
            Spacer(Modifier.height(48.dp))
            Button(
                onClick = onGetStarted,
                shape = RoundedCornerShape(24),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    text = stringResource(R.string.get_started),
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SatoshiFontFamily
                )
            }
        }

        // Bottom center VinnovateIT logo
        Image(
            painter = painterResource(id = R.drawable.vinnovate),
            contentDescription = stringResource(R.string.landing_vinnovateit_logo_content_description),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .size(150.dp)
        )
    }
}