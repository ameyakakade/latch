package com.vinnovateit.latch.features.about

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.vinnovateit.latch.ui.theme.LatchTheme

class MeetTheTeamActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            LatchTheme {
                MeetTheTeamPage(onBackClick = { finish() })
            }
        }
    }
}