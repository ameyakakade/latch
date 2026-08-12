package com.vinnovateit.latch.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vinnovateit.latch.desktop.resources.Res
import com.vinnovateit.latch.desktop.resources.moderniz
import com.vinnovateit.latch.desktop.resources.satoshi_bold
import com.vinnovateit.latch.desktop.resources.satoshi_light
import com.vinnovateit.latch.desktop.resources.satoshi_medium
import com.vinnovateit.latch.desktop.resources.satoshi_regular
import org.jetbrains.compose.resources.Font

/*
 * Compose Multiplatform's Font() is @Composable, unlike Android's. That forces
 * the font families and the Typography -- top-level `val`s in the Android app --
 * to become composable functions. This is the single most invasive mechanical
 * change in the theme port.
 *
 * Every size, lineHeight and letterSpacing below is byte-identical to the
 * Android app. Note the deliberate deviations from M3 defaults, which must be
 * preserved for pixel fidelity:
 *   bodyLarge  18.sp (M3 default is 16)
 *   bodySmall  13.sp / 16.sp lineHeight / 0.3.sp letterSpacing (M3: 12/16/0.4)
 *   displayLarge letterSpacing -0.25.sp
 */

@Composable
internal fun satoshiFontFamily(): FontFamily {
    val light = Font(Res.font.satoshi_light, FontWeight.Light, FontStyle.Normal)
    val regular = Font(Res.font.satoshi_regular, FontWeight.Normal, FontStyle.Normal)
    val medium = Font(Res.font.satoshi_medium, FontWeight.Medium, FontStyle.Normal)
    val bold = Font(Res.font.satoshi_bold, FontWeight.Bold, FontStyle.Normal)
    return remember(light, regular, medium, bold) {
        FontFamily(light, regular, medium, bold)
    }
}

@Composable
internal fun modernizFontFamily(): FontFamily {
    val moderniz = Font(Res.font.moderniz, FontWeight.Normal)
    return remember(moderniz) { FontFamily(moderniz) }
}

/**
 * The app typography. [remember] is load-bearing: without it all 15 TextStyles
 * are rebuilt on every recomposition of LatchTheme.
 */
@Composable
internal fun appTypography(): Typography {
    val satoshi = satoshiFontFamily()
    return remember(satoshi) {
        Typography(
            displayLarge = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.W400,
                fontSize = 54.sp,
                lineHeight = 60.sp,
                letterSpacing = (-0.25).sp,
            ),
            displayMedium = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Normal,
                fontSize = 43.sp,
                lineHeight = 49.sp,
            ),
            displaySmall = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Normal,
                fontSize = 34.sp,
                lineHeight = 42.sp,
            ),
            headlineLarge = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Normal,
                fontSize = 30.sp,
                lineHeight = 38.sp,
            ),
            headlineMedium = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Medium,
                fontSize = 26.6.sp,
                lineHeight = 34.sp,
            ),
            headlineSmall = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Normal,
                fontSize = 22.8.sp,
                lineHeight = 30.sp,
            ),
            titleLarge = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Normal,
                fontSize = 21.sp,
                lineHeight = 26.sp,
            ),
            titleMedium = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Medium,
                fontSize = 15.2.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.15.sp,
            ),
            titleSmall = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Medium,
                fontSize = 13.3.sp,
                lineHeight = 19.sp,
                letterSpacing = 0.1.sp,
            ),
            bodyLarge = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Normal,
                fontSize = 17.1.sp,
                lineHeight = 23.sp,
                letterSpacing = 0.5.sp,
            ),
            bodyMedium = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Normal,
                fontSize = 13.3.sp,
                lineHeight = 19.sp,
                letterSpacing = 0.25.sp,
            ),
            bodySmall = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Normal,
                fontSize = 12.35.sp,
                lineHeight = 15.sp,
                letterSpacing = 0.3.sp,
            ),
            labelLarge = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Medium,
                fontSize = 13.3.sp,
                lineHeight = 19.sp,
                letterSpacing = 0.1.sp,
            ),
            labelMedium = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Medium,
                fontSize = 11.4.sp,
                lineHeight = 15.sp,
                letterSpacing = 0.5.sp,
            ),
            labelSmall = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Medium,
                fontSize = 10.45.sp,
                lineHeight = 15.sp,
                letterSpacing = 0.5.sp,
            ),
        )
    }
}
