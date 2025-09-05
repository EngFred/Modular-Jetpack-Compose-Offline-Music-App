package com.engfred.musicplayer

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.engfred.musicplayer.core.domain.repository.PlaybackState
import com.engfred.musicplayer.core.domain.repository.PlaybackController
import com.engfred.musicplayer.core.ui.theme.AppThemeType
import com.engfred.musicplayer.core.ui.theme.MusicPlayerAppTheme
import com.engfred.musicplayer.core.domain.model.AppSettings
import com.engfred.musicplayer.feature_settings.domain.usecases.GetAppSettingsUseCase
import com.engfred.musicplayer.navigation.AppNavHost
import com.engfred.musicplayer.ui.about.screen.CustomSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var getAppSettingsUseCase: GetAppSettingsUseCase

    @Inject
    lateinit var playbackController: PlaybackController

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        var appSettingsLoaded by mutableStateOf(false)
        var initialAppSettings: AppSettings? by mutableStateOf(null)
        var playbackState by mutableStateOf(PlaybackState())

        // Control custom splash duration
        var showCustomSplash by mutableStateOf(true)

        lifecycleScope.launch {
            getAppSettingsUseCase().collect { settings ->
                initialAppSettings = settings
                appSettingsLoaded = true
            }
        }

        lifecycleScope.launch {
            playbackController.getPlaybackState().collect {
                playbackState = it
            }
        }

        setContent {
            val selectedTheme = initialAppSettings?.selectedTheme ?: AppThemeType.DEEP_BLUE

            MusicPlayerAppTheme(selectedTheme = selectedTheme) {
                val windowSizeClass = calculateWindowSizeClass(this)

                // Delay 3 seconds for splash
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(3000)
                    showCustomSplash = false
                }

                if (showCustomSplash) {
                    CustomSplashScreen()
                } else {
                    val navController = rememberNavController()
                    AppNavHost(
                        rootNavController = navController,
                        windowWidthSizeClass = windowSizeClass.widthSizeClass,
                        windowHeightSizeClass = windowSizeClass.heightSizeClass,
                        onPlayPause = {
                            lifecycleScope.launch { playbackController.playPause() }
                        },
                        onPlayNext = {
                            lifecycleScope.launch { playbackController.skipToNext() }
                        },
                        onPlayPrev = {
                            lifecycleScope.launch { playbackController.skipToPrevious() }
                        },
                        isPlaying = playbackState.isPlaying,
                        playingAudioFile = playbackState.currentAudioFile
                    )
                }
            }
        }
    }
}
