package com.engfred.musicplayer

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.engfred.musicplayer.core.ui.theme.AppThemeType
import com.engfred.musicplayer.core.ui.theme.MusicPlayerAppTheme
import com.engfred.musicplayer.feature_settings.domain.model.AppSettings
import com.engfred.musicplayer.feature_settings.domain.usecases.GetAppSettingsUseCase
import com.engfred.musicplayer.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var getAppSettingsUseCase: GetAppSettingsUseCase

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Create a mutable state to track if settings are loaded
        var appSettingsLoaded by mutableStateOf(false)
        var initialAppSettings: AppSettings? by mutableStateOf(null)

        val splashScreen = installSplashScreen()

        splashScreen.setKeepOnScreenCondition {
            // Keep the splash screen on screen as long as appSettingsLoaded is false
            !appSettingsLoaded
        }

        super.onCreate(savedInstanceState)

        // Use a LifecycleScope to collect the flow and update the state
        lifecycleScope.launch {
            getAppSettingsUseCase().collect { settings ->
                initialAppSettings = settings
                appSettingsLoaded = true // Settings are loaded, dismiss splash screen
            }
        }

        setContent {
            // Use the initialAppSettings once it's available
            val selectedTheme = initialAppSettings?.selectedTheme ?: AppThemeType.FROSTBYTE

            MusicPlayerAppTheme(
                selectedTheme = selectedTheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavHost(rootNavController = navController)
                }
            }
        }
    }
}