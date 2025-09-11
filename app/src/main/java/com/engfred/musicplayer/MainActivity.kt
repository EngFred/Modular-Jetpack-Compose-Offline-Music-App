package com.engfred.musicplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.delay

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var getAppSettingsUseCase: GetAppSettingsUseCase
    @Inject lateinit var playbackController: PlaybackController

    // State used to hold an incoming external playback URI so Compose can react and start playback
    private var externalPlaybackUri by mutableStateOf<Uri?>(null)

    // Will hold the last intent data handled so we don't process same URI multiple times
    private var lastHandledUriString: String? = null

    // Permission launcher, initialized in onCreate
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: start")

        enableEdgeToEdge()

        // Register permission launcher before using it
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                Log.d(TAG, "Storage/read permission granted by the user.")
                // Try to play the pending URI if we have one
                externalPlaybackUri?.let { uri ->
                    lifecycleScope.launch {
                        initiatePlaybackFromExternalUri(uri)
                    }
                }
            } else {
                Toast.makeText(this, "Permission required to play external audio files.", Toast.LENGTH_SHORT).show()
            }
        }

        // Observe settings and playback state outside of Compose (keeps previous behavior)
        var appSettingsLoaded by mutableStateOf(false)
        var initialAppSettings: AppSettings? by mutableStateOf(null)
        var playbackState by mutableStateOf(PlaybackState())

        lifecycleScope.launch {
            getAppSettingsUseCase().collect { settings ->
                initialAppSettings = settings
                appSettingsLoaded = true
                playbackController.setRepeatMode(settings.repeatMode)
                playbackController.setShuffleMode(settings.shuffleMode)
                Log.d(TAG, "App settings loaded. repeat=${settings.repeatMode}, shuffle=${settings.shuffleMode}")
            }
        }

        lifecycleScope.launch {
            playbackController.getPlaybackState().collect {
                playbackState = it
            }
        }

        // Handle the initial intent (if app was opened via "Open with" chooser)
        handleIncomingIntent(intent)

        // Compose UI: keep your existing navigation and now-playing workflow.
        setContent {
            val selectedTheme = initialAppSettings?.selectedTheme ?: AppThemeType.DEEP_BLUE
            MusicPlayerAppTheme(selectedTheme = selectedTheme) {
                val windowSizeClass = calculateWindowSizeClass(this)
                val navController = androidx.navigation.compose.rememberNavController()
                AppNavHost(
                    rootNavController = navController,
                    isPlayerActive = playbackState.currentAudioFile != null,
                    windowWidthSizeClass = windowSizeClass.widthSizeClass,
                    windowHeightSizeClass = windowSizeClass.heightSizeClass,
                    onPlayPause = { lifecycleScope.launch { playbackController.playPause() } },
                    onPlayNext = { lifecycleScope.launch { playbackController.skipToNext() } },
                    onPlayPrev = { lifecycleScope.launch { playbackController.skipToPrevious() } },
                    isPlaying = playbackState.isPlaying,
                    playingAudioFile = playbackState.currentAudioFile,
                    context = this,
                    onNavigateToNowPlaying = {
                        if (playbackState.currentAudioFile != null) {
                            navController.navigate(AppDestinations.NowPlaying.route)
                        } else {
                            Toast.makeText(this, "Something is wrong!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                // If an external URI is set, initiate playback and navigate to NowPlaying
                androidx.compose.runtime.LaunchedEffect(externalPlaybackUri) {
                    val uri = externalPlaybackUri
                    if (uri != null) {
                        initiatePlaybackFromExternalUri(uri)
                        // Clear to avoid replaying the same URI repeatedly
                        externalPlaybackUri = null
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent called")
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    /**
     * Inspect the incoming intent for ACTION_VIEW and pull out the data URI.
     * If URI is present and not previously handled, set externalPlaybackUri (Compose will react) or
     * ask for permissions if necessary.
     */
    private fun handleIncomingIntent(intent: Intent?) {
        try {
            if (intent == null) return

            if (intent.action == Intent.ACTION_VIEW) {
                val uri = intent.data
                if (uri != null) {
                    val uriString = uri.toString()
                    // Avoid handling the same URI multiple times
                    if (uriString == lastHandledUriString) {
                        Log.d(TAG, "Intent URI already handled: $uriString")
                        return
                    }
                    lastHandledUriString = uriString
                    Log.d(TAG, "Incoming ACTION_VIEW with URI: $uriString")

                    // If the sending app granted transient read permission, take persistable permission when available.
                    if (intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0) {
                        try {
                            // FLAG_GRANT_PERSISTABLE_URI_PERMISSION may not always be present.
                            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            Log.d(TAG, "Persistable URI permission taken.")
                        } catch (e: SecurityException) {
                            // Not all URIs permit persistable permission — ignore safely.
                            Log.w(TAG, "Could not take persistable permission: ${e.message}")
                        } catch (e: Exception) {
                            Log.w(TAG, "takePersistableUriPermission failed: ${e.message}")
                        }
                    }

                    // Quick check: can we open the input stream right now? If so, no extra runtime permission needed.
                    val canOpenNow = tryOpenUriStream(uri)

                    if (canOpenNow) {
                        // Compose's LaunchedEffect will see this and start playback.
                        externalPlaybackUri = uri
                    } else {
                        // Check and request runtime permission (READ_MEDIA_AUDIO on API33+, READ_EXTERNAL_STORAGE otherwise).
                        if (needsRuntimeReadPermission()) {
                            val requiredPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.READ_MEDIA_AUDIO
                            } else {
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            }
                            // If permission already granted, set URI. Otherwise, request permission and then playback in callback.
                            if (ContextCompat.checkSelfPermission(this, requiredPerm) == PackageManager.PERMISSION_GRANTED) {
                                externalPlaybackUri = uri
                            } else {
                                // Save URI to try playback immediately after permission result
                                externalPlaybackUri = uri
                                permissionLauncher.launch(requiredPerm)
                            }
                        } else {
                            // No runtime permission required (older devices with content/file URI accessible), try to play anyway.
                            externalPlaybackUri = uri
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming intent: ${e.message}", e)
        }
    }

    /**
     * Small helper that tries to open and close an InputStream for the URI to verify read access.
     * Returns true if the stream can be opened.
     */
    private fun tryOpenUriStream(uri: Uri): Boolean {
        return try {
            contentResolver.openInputStream(uri)?.use { /* just open and close */ }
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "No permission to open URI: ${e.message}")
            false
        } catch (e: Exception) {
            Log.w(TAG, "Could not open URI stream: ${e.message}")
            false
        }
    }

    /**
     * Whether we should request runtime permission on this device.
     */
    private fun needsRuntimeReadPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires READ_MEDIA_* for media (READ_MEDIA_AUDIO)
            true
        } else {
            // On older devices, READ_EXTERNAL_STORAGE is required for many file URIs
            true
        }
    }

    /**
     * Attempt to initiate playback for the external uri using the PlaybackController.
     * This runs on the lifecycleScope and navigates to NowPlaying if playback state updates.
     */
    private suspend fun initiatePlaybackFromExternalUri(uri: Uri) {
        try {
            Log.d(TAG, "Attempt to initiate playback for external URI: $uri")
            // Delegate to PlaybackController which already performs accessibility checks and queue setup.
            playbackController.initiatePlayback(uri)

            // Wait briefly for the playback state to update and navigation to work smoothly (tunable).
            var attempts = 0
            while (attempts < 20) { // wait up to ~4 seconds (20 * 200ms)
                val state = playbackController.getPlaybackState() // Flow; we cannot block here; instead check last known state is not accessible directly
                // Instead of attempting to collect here, let UI's playback state flows update and the AppNavHost decide when to enable NowPlaying navigation.
                // We'll break early to avoid blocking.
                break
            }

            // Inform the user
            Toast.makeText(this, "Opening file in Music..", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start playback for external URI: ${e.message}", e)
            Toast.makeText(this, "Failed to play selected file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
