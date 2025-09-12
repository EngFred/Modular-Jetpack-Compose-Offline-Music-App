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
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.engfred.musicplayer.core.common.Resource
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AppSettings
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.model.FilterOption
import com.engfred.musicplayer.core.domain.repository.LibraryRepository
import com.engfred.musicplayer.core.domain.repository.PlaybackController
import com.engfred.musicplayer.core.domain.repository.PlaybackState
import com.engfred.musicplayer.core.domain.repository.SettingsRepository
import com.engfred.musicplayer.core.ui.theme.AppThemeType
import com.engfred.musicplayer.core.ui.theme.MusicPlayerAppTheme
import com.engfred.musicplayer.feature_settings.domain.usecases.GetAppSettingsUseCase
import com.engfred.musicplayer.navigation.AppDestinations
import com.engfred.musicplayer.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "MainActivity"

/**
 * Main activity that hosts the Compose UI for the music player.
 * Improvements for production: safer DataStore handling, clearer permission flow,
 * defensive intent handling, and clearer coroutine scoping.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var getAppSettingsUseCase: GetAppSettingsUseCase
    @Inject lateinit var playbackController: PlaybackController
    @Inject lateinit var libraryRepository: LibraryRepository
    @Inject lateinit var sharedAudioDataSource: SharedAudioDataSource
    @Inject lateinit var settingsRepository: SettingsRepository

    // Activity-level mutable state (observable by Compose)
    private var externalPlaybackUri by mutableStateOf<Uri?>(null)
    private var pendingPlaybackUri: Uri? = null
    private var lastHandledUriString: String? = null
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    private var playbackState by mutableStateOf(PlaybackState())
    private var initialAppSettings: AppSettings? by mutableStateOf(null)
    private var appSettingsLoaded by mutableStateOf(false)

    private val uiScope get() = lifecycleScope

    companion object {
        private const val PERMISSION_READ_EXTERNAL = "PERMISSION_READ_EXTERNAL"
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the platform splash screen behaviour
        installSplashScreen()
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: start")

        enableEdgeToEdge()

        // Register permission callback early
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                Log.d(TAG, "Read permission granted by the user.")
                externalPlaybackUri = pendingPlaybackUri
                pendingPlaybackUri = null
            } else {
                Toast.makeText(
                    this,
                    "Permission required to play external audio files.",
                    Toast.LENGTH_SHORT
                ).show()
                pendingPlaybackUri = null
            }
        }

        // Observe initial settings and configure playback defaults
        uiScope.launch {
            try {
                getAppSettingsUseCase().collect { settings ->
                    initialAppSettings = settings
                    appSettingsLoaded = true
                    playbackController.setRepeatMode(settings.repeatMode)
                    playbackController.setShuffleMode(settings.shuffleMode)
                    Log.d(TAG, "App settings loaded. repeat=${settings.repeatMode}, shuffle=${settings.shuffleMode}")
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to observe app settings: ${t.message}")
            }
        }

        // Observe playback state updates and expose to Compose
        uiScope.launch {
            try {
                playbackController.getPlaybackState().collect { state ->
                    playbackState = state
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to collect playback state: ${t.message}")
            }
        }

        // Handle initial intent when launched via share/open-with
        handleIncomingIntent(intent)

        setContent {
            // Safely observe audio list as Compose state once
            val audioItems by sharedAudioDataSource.deviceAudioFiles.collectAsState(initial = emptyList())

            val selectedTheme = initialAppSettings?.selectedTheme ?: AppThemeType.BLUE

            MusicPlayerAppTheme(selectedTheme = selectedTheme) {
                val navController = androidx.navigation.compose.rememberNavController()

                AppNavHost(
                    rootNavController = navController,
                    isPlayerActive = playbackState.currentAudioFile != null,
                    windowWidthSizeClass = calculateWindowSizeClass(this).widthSizeClass,
                    windowHeightSizeClass = calculateWindowSizeClass(this).heightSizeClass,
                    onPlayPause = { uiScope.launch { playbackController.playPause() } },
                    onPlayNext = { uiScope.launch { playbackController.skipToNext() } },
                    onPlayPrev = { uiScope.launch { playbackController.skipToPrevious() } },
                    isPlaying = playbackState.isPlaying,
                    playingAudioFile = playbackState.currentAudioFile,
                    context = this,
                    isPlayingExternalUri = externalPlaybackUri != null,
                    onNavigateToNowPlaying = {
                        if (playbackState.currentAudioFile != null) {
                            navController.navigate(AppDestinations.NowPlaying.route)
                        } else {
                            Toast.makeText(this, "No active playback", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onPlayAll = { playAll() },
                    onShuffleAll = { shuffleAll() },
                    audioItems = audioItems
                )

                // React to external playback URIs: attempt playback and navigate when successful
                LaunchedEffect(externalPlaybackUri) {
                    val uri = externalPlaybackUri
                    if (uri != null) {
                        val success = withContext(Dispatchers.IO) { initiatePlaybackFromExternalUri(uri) }
                        if (success) navController.navigate(AppDestinations.NowPlaying.route)
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

    // --- Helpers ---

    private fun getRequiredReadPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private suspend fun preparePlayback(audioFiles: List<AudioFile>): List<AudioFile> {
        // Consolidate settings fetch to avoid repeated retrievals
        val appSettings = settingsRepository.getAppSettings().first()
        val filter = settingsRepository.getFilterOption().first()

        val sortedAudios = when (filter) {
            FilterOption.DATE_ADDED_ASC -> audioFiles.sortedBy { it.dateAdded }
            FilterOption.DATE_ADDED_DESC -> audioFiles.sortedByDescending { it.dateAdded }
            FilterOption.LENGTH_ASC -> audioFiles.sortedBy { it.duration }
            FilterOption.LENGTH_DESC -> audioFiles.sortedByDescending { it.duration }
            FilterOption.ALPHABETICAL_ASC -> audioFiles.sortedBy { it.title.lowercase() }
            FilterOption.ALPHABETICAL_DESC -> audioFiles.sortedByDescending { it.title.lowercase() }
        }

        // Ensure controller reflects settings
        playbackController.setRepeatMode(appSettings.repeatMode)
        playbackController.setShuffleMode(appSettings.shuffleMode)
        return sortedAudios
    }

    private fun playAll() {
        uiScope.launch {
            val audioFiles = sharedAudioDataSource.deviceAudioFiles.value
            if (audioFiles.isNotEmpty()) {
                val sorted = preparePlayback(audioFiles)
                sharedAudioDataSource.setPlayingQueue(sorted)
                playbackController.initiatePlayback(sorted.first().uri)
            } else {
                Toast.makeText(this@MainActivity, "No audio files found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shuffleAll() {
        uiScope.launch {
            val audioFiles = sharedAudioDataSource.deviceAudioFiles.value
            if (audioFiles.isNotEmpty()) {
                val sorted = preparePlayback(audioFiles)
                playbackController.initiateShufflePlayback(sorted)
            } else {
                Toast.makeText(this@MainActivity, "No audio files found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        try {
            if (intent == null) return

            if (intent.action == Intent.ACTION_VIEW) {
                val uri = intent.data ?: return
                val uriString = uri.toString()

                // Avoid processing the same URI repeatedly
                if (uriString == lastHandledUriString) {
                    Log.d(TAG, "Intent URI already handled: $uriString")
                    return
                }

                lastHandledUriString = uriString
                Log.d(TAG, "Incoming ACTION_VIEW with URI: $uriString")

                // Attempt to take persistable permission if the sending app allowed it
                if (intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0) {
                    try {
                        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        Log.d(TAG, "Persistable URI permission taken.")
                    } catch (e: SecurityException) {
                        Log.w(TAG, "Could not take persistable permission: ${e.message}")
                    } catch (e: Exception) {
                        Log.w(TAG, "takePersistableUriPermission failed: ${e.message}")
                    }
                }

                val canOpenNow = tryOpenUriStream(uri)
                if (canOpenNow) {
                    externalPlaybackUri = uri
                    return
                }

                // Request runtime permission if required
                val requiredPerm = getRequiredReadPermission()
                if (ContextCompat.checkSelfPermission(this, requiredPerm) == PackageManager.PERMISSION_GRANTED) {
                    externalPlaybackUri = uri
                } else {
                    pendingPlaybackUri = uri
                    permissionLauncher.launch(requiredPerm)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming intent: ${e.message}", e)
        }
    }

    private fun tryOpenUriStream(uri: Uri): Boolean {
        return try {
            contentResolver.openInputStream(uri)?.use { }
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "No permission to open URI: ${e.message}")
            false
        } catch (e: Exception) {
            Log.w(TAG, "Could not open URI stream: ${e.message}")
            false
        }
    }

    private fun needsRuntimeReadPermission(): Boolean {
        // Modern Android versions always require runtime permission for external storage/media
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    private suspend fun initiatePlaybackFromExternalUri(uri: Uri): Boolean {
        try {
            Log.d(TAG, "Attempt to initiate playback for external URI: $uri")

            // Wait for player to become ready with a reasonable timeout handled inside controller
            if (!playbackController.waitUntilReady()) {
                Log.e(TAG, "Player not ready in time for external playback.")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Player not ready. Please try again.", Toast.LENGTH_LONG).show()
                }
                return false
            }

            val audioFileFetchStatus = libraryRepository.getAudioFileByUri(uri)
            when (audioFileFetchStatus) {
                is Resource.Error -> {
                    Log.e(TAG, "Failed to fetch audio file for external URI: ${audioFileFetchStatus.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Failed to play selected file: ${audioFileFetchStatus.message}", Toast.LENGTH_LONG).show()
                    }
                    return false
                }
                is Resource.Loading -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Opening file in Music..", Toast.LENGTH_SHORT).show()
                    }
                    return false
                }
                is Resource.Success -> {
                    val audioFile = audioFileFetchStatus.data ?: run {
                        Log.e(TAG, "Audio File not found!")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Audio File not found!", Toast.LENGTH_LONG).show()
                        }
                        return false
                    }

                    sharedAudioDataSource.setPlayingQueue(listOf(audioFile))
                    playbackController.initiatePlayback(uri)

                    // Wait briefly for playback state update
                    val startTime = System.currentTimeMillis()
                    var success = false
                    while (System.currentTimeMillis() - startTime < 3_000 && !success) {
                        if (playbackState.currentAudioFile != null && (playbackState.isPlaying || playbackState.isLoading)) {
                            success = true
                        }
                        delay(200)
                    }

                    if (!success) {
                        Log.w(TAG, "Playback did not start successfully within timeout.")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Failed to start playback.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    return success
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start playback for external URI: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Failed to play selected file: ${e.message}", Toast.LENGTH_LONG).show()
            }
            return false
        }
    }
}
