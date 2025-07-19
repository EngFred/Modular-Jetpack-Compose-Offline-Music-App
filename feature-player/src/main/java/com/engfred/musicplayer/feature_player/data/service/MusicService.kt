package com.engfred.musicplayer.feature_player.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import com.engfred.musicplayer.feature_library.domain.usecases.PermissionHandlerUseCase
import com.engfred.musicplayer.feature_player.domain.model.PlaybackState
import com.engfred.musicplayer.feature_player.domain.model.RepeatMode
import com.engfred.musicplayer.feature_player.domain.model.ShuffleMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay // Import delay
import kotlinx.coroutines.withContext
import javax.inject.Inject

// Notification Channel ID
const val MUSIC_NOTIFICATION_CHANNEL_ID = "music_playback_channel"

@UnstableApi
@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject
    lateinit var exoPlayer: ExoPlayer

    @Inject
    lateinit var audioFileMapper: AudioFileMapper

    @Inject
    lateinit var musicNotificationProvider: MusicNotificationProvider

    @Inject
    lateinit var permissionHandlerUseCase: PermissionHandlerUseCase // Injected to check permissions

    private var mediaSession: MediaSession? = null
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState = _playbackState.asStateFlow()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val startTime = System.currentTimeMillis()
        Log.d("MusicService", "onCreate started")

        // Start foreground with a temporary notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
            val notification = Notification.Builder(this, MUSIC_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Music Player")
                .setContentText("Starting music service...")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
            try {
                startForeground(1, notification)
                Log.d("MusicService", "Foreground started: ${System.currentTimeMillis() - startTime}ms")
            } catch (e: Exception) {
                Log.e("MusicService", "Failed to start foreground service: ${e.message}")
                // Log to Crashlytics in production
                // FirebaseCrashlytics.getInstance().recordException(e)
            }
        }

        // Check permissions before initializing media components
        if (!permissionHandlerUseCase.hasAudioPermission()) {
            Log.w("MusicService", "Audio permissions not granted, stopping service")
            stopSelf()
            return
        }

        // Initialize in background
        serviceScope.launch(Dispatchers.IO) {
            try {
                val initStart = System.currentTimeMillis()
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build()

                (exoPlayer as? ExoPlayer)?.let {
                    it.setAudioAttributes(audioAttributes, true)
                    it.setHandleAudioBecomingNoisy(true)
                }
                Log.d("MusicService", "ExoPlayer initialized: ${System.currentTimeMillis() - initStart}ms")

                withContext(Dispatchers.Main) {
                    mediaSession = MediaSession.Builder(this@MusicService, exoPlayer)
                        .setCallback(MediaSessionCallback())
                        .build()
                    setMediaNotificationProvider(musicNotificationProvider)
                    Log.d("MusicService", "MediaSession created: ${System.currentTimeMillis() - initStart}ms")
                }

                exoPlayer.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        _playbackState.value = _playbackState.value.copy(
                            isLoading = playbackState == Player.STATE_BUFFERING
                        )
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        mediaItem?.let {
                            val currentAudioFile = audioFileMapper.mapMediaItemToAudioFile(it)
                            _playbackState.value = _playbackState.value.copy(
                                currentAudioFile = currentAudioFile,
                                playbackPositionMs = exoPlayer.currentPosition,
                                totalDurationMs = exoPlayer.duration
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
                            error = error.message ?: "Unknown playback error"
                        )
                        Log.e("MusicService", "Playback error: ${error.message}")
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
                Log.e("MusicService", "Initialization failed: ${e.message}")
                // FirebaseCrashlytics.getInstance().recordException(e)
                withContext(Dispatchers.Main) {
                    _playbackState.value = _playbackState.value.copy(
                        error = "Service initialization failed: ${e.message}"
                    )
                }
            }
        }

        // Update playback position in background
        serviceScope.launch(Dispatchers.Default) {
            while (true) {
                if (exoPlayer.isPlaying || exoPlayer.playbackState == Player.STATE_BUFFERING) {
                    val newState = _playbackState.value.copy(
                        playbackPositionMs = exoPlayer.currentPosition,
                        bufferedPositionMs = exoPlayer.bufferedPosition,
                        totalDurationMs = exoPlayer.duration
                    )
                    withContext(Dispatchers.Main) {
                        _playbackState.value = newState
                    }
                }
                delay(2000) // Update every 2 seconds
            }
        }

        Log.d("MusicService", "onCreate completed: ${System.currentTimeMillis() - startTime}ms")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        try {
            mediaSession?.run {
                exoPlayer.release()
                release()
                mediaSession = null
            }
            serviceScope.cancel()
        } catch (e: Exception) {
            Log.e("MusicService", "Error during onDestroy: ${e.message}")
            // FirebaseCrashlytics.getInstance().recordException(e)
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MUSIC_NOTIFICATION_CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for music playback controls"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @OptIn(UnstableApi::class)
    private inner class MediaSessionCallback : MediaSession.Callback {
        // Custom callback logic if needed
    }
}