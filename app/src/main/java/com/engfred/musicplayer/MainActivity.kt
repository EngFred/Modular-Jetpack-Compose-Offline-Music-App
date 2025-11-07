package com.engfred.musicplayer

import android.Manifest
import android.content.Intent
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.engfred.musicplayer.core.common.Resource
import com.engfred.musicplayer.core.data.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AppSettings
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.repository.LibraryRepository
import com.engfred.musicplayer.core.domain.repository.PlaybackController
import com.engfred.musicplayer.core.domain.repository.PlaybackState
import com.engfred.musicplayer.core.domain.repository.SettingsRepository
import com.engfred.musicplayer.core.domain.usecases.PermissionHandlerUseCase
import com.engfred.musicplayer.core.ui.theme.AppThemeType
import com.engfred.musicplayer.core.ui.theme.MusicPlayerAppTheme
import com.engfred.musicplayer.core.util.MediaUtils
import com.engfred.musicplayer.feature_settings.domain.usecases.GetAppSettingsUseCase
import com.engfred.musicplayer.helpers.IntentPermissionHelper
import com.engfred.musicplayer.helpers.PlaybackQueueHelper
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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var getAppSettingsUseCase: GetAppSettingsUseCase
    @Inject lateinit var playbackController: PlaybackController
    @Inject lateinit var libraryRepository: LibraryRepository
    @Inject lateinit var sharedAudioDataSource: SharedAudioDataSource
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var permissionHandlerUseCase: PermissionHandlerUseCase

    private var externalPlaybackUri by mutableStateOf<Uri?>(null)
    private var pendingPlaybackUri: Uri? = null
    private var lastHandledUriString: String? = null
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    private var playbackState by mutableStateOf(PlaybackState())
    private var initialAppSettings: AppSettings? by mutableStateOf(null)
    private var appSettingsLoaded by mutableStateOf(false)

    private var lastPlaybackAudio: AudioFile? by mutableStateOf(null)

    private val uiScope get() = lifecycleScope

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: start")

        enableEdgeToEdge()

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

        uiScope.launch {
            try {
                playbackController.getPlaybackState().collect { state ->
                    playbackState = state
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to collect playback state: ${t.message}")
            }
        }

        uiScope.launch {
            try {
                val start = withContext(Dispatchers.IO) {
                    PlaybackQueueHelper.preparePlayingQueue(
                        context = this@MainActivity,
                        settingsRepository = settingsRepository,
                        libRepo = libraryRepository,
                        sharedAudioDataSource = sharedAudioDataSource
                    )
                }
                // Validating if the last playback audio still exists and is accessible using MediaUtils
                lastPlaybackAudio = if (start != null) {
                    val isAccessible = MediaUtils.isAudioFileAccessible(
                        context = this@MainActivity,
                        audioFileUri = start.uri,
                        permissionHandlerUseCase = permissionHandlerUseCase
                    )
                    if (isAccessible) {
                        Log.d(TAG, "Last playback audio validated as accessible: ${start.title}")
                        start
                    } else {
                        Log.w(TAG, "Last playback audio no longer accessible")
                        null
                    }
                } else null
                Log.d(TAG, "preparePlayingQueue returned startAudio=${lastPlaybackAudio?.id}")
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to prepare playing queue: ${t.message}")
            }
        }

        handleIncomingIntent(intent)

        setContent {
            val audioItems by sharedAudioDataSource.deviceAudioFiles.collectAsState(initial = emptyList())
            val selectedTheme = initialAppSettings?.selectedTheme ?: AppThemeType.CLASSIC_BLUE

            MusicPlayerAppTheme(selectedTheme = selectedTheme) {
                val navController = androidx.navigation.compose.rememberNavController()

                AppNavHost(
                    rootNavController = navController,
                    onPlayPause = {
                        uiScope.launch {
                            if (playbackState.currentAudioFile != null) {
                                playbackController.playPause()
                            } else {
                                val lastState = settingsRepository.getLastPlaybackState().first()
                                val startUri = lastPlaybackAudio?.uri
                                if (startUri != null) {
                                    playbackController.initiatePlayback(startUri, lastState.positionMs)
                                }
                            }
                        }
                    },
                    onPlayNext = {
                        uiScope.launch {
                            if (playbackState.currentAudioFile != null) {
                                playbackController.skipToNext()
                            } else {
                                val lastState = settingsRepository.getLastPlaybackState().first()
                                val startUri = lastPlaybackAudio?.uri
                                if (startUri != null) {
                                    playbackController.initiatePlayback(startUri, lastState.positionMs)
                                    if (playbackController.waitUntilReady(5000)) {
                                        playbackController.skipToNext()
                                    } else {
                                        Toast.makeText(this@MainActivity, "Failed to start playback", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    },
                    onPlayPrev = {
                        uiScope.launch {
                            if (playbackState.currentAudioFile != null) {
                                playbackController.skipToPrevious()
                            } else {
                                val lastState = settingsRepository.getLastPlaybackState().first()
                                val startUri = lastPlaybackAudio?.uri
                                if (startUri != null) {
                                    playbackController.initiatePlayback(startUri, lastState.positionMs)
                                    if (playbackController.waitUntilReady(5000)) {
                                        playbackController.skipToPrevious()
                                    } else {
                                        Toast.makeText(this@MainActivity, "Failed to start playback", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    },
                    playingAudioFile = playbackState.currentAudioFile,
                    isPlaying = playbackState.isPlaying,
                    context = this,
                    onNavigateToNowPlaying = {
                        uiScope.launch {
                            if (playbackState.currentAudioFile == null) {
                                val lastState = settingsRepository.getLastPlaybackState().first()
                                val startUri = lastPlaybackAudio?.uri
                                if (startUri != null) {
                                    playbackController.initiatePlayback(startUri, lastState.positionMs)
                                    if (!playbackController.waitUntilReady(5000)) {
                                        Toast.makeText(this@MainActivity, "Failed to start playback", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }
                                } else {
                                    Toast.makeText(this@MainActivity, "No previous playback", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                            }
                            navController.navigate(AppDestinations.NowPlaying.route)
                        }
                    },
//                    isPlayerActive = playbackState.currentAudioFile != null,
//                    isPlayingExternalUri = externalPlaybackUri != null,
                    onPlayAll = { PlaybackQueueHelper.playAll(this, sharedAudioDataSource, playbackController, settingsRepository) },
                    onShuffleAll = { PlaybackQueueHelper.shuffleAll(this, sharedAudioDataSource, playbackController, settingsRepository) },
                    audioItems = audioItems,
                    onReleasePlayer = {
                        uiScope.launch { playbackController.releasePlayer() }
                    },
                    lastPlaybackAudio = lastPlaybackAudio
                )

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

    private fun handleIncomingIntent(intent: Intent?) {
        try {
            IntentPermissionHelper.handleIncomingIntent(
                this,
                intent,
                ::getRequiredReadPermission,
                { uri -> this.externalPlaybackUri = uri },
                { pending -> this.pendingPlaybackUri = pending },
                permissionLauncher,
                ::tryOpenUriStream,
                { s -> this.lastHandledUriString = s },
                { s -> this.lastHandledUriString == s }
            )
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