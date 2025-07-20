package com.engfred.musicplayer.feature_player.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.engfred.musicplayer.feature_library.domain.usecases.PermissionHandlerUseCase
import com.engfred.musicplayer.feature_player.domain.model.PlaybackState
import com.engfred.musicplayer.feature_player.domain.model.RepeatMode
import com.engfred.musicplayer.feature_player.domain.model.ShuffleMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext // Still needed if you explicitly move to another thread and then back

import javax.inject.Inject

// Notification Channel ID
const val MUSIC_NOTIFICATION_CHANNEL_ID = "music_playback_channel"
const val MUSIC_NOTIFICATION_ID = 101 // Unique ID for your foreground notification

@UnstableApi
@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject
    lateinit var exoPlayer: ExoPlayer

    @Inject
    lateinit var audioFileMapper: AudioFileMapper // Ensure this is a lightweight, thread-safe mapper

    @Inject
    lateinit var musicNotificationProvider: MusicNotificationProvider

    @Inject
    lateinit var permissionHandlerUseCase: PermissionHandlerUseCase

    private var mediaSession: MediaSession? = null

    // Using a single StateFlow for playback state, updated on the main thread
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState = _playbackState.asStateFlow()

    // Service scope for coroutines, allows cancellation when service is destroyed
    // SupervisorJob allows child coroutines to fail without cancelling the parent
    // Keep this on Dispatchers.Main as it's for UI-related tasks (like updating _playbackState)
    // and periodic ExoPlayer polling that MUST be on the Main thread.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        val startTime = System.currentTimeMillis()
        Log.d("MusicService", "onCreate started")

        // 1. Create Notification Channel (can be done on any thread, but Main is fine here)
        createNotificationChannel()

        // 2. Start foreground with a temporary notification (required by Android 10+ for background services)
        // This must be done quickly on the main thread after service creation.
        // It's crucial for the service to remain alive.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notification = NotificationCompat.Builder(this, MUSIC_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Music Player")
                .setContentText("Starting music service...")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setPriority(NotificationManager.IMPORTANCE_LOW) // Use LOW for background playback
                .setSilent(true) // No sound for initial notification
                .build()
            try {
                startForeground(MUSIC_NOTIFICATION_ID, notification)
                Log.d("MusicService", "Foreground started: ${System.currentTimeMillis() - startTime}ms")
            } catch (e: Exception) {
                Log.e("MusicService", "Failed to start foreground service: ${e.message}")
                // Consider stopping the service if foreground can't be started.
                stopSelf()
                return
            }
        }

        // 3. Permission Check: Crucial before any media-related operations
        if (!permissionHandlerUseCase.hasAudioPermission()) {
            Log.w("MusicService", "Audio permissions not granted, stopping service")
            stopSelf() // Stop service if permissions are not granted
            return
        }

        // 4. Initialize ExoPlayer and MediaSession on the Main Thread
        // All direct interactions with ExoPlayer (setting attributes, listeners, MediaSession creation)
        // MUST happen on the Main thread. Since onCreate is on the Main thread, and Hilt typically
        // injects ExoPlayer on the Main thread for Application scope, we don't need a `serviceScope.launch(Dispatchers.Main)`
        // around the *entire* setup block if we are confident about the injection context.
        // However, if any part of the setup involves potentially blocking calls or needs to be
        // deferred slightly, a dedicated launch on Main could still be beneficial.
        // For direct property setting and listener registration, `onCreate` (main thread) is fine.
        try {
            val initStart = System.currentTimeMillis()

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            // Configure ExoPlayer directly on the Main thread (onCreate is on Main)
            (exoPlayer as? ExoPlayer)?.let {
                it.setAudioAttributes(audioAttributes, true)
                it.setHandleAudioBecomingNoisy(true)
                // Consider setting a WakeLock here if persistent playback is critical
                // it.setWakeMode(C.WAKE_MODE_LOCAL);
            }
            Log.d("MusicService", "ExoPlayer configured: ${System.currentTimeMillis() - initStart}ms")

            // Create MediaSession on the Main thread
            mediaSession = MediaSession.Builder(this@MusicService, exoPlayer)
                .setCallback(MediaSessionCallback())
                .build()
            setMediaNotificationProvider(musicNotificationProvider) // This sets the provider for Media3
            Log.d("MusicService", "MediaSession created: ${System.currentTimeMillis() - initStart}ms")

            // Add ExoPlayer Listener to react to player state changes
            // Callbacks from ExoPlayer's Player.Listener are also on the Main thread by default
            // if the player was created on the Main thread.
            exoPlayer.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _playbackState.value = _playbackState.value.copy(
                        isLoading = playbackState == Player.STATE_BUFFERING
                        // Removed `isReady = playbackState == Player.STATE_READY` as it's not in your PlaybackState class
                    )
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    mediaItem?.let {
                        val currentAudioFile = audioFileMapper.mapMediaItemToAudioFile(it)
                        _playbackState.value = _playbackState.value.copy(
                            currentAudioFile = currentAudioFile,
                            playbackPositionMs = exoPlayer.currentPosition,
                            totalDurationMs = exoPlayer.duration,
                            isLoading = false, // Not buffering at start of transition
                        )
                    }
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    _playbackState.value = _playbackState.value.copy(
                        playbackPositionMs = exoPlayer.currentPosition
                    )
                }

                override fun onPlayerError(error: PlaybackException) {
                    _playbackState.value = _playbackState.value.copy(
                        error = error.message ?: "Unknown playback error",
                        isPlaying = false, // Player stopped due to error
                        isLoading = false
                    )
                    Log.e("MusicService", "Playback error: ${error.message}", error)
                    // FirebaseCrashlytics.getInstance().recordException(error)
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    _playbackState.value = _playbackState.value.copy(
                        repeatMode = when (repeatMode) {
                            Player.REPEAT_MODE_OFF -> RepeatMode.OFF
                            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                            else -> RepeatMode.OFF
                        }
                    )
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    _playbackState.value = _playbackState.value.copy(
                        shuffleMode = if (shuffleModeEnabled) ShuffleMode.ON else ShuffleMode.OFF
                    )
                }
            })
        } catch (e: Exception) {
            Log.e("MusicService", "Initialization failed: ${e.message}", e)
            // FirebaseCrashlytics.getInstance().recordException(e)
            // Update state on Main thread in case of error
            _playbackState.value = _playbackState.value.copy(
                error = "Service initialization failed: ${e.message}",
                isPlaying = false,
                isLoading = false
            )
            stopSelf() // Stop the service if initialization fails critically
        }


        // 5. Update playback position at a regular interval
        // This polling must also be on the main thread to access ExoPlayer properties.
        serviceScope.launch(Dispatchers.Main) {
            while (true) {
                if (exoPlayer.playbackState != Player.STATE_IDLE && exoPlayer.playbackState != Player.STATE_ENDED) {
                    val newState = _playbackState.value.copy(
                        playbackPositionMs = exoPlayer.currentPosition,
                        bufferedPositionMs = exoPlayer.bufferedPosition,
                        totalDurationMs = exoPlayer.duration,
                        // Ensure isPlaying is always reflective of the player's true state
                        isPlaying = exoPlayer.isPlaying
                    )
                    _playbackState.value = newState
                }
                // Update more frequently for smoother progress bar, e.g., every 500ms
                delay(500)
            }
        }

        Log.d("MusicService", "onCreate completed: ${System.currentTimeMillis() - startTime}ms")
    }

    // This method is called by the system when a controller wants to connect to your service.
    // It will be called on the main thread.
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle incoming intents, e.g., play a specific song
        // If an intent contains a command to play a specific song,
        // you would parse it here and call exoPlayer.setMediaItem() and exoPlayer.prepare()
        // Ensure such calls are also on the main thread.
        super.onStartCommand(intent, flags, startId)
        return START_STICKY // Service will be restarted if killed by the system
    }

    override fun onDestroy() {
        Log.d("MusicService", "onDestroy called")
        try {
            // Release resources on the main thread.
            serviceScope.cancel() // Cancel all coroutines in this scope
            mediaSession?.run {
                exoPlayer.release() // Release ExoPlayer
                release() // Release MediaSession
                mediaSession = null
            }
            Log.d("MusicService", "ExoPlayer and MediaSession released")
        } catch (e: Exception) {
            Log.e("MusicService", "Error during onDestroy: ${e.message}", e)
            // FirebaseCrashlytics.getInstance().recordException(e)
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MUSIC_NOTIFICATION_CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW // Use LOW for ongoing background playback
            ).apply {
                description = "Notifications for music playback controls"
                setSound(null, null) // No sound for this notification channel
                enableLights(false)
                enableVibration(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @OptIn(UnstableApi::class)
    private inner class MediaSessionCallback : MediaSession.Callback {
        // You can override specific callbacks here if you need custom logic for
        // handling media controls (e.g., custom actions).
        // For basic playback controls, ExoPlayer's default MediaSession behavior is often sufficient.
    }
}