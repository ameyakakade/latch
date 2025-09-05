package com.vinnovateit.latch.features.onboarding.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.vinnovateit.latch.common.ui.HandsConnectAnimation
import com.vinnovateit.latch.ui.theme.ModernizFontFamily
import com.vinnovateit.latch.ui.theme.SatoshiFontFamily

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
                .statusBarsPadding()
                .padding(start = 25.dp, end = 25.dp)
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


@Composable
fun WelcomeToLatchPageLandscape() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Welcome to Latch",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 32.sp,
                        lineHeight = 1.6.em,
                        fontFamily = ModernizFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Let's get everything set up for you.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontFamily = SatoshiFontFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            Spacer(modifier = Modifier.width(32.dp))
            Box(
                modifier = Modifier.weight(0.8f)
            ) {
                HandsConnectAnimation()
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewFirstPage() {
    WelcomeToLatchPage()
}