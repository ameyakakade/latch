package com.vinnovateit.latch.features.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class SlideContent(
    val title: String,
    val description: AnnotatedString,
    val icon: @Composable () -> Unit,
    val icons: ImmutableList<Int> = persistentListOf()
)