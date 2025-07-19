package com.engfred.musicplayer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MusicPlayerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Any global initialization goes here
    }
}
