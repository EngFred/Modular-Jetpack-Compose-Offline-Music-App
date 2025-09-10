package com.engfred.musicplayer

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.engfred.musicplayer.core.domain.model.AppSettings
import com.engfred.musicplayer.core.domain.repository.PlaybackController
import com.engfred.musicplayer.core.domain.repository.PlaybackState
import com.engfred.musicplayer.core.ui.theme.AppThemeType
import com.engfred.musicplayer.core.ui.theme.MusicPlayerAppTheme
import com.engfred.musicplayer.feature_settings.domain.usecases.GetAppSettingsUseCase
import com.engfred.musicplayer.navigation.AppDestinations
import com.engfred.musicplayer.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainActivity22"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var getAppSettingsUseCase: GetAppSettingsUseCase

    @Inject
    lateinit var playbackController: PlaybackController

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: start")

        enableEdgeToEdge()

        var appSettingsLoaded by mutableStateOf(false)
        var initialAppSettings: AppSettings? by mutableStateOf(null)
        var playbackState by mutableStateOf(PlaybackState())

        lifecycleScope.launch {
            Log.d(TAG, "collecting app settings...")
            getAppSettingsUseCase().collect { settings ->
                initialAppSettings = settings
                appSettingsLoaded = true
                Log.d(TAG, "App settings loaded. Repeat mode: ${settings.repeatMode}, Shuffle mode: ${settings.shuffleMode}")
                playbackController.setRepeatMode(settings.repeatMode)
                playbackController.setShuffleMode(settings.shuffleMode)
            }
        }

        lifecycleScope.launch {
            Log.d(TAG, "collecting playback state...")
            playbackController.getPlaybackState().collect {
                playbackState = it
//                Log.d(TAG, "Playback state updated. IsPlaying: ${it.isPlaying}, Current file: ${it.currentAudioFile?.title}")
            }
        }

        setContent {
            Log.d(TAG, "setContent: composing UI...")
            val selectedTheme = initialAppSettings?.selectedTheme ?: AppThemeType.DEEP_BLUE
            MusicPlayerAppTheme(selectedTheme = selectedTheme) {
                val windowSizeClass = calculateWindowSizeClass(this)
                val navController = rememberNavController()
                AppNavHost(
                    rootNavController = navController,
                    isPlayerActive = playbackState.currentAudioFile != null,
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
                    playingAudioFile = playbackState.currentAudioFile,
                    context = this,
                    onNavigateToNowPlaying = {
                        //only navigate to now playing if the currentAudio in the playback state is not null
                        if (playbackState.currentAudioFile != null) {
                            navController.navigate(AppDestinations.NowPlaying.route)
                        } else {
                            //show a toast "something went wrong!
                            Toast.makeText(this, "Something is wrong!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}