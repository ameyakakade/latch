package com.vinnovateit.autonetconnector

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Define Inter font family
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Bold),
)
val satoshiFont = FontFamily(
    Font(R.font.satoshi_bold, FontWeight.Bold),
)

class LandingPageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val db = com.vinnovateit.autonetconnector.functionality2.storage.CredentialDatabase.getInstance(this@LandingPageActivity)
            val existing = db.credentialDao().getCredential()
            if (existing != null) {
                // Credentials exist, go straight to MainActivity
                val intent = Intent(this@LandingPageActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                setContent {
                    LandingPageScreen(onGetStarted = {
                        val intent = Intent(this@LandingPageActivity, SecondPageActivity::class.java)
                        startActivity(intent)
                    })
                }
            }
        }
    }
}

@Composable
fun LandingPageScreen(onGetStarted: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1956))
    ) {
        // Top right VinnovateIT logo
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.vinnovate),
            contentDescription = "VinnovateIT Logo",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 20.dp)
                .size(120.dp)
        )
        // Centered column
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.latchlogo),
                contentDescription = "Latch Logo",
                modifier = Modifier
                    .size(120.dp)
            )
            // Latch logo
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.latch),
                contentDescription = "Latch Logo",
                modifier = Modifier
                    .size(120.dp)
            )
            
            Text(
                text = "Welcome back",
                color = Color.White,
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                modifier = Modifier.padding(bottom = 14.dp)
            )
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                shape = RoundedCornerShape(5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF0B1956)
                )
            ) {
                Text(
                    text = "Get Started",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = satoshiFont
                )
            }
        }
    }
} 