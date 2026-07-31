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
import com.vinnovateit.latch.desktop.resources.outfit_variable
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
 * Used only by the Meet-the-Team screen, which built this inline twice in the
 * Android app. Hoisted so the font is resolved once.
 */
@Composable
internal fun outfitFontFamily(): FontFamily {
    val outfit = Font(Res.font.outfit_variable, FontWeight.Normal)
    return remember(outfit) { FontFamily(outfit) }
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
                fontSize = 57.sp,
                lineHeight = 64.sp,
                letterSpacing = (-0.25).sp,
            ),
            displayMedium = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Normal,
                fontSize = 45.sp,
                lineHeight = 52.sp,
            ),
            displaySmall = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Normal,
                fontSize = 36.sp,
                lineHeight = 44.sp,
            ),
            headlineLarge = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Normal,
                fontSize = 32.sp,
                lineHeight = 40.sp,
            ),
            headlineMedium = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Medium,
                fontSize = 28.sp,
                lineHeight = 36.sp,
            ),
            headlineSmall = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Normal,
                fontSize = 24.sp,
                lineHeight = 32.sp,
            ),
            titleLarge = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Normal,
                fontSize = 22.sp,
                lineHeight = 28.sp,
            ),
            titleMedium = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp,
            ),
            titleSmall = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
            bodyLarge = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp,
            ),
            bodyMedium = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp,
            ),
            bodySmall = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.3.sp,
            ),
            labelLarge = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
            labelMedium = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
            labelSmall = TextStyle(
                fontFamily = satoshi,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
        )
    }
}
