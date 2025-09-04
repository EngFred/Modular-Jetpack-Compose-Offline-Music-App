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

        var appSettingsLoaded by mutableStateOf(false)
        var initialAppSettings: AppSettings? by mutableStateOf(null)

        val splashScreen = installSplashScreen()

        enableEdgeToEdge()

        splashScreen.setKeepOnScreenCondition {
            !appSettingsLoaded
        }

        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            getAppSettingsUseCase().collect { settings ->
                initialAppSettings = settings
                appSettingsLoaded = true
            }
        }

        var playbackState by mutableStateOf(PlaybackState())

        lifecycleScope.launch {
            playbackController.getPlaybackState().collect{
                playbackState = it
            }
        }

        setContent {
            val selectedTheme = initialAppSettings?.selectedTheme ?: AppThemeType.DEEP_BLUE

            // --- GLOBAL PLAYBACK ERROR TOAST LOGIC ---
            LaunchedEffect(playbackState.error) {
                playbackState.error?.let { errorMessage ->
                    Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_LONG).show()
                    // Clear the error in PlayerController after showing the toast
                    playbackController.clearPlaybackError()
                }
            }
            // --- END GLOBAL PLAYBACK ERROR TOAST LOGIC ---

            MusicPlayerAppTheme(
                selectedTheme = selectedTheme
            ) {
                val windowSizeClass = calculateWindowSizeClass(this)
                Surface(
                    modifier = Modifier.fillMaxSize().background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    )
                ) {
                    val navController = rememberNavController()
                    AppNavHost(
                        rootNavController = navController,
                        windowWidthSizeClass =  windowSizeClass.widthSizeClass,
                        windowHeightSizeClass = windowSizeClass.heightSizeClass,
                        onPlayPause = {
                            lifecycleScope.launch {
                                playbackController.playPause()
                            }
                        },
                        onPlayNext = {
                            lifecycleScope.launch {
                                playbackController.skipToNext()
                            }
                        },
                        onPlayPrev = {
                            lifecycleScope.launch {
                                playbackController.skipToPrevious()
                            }
                        },
                        isPlaying = playbackState.isPlaying,
                        playingAudioFile = playbackState.currentAudioFile
                    )
                }
            }
        }
    }
}