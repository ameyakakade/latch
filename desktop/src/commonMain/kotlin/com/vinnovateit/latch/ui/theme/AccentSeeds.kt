package com.vinnovateit.latch.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The six accent seed colours the whole scheme is generated from.
 *
 * In the Android app this list is duplicated three times (Theme.kt and twice in
 * SettingsActivity.kt). Consolidated here so the picker swatches and the
 * generated scheme can never disagree.
 *
 * Names are the persisted setting values, so they must not change.
 */
internal object AccentSeeds {
    val Red = Color(0xFFC01221)
    val Blue = Color(0xFF005AC1)
    val Green = Color(0xFF0F5223)
    val Purple = Color(0xFF7D00B8)
    val Pink = Color(0xFFD81B60)
    val Yellow = Color(0xFFF5B300)

    /** Display name -> seed, in the order the settings picker shows them. */
    val ordered: List<Pair<String, Color>> = listOf(
        "Red" to Red,
        "Blue" to Blue,
        "Green" to Green,
        "Purple" to Purple,
        "Pink" to Pink,
        "Yellow" to Yellow,
    )

    /** Red is the default for any unrecognised value, matching Android. */
    fun forName(name: String): Color = when (name) {
        "Blue" -> Blue
        "Green" -> Green
        "Purple" -> Purple
        "Pink" -> Pink
        "Yellow" -> Yellow
        else -> Red
    }
}
