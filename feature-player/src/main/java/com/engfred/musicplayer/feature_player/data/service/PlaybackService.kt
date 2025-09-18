package com.engfred.musicplayer.feature_player.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.audiofx.Equalizer
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.engfred.musicplayer.core.data.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AudioPreset
import com.engfred.musicplayer.core.domain.repository.LibraryRepository
import com.engfred.musicplayer.core.domain.repository.PlaybackController
import com.engfred.musicplayer.core.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

const val MUSIC_NOTIFICATION_CHANNEL_ID = "music_playback_channel"
const val MUSIC_NOTIFICATION_ID = 101

@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var musicNotificationProvider: MusicNotificationProvider

    @Inject
    lateinit var playbackController: PlaybackController

    @Inject
    lateinit var libRepo: LibraryRepository

    @Inject
    lateinit var sharedAudioDataSource: SharedAudioDataSource

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private lateinit var exoPlayer: ExoPlayer
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var equalizer: Equalizer? = null

    companion object {
        const val ACTION_WIDGET_PLAY_PAUSE = "com.engfred.musicplayer.ACTION_WIDGET_PLAY_PAUSE"
        const val ACTION_WIDGET_NEXT = "com.engfred.musicplayer.ACTION_WIDGET_NEXT"
        const val ACTION_WIDGET_PREV = "com.engfred.musicplayer.ACTION_WIDGET_PREV"
        const val ACTION_REFRESH_WIDGET = "com.engfred.musicplayer.ACTION_REFRESH_WIDGET"
        const val ACTION_WIDGET_REPEAT = "com.engfred.musicplayer.ACTION_WIDGET_REPEAT"
        const val WIDGET_PROVIDER_CLASS = "com.engfred.musicplayer.widget.PlayerWidgetProvider"
        private const val TAG = "PlaybackService"
        private const val PERIODIC_SAVE_INTERVAL_MS = 5000L
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notification = NotificationCompat.Builder(this, MUSIC_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Music Player")
                .setContentText("Starting music service...")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .setSilent(true)
                .build()
            try {
                startForeground(MUSIC_NOTIFICATION_ID, notification)
            } catch (_: Exception) {
                stopSelf()
                return
            }
        }

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
            exoPlayer = ExoPlayer.Builder(this).build().apply {
                setAudioAttributes(audioAttributes, true)
                setHandleAudioBecomingNoisy(true)
            }

            val intent = Intent().setClassName(this, "${packageName}.MainActivity")
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)

            mediaSession = MediaSession.Builder(this, exoPlayer)
                .setSessionActivity(pendingIntent)
                .build()

            setMediaNotificationProvider(musicNotificationProvider)

            // listen for player changes and update widget
            exoPlayer.addListener(object : Player.Listener {
                @RequiresApi(Build.VERSION_CODES.P)
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    WidgetUpdater.updateWidget(this@PlaybackService, exoPlayer)
                }

                @RequiresApi(Build.VERSION_CODES.P)
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    WidgetUpdater.updateWidget(this@PlaybackService, exoPlayer)
                }

                @RequiresApi(Build.VERSION_CODES.P)
                override fun onPositionDiscontinuity(reason: Int) {
                    super.onPositionDiscontinuity(reason)
                    WidgetUpdater.updateWidget(this@PlaybackService, exoPlayer)
                }
            })

            // Start periodic update for duration (1s)
            serviceScope.launch {
                while (true) {
                    delay(1000)
                    if (exoPlayer.isPlaying) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            WidgetUpdater.updateWidget(this@PlaybackService, exoPlayer)
                        }
                    }
                }
            }

            // Periodic state saving
            serviceScope.launch {
                while (true) {
                    delay(PERIODIC_SAVE_INTERVAL_MS)
                    if (exoPlayer.currentMediaItem != null) {
                        savePlaybackStateAsync(serviceScope, settingsRepository, exoPlayer)
                    }
                }
            }

            equalizer = Equalizer(0, exoPlayer.audioSessionId)

            // Observe settings and apply preset
            serviceScope.launch {
                settingsRepository.getAppSettings().collect { settings ->
                    applyAudioPreset(settings.audioPreset)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onCreate error: ${e.message}", e)
            stopSelf()
        }
    }

    private fun applyAudioPreset(preset: AudioPreset) {
        try {
            equalizer?.let { eq ->
                eq.enabled = preset != AudioPreset.NONE
                if (preset != AudioPreset.NONE) {
                    val presetIndex = when (preset) {
                        AudioPreset.ROCK -> 9
                        AudioPreset.JAZZ -> 7
                        AudioPreset.POP -> 8
                        AudioPreset.CLASSICAL -> 1
                        AudioPreset.DANCE -> 2
                        AudioPreset.HIP_HOP -> 6
                        else -> 0
                    }.toShort()
                    eq.usePreset(presetIndex)
                    Log.d(TAG, "Applied audio preset: $preset (index: $presetIndex)")
                } else {
                    Log.d(TAG, "Disabled equalizer for preset: NONE")
                }
            } ?: Log.w(TAG, "Equalizer not initialized when applying preset.")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying audio preset: ${e.message}", e)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                ACTION_WIDGET_PLAY_PAUSE -> {
                    PlaybackActions.handlePlayPauseFromWidget(
                        exoPlayer,
                        libRepo,
                        settingsRepository,
                        sharedAudioDataSource,
                        playbackController,
                        serviceScope
                    )
                    WidgetUpdater.updateWidget(this, exoPlayer)
                }
                ACTION_WIDGET_NEXT -> {
                    try {
                        exoPlayer.seekToNextMediaItem()
                    } catch (_: Exception) {}
                    WidgetUpdater.updateWidget(this, exoPlayer)
                }
                ACTION_WIDGET_PREV -> {
                    try {
                        exoPlayer.seekToPreviousMediaItem()
                    } catch (_: Exception) {}
                    WidgetUpdater.updateWidget(this, exoPlayer)
                }
                ACTION_REFRESH_WIDGET -> WidgetUpdater.updateWidget(this, exoPlayer)
                ACTION_WIDGET_REPEAT -> {
                    PlaybackActions.handleRepeatToggle(exoPlayer, settingsRepository, serviceScope)
                    WidgetUpdater.updateWidget(this, exoPlayer)
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        try {
            // Save last state synchronously (as before)
            saveLastPlaybackStateBlocking(settingsRepository, exoPlayer)
            serviceScope.cancel()
            mediaSession?.run {
                exoPlayer.release()
                release()
                mediaSession = null
            }
            equalizer?.release()
            equalizer = null
        } catch (_: Exception) {
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
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
