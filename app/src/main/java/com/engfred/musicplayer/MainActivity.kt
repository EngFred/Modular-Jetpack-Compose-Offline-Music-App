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
import com.engfred.musicplayer.core.data.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AppSettings
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.model.FilterOption
import com.engfred.musicplayer.core.domain.model.LastPlaybackState
import com.engfred.musicplayer.core.domain.repository.LibraryRepository
import com.engfred.musicplayer.core.domain.repository.PlaybackController
import com.engfred.musicplayer.core.domain.repository.PlaybackState
import com.engfred.musicplayer.core.domain.repository.SettingsRepository
import com.engfred.musicplayer.core.domain.repository.ShuffleMode
import com.engfred.musicplayer.core.ui.theme.AppThemeType
import com.engfred.musicplayer.core.ui.theme.MusicPlayerAppTheme
import com.engfred.musicplayer.feature_player.data.service.PlaybackService
import com.engfred.musicplayer.feature_settings.domain.usecases.GetAppSettingsUseCase
import com.engfred.musicplayer.navigation.AppDestinations
import com.engfred.musicplayer.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
                    Log.d(TAG, "App settings loaded. repeat=${settings.repeatMode}")
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

            val selectedTheme = initialAppSettings?.selectedTheme ?: AppThemeType.CLASSIC_BLUE

            MusicPlayerAppTheme(selectedTheme = selectedTheme) {
                val navController = androidx.navigation.compose.rememberNavController()

                AppNavHost(
                    rootNavController = navController,
                    onPlayPause = { uiScope.launch { playbackController.playPause() } },
                    onPlayNext = { uiScope.launch { playbackController.skipToNext() } },
                    onPlayPrev = { uiScope.launch { playbackController.skipToPrevious() } },
                    playingAudioFile = playbackState.currentAudioFile,
                    isPlaying = playbackState.isPlaying,
                    context = this,
                    onNavigateToNowPlaying = {
                        if (playbackState.currentAudioFile != null) {
                            navController.navigate(AppDestinations.NowPlaying.route)
                        } else {
                            Toast.makeText(this, "No active playback", Toast.LENGTH_SHORT).show()
                        }
                    },
                    isPlayerActive = playbackState.currentAudioFile != null,
                    isPlayingExternalUri = externalPlaybackUri != null,
                    onPlayAll = { playAll() },
                    onShuffleAll = { shuffleAll() },
                    audioItems = audioItems,
                    onReleasePlayer = {
                        uiScope.launch { playbackController.releasePlayer() }
                    }
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

    private fun playAll() {
        uiScope.launch {
            val audioFiles = sharedAudioDataSource.deviceAudioFiles.value
            if (audioFiles.isEmpty()) {
                Toast.makeText(this@MainActivity, "No audio files found", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val lastState = settingsRepository.getLastPlaybackState().first()
            val appSettings = settingsRepository.getAppSettings().first()
            val repeat = appSettings.repeatMode
            val filter = settingsRepository.getFilterOption().first()
            val sorted = sortAudioFiles(audioFiles, filter)
            val playingQueue = lastState.queueIds?.takeIf { it.isNotEmpty() }?.let { ids ->
                val idToAudio = audioFiles.associateBy { it.id }
                ids.mapNotNull { idToAudio[it] }.takeIf { it.isNotEmpty() } ?: sorted
            } ?: sorted
            sharedAudioDataSource.setPlayingQueue(playingQueue)
            playbackController.setRepeatMode(repeat)
            playbackController.setShuffleMode(ShuffleMode.OFF)
            val startAudio = lastState.audioId?.let { id ->
                playingQueue.find { it.id == id }
            }
            val startUri = startAudio?.uri ?: playingQueue.firstOrNull()?.uri
            if (startUri != null) {
                Log.d(TAG, "Starting playback with URI: $startUri")
                playbackController.initiatePlayback(startUri)
                if (startAudio != null && lastState.positionMs > 0) {
                    Log.d(TAG, "Seeking to last position: ${lastState.positionMs}")
                    playbackController.seekTo(lastState.positionMs)
                }
            } else {
                Toast.makeText(this@MainActivity, "No audio files found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shuffleAll() {
        uiScope.launch {
            val audioFiles = sharedAudioDataSource.deviceAudioFiles.value
            if (audioFiles.isEmpty()) {
                Toast.makeText(this@MainActivity, "No audio files found", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val lastState = settingsRepository.getLastPlaybackState().first()
            val appSettings = settingsRepository.getAppSettings().first()
            val repeat = appSettings.repeatMode
            val filter = settingsRepository.getFilterOption().first()
            val sorted = sortAudioFiles(audioFiles, filter)
            val playingQueue = lastState.queueIds?.takeIf { it.isNotEmpty() }?.let { ids ->
                val idToAudio = audioFiles.associateBy { it.id }
                ids.mapNotNull { idToAudio[it] }.takeIf { it.isNotEmpty() } ?: sorted
            } ?: sorted
            sharedAudioDataSource.setPlayingQueue(playingQueue)
            playbackController.setRepeatMode(repeat)
            val startAudio = lastState.audioId?.let { id ->
                playingQueue.find { it.id == id }
            }
            val startUri = startAudio?.uri ?: playingQueue.firstOrNull()?.uri
            if (startUri != null) {
                playbackController.initiateShufflePlayback(playingQueue)
                if (startAudio != null && lastState.positionMs > 0) {
                    playbackController.seekTo(lastState.positionMs)
                }
            } else {
                Toast.makeText(this@MainActivity, "No audio files found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sortAudioFiles(audioFiles: List<AudioFile>, filter: FilterOption): List<AudioFile> {
        return when (filter) {
            FilterOption.DATE_ADDED_ASC -> audioFiles.sortedBy { it.dateAdded }
            FilterOption.DATE_ADDED_DESC -> audioFiles.sortedByDescending { it.dateAdded }
            FilterOption.LENGTH_ASC -> audioFiles.sortedBy { it.duration }
            FilterOption.LENGTH_DESC -> audioFiles.sortedByDescending { it.duration }
            FilterOption.ALPHABETICAL_ASC -> audioFiles.sortedBy { it.title.lowercase() }
            FilterOption.ALPHABETICAL_DESC -> audioFiles.sortedByDescending { it.title.lowercase() }
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

    override fun onDestroy() {
        super.onDestroy()

        try {
            // Best-effort synchronous save to persist only the playing queue IDs before the activity is destroyed.
            runBlocking {
                try {
                    // Read current playing queue from shared data source (StateFlow)
                    val currentQueue: List<AudioFile> = try {
                        sharedAudioDataSource.playingQueueAudioFiles.value
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to read playingQueueAudioFiles.value, falling back to emptyList(): ${e.message}")
                        emptyList()
                    }

                    // Convert to IDs (persist only IDs — minimal)
                    val queueIds: List<Long> = currentQueue.mapNotNull { it.id }

                    // Save minimal LastPlaybackState that contains only the queue (no current audio, no position)
                    val stateToSave = if (queueIds.isNotEmpty()) {
                        LastPlaybackState(audioId = null, positionMs = 0L, queueIds = queueIds)
                    } else {
                        // explicit cleared state
                        LastPlaybackState(null)
                    }

                    settingsRepository.saveLastPlaybackState(stateToSave)
                    Log.d(TAG, "Saved playing queue on Activity.onDestroy: queue size=${queueIds.size}")
                } catch (e: Exception) {
                    Log.w(TAG, "Error saving playing queue in onDestroy: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "runBlocking save failure in onDestroy: ${e.message}", e)
        }
    }

}