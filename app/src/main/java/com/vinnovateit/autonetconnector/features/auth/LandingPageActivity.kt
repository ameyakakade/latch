package com.vinnovateit.autonetconnector.features.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.vinnovateit.autonetconnector.R
import com.vinnovateit.autonetconnector.features.home.MainActivity
import com.vinnovateit.autonetconnector.data.CredentialDatabase
import com.vinnovateit.autonetconnector.domain.model.SessionRepository
import com.vinnovateit.autonetconnector.features.settings.manager.SettingsManager
import com.vinnovateit.autonetconnector.ui.theme.AutoNetConnectorTheme
import com.vinnovateit.autonetconnector.ui.theme.SakingFontFamily
import com.vinnovateit.autonetconnector.ui.theme.SatoshiFontFamily

class LandingPageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SettingsManager to apply the correct theme immediately
        SettingsManager.initialize(this)

        lifecycleScope.launch {
            val db = CredentialDatabase.getInstance(this@LandingPageActivity)
            val existing = db.credentialDao().getCredential()
            if (existing != null) {
                // Credentials exist, go straight to MainActivity
                val intent = Intent(this@LandingPageActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                setContent {
                    AutoNetConnectorTheme {
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
    val logoRes = if (isSystemInDarkTheme()) R.drawable.ic_latch_light else R.drawable.ic_latch_dark



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
                contentDescription = "Latch Logo",
                modifier = Modifier
                    .size(120.dp)
            )
            // Latch logo text
            Text(
                text = stringResource(R.string.name_uppercase),
                fontSize = 32.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = SakingFontFamily
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
                    text = "Get Started",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SatoshiFontFamily
                )
            }
        }

        // Bottom center VinnovateIT logo
        Image(
            painter = painterResource(id = R.drawable.vinnovate),
            contentDescription = "VinnovateIT Logo",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .size(150.dp)
        )
    }
}