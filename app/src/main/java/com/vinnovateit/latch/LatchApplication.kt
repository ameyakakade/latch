package com.vinnovateit.latch

import android.app.Application
import com.vinnovateit.latch.domain.model.SessionRepository

class LatchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionRepository.initialize(this)
    }
}
