package com.engfred.musicplayer.feature_player.data.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Build
import android.util.Log
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
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.model.AudioPreset
import com.engfred.musicplayer.core.domain.model.FilterOption
import com.engfred.musicplayer.core.domain.model.LastPlaybackState
import com.engfred.musicplayer.core.domain.model.WidgetBackgroundMode
import com.engfred.musicplayer.core.domain.repository.LibraryRepository
import com.engfred.musicplayer.core.domain.repository.PlaybackController
import com.engfred.musicplayer.core.domain.repository.RepeatMode
import com.engfred.musicplayer.core.domain.repository.SettingsRepository
import com.engfred.musicplayer.core.domain.repository.ShuffleMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

const val MUSIC_NOTIFICATION_CHANNEL_ID = "music_playback_channel"
const val MUSIC_NOTIFICATION_ID = 101
private const val UNKNOWN_ARTIST = "Unknown Artist"

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
    private var lastIdleDisplayInfo: WidgetDisplayInfo? = null
    private var preferredRepeatMode: RepeatMode = RepeatMode.OFF
    private var widgetThemeAware: Boolean = false

    companion object {
        const val ACTION_WIDGET_PLAY_PAUSE = "com.engfred.musicplayer.ACTION_WIDGET_PLAY_PAUSE"
        const val ACTION_WIDGET_NEXT = "com.engfred.musicplayer.ACTION_WIDGET_NEXT"
        const val ACTION_WIDGET_PREV = "com.engfred.musicplayer.ACTION_WIDGET_PREV"
        const val ACTION_REFRESH_WIDGET = "com.engfred.musicplayer.ACTION_REFRESH_WIDGET"
        const val ACTION_WIDGET_REPEAT = "com.engfred.musicplayer.ACTION_WIDGET_REPEAT"
        const val WIDGET_PROVIDER_CLASS = "com.engfred.musicplayer.widget.PlayerWidgetProvider"
        private const val TAG = "PlaybackService"
        private const val PERIODIC_SAVE_INTERVAL_MS = 10000L
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
                    updateWidgetWithInfo()
                }

                @RequiresApi(Build.VERSION_CODES.P)
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    if (mediaItem == null) {
                        serviceScope.launch {
                            loadLastIdleDisplayInfo()
                            updateWidgetWithInfo()
                        }
                    } else {
                        updateWidgetWithInfo()
                    }
                }

                @RequiresApi(Build.VERSION_CODES.P)
                override fun onPositionDiscontinuity(reason: Int) {
                    super.onPositionDiscontinuity(reason)
                    updateWidgetWithInfo()
                }

                private fun updateWidgetWithInfo() {
                    val idleInfo = if (exoPlayer.currentMediaItem == null) lastIdleDisplayInfo else null
                    val idleRepeat = if (exoPlayer.currentMediaItem == null) getIdleRepeatMode() else Player.REPEAT_MODE_OFF
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        // user interactions are debounced in WidgetUpdater; don't force immediate here
                        WidgetUpdater.updateWidget(this@PlaybackService, exoPlayer, idleInfo, idleRepeat, widgetThemeAware)
                    }
                }
            })

            // Start periodic update for duration (1s)
            serviceScope.launch {
                while (true) {
                    delay(1000)
                    if (exoPlayer.isPlaying) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val idleInfo = if (exoPlayer.currentMediaItem == null) lastIdleDisplayInfo else null
                            val idleRepeat = if (exoPlayer.currentMediaItem == null) getIdleRepeatMode() else Player.REPEAT_MODE_OFF
                            WidgetUpdater.updateWidget(this@PlaybackService, exoPlayer, idleInfo, idleRepeat, widgetThemeAware)
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

            // Load last idle display info, preferred repeat, and initial widget update
            serviceScope.launch {
                loadLastIdleDisplayInfo()
                val appSettings = settingsRepository.getAppSettings().first()
                preferredRepeatMode = appSettings.repeatMode
                widgetThemeAware = (appSettings.widgetBackgroundMode == WidgetBackgroundMode.THEME_AWARE)
                val idleInfo = if (exoPlayer.currentMediaItem == null) lastIdleDisplayInfo else null
                val idleRepeat = if (exoPlayer.currentMediaItem == null) getIdleRepeatMode() else Player.REPEAT_MODE_OFF
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // Initial startup: bypass debounce so widget is set immediately with last state.
                    WidgetUpdater.updateWidget(
                        this@PlaybackService,
                        exoPlayer,
                        idleInfo,
                        idleRepeat,
                        widgetThemeAware,
                        forceImmediate = true
                    )
                }
            }

            // Observe settings for preset and repeat changes
            serviceScope.launch {
                settingsRepository.getAppSettings().collect { settings ->
                    val oldRepeat = preferredRepeatMode
                    preferredRepeatMode = settings.repeatMode
                    val oldWidgetTheme = widgetThemeAware
                    widgetThemeAware = (settings.widgetBackgroundMode == WidgetBackgroundMode.THEME_AWARE)
                    applyAudioPreset(settings.audioPreset)

                    // Update widget when repeat or widget-mode changed.
                    // IMPORTANT: always trigger update so theme changes are visible even when player is idle
                    if (oldRepeat != preferredRepeatMode || oldWidgetTheme != widgetThemeAware) {
                        val idleInfo = lastIdleDisplayInfo
                        val idleRepeat = getIdleRepeatMode()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            // Do not forceImmediate — allow coalescing for rapid UI toggles.
                            WidgetUpdater.updateWidget(this@PlaybackService, exoPlayer, idleInfo, idleRepeat, widgetThemeAware)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onCreate error: ${e.message}", e)
            stopSelf()
        }
    }

    private fun getIdleRepeatMode(): Int = when (preferredRepeatMode) {
        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
    }

    private suspend fun loadLastIdleDisplayInfo() {
        val lastState = settingsRepository.getLastPlaybackState().first()
        if (lastState.audioId != null) {
            val audios = libRepo.getAllAudioFiles().first()
            val audio = audios.find { it.id == lastState.audioId }
            if (audio != null) {
                lastIdleDisplayInfo = WidgetDisplayInfo(
                    title = audio.title,
                    artist = audio.artist ?: UNKNOWN_ARTIST,
                    durationMs = audio.duration,
                    positionMs = lastState.positionMs.coerceAtLeast(0L).coerceAtMost(audio.duration),
                    artworkUri = audio.albumArtUri
                )
                Log.d(TAG, "Cached last idle display info: ${audio.title} by ${audio.artist}")
            } else {
                // Clear invalid state
                settingsRepository.saveLastPlaybackState(LastPlaybackState(null))
                lastIdleDisplayInfo = null
                Log.w(TAG, "Last audio ID ${lastState.audioId} not found; cleared state")
            }
        } else {
            lastIdleDisplayInfo = null
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

    /**
     * New: central widget play/pause handling that calls playbackController directly.
     * This mirrors the previous PlaybackActions logic but uses the controller's deterministic seek.
     */
    private suspend fun handleWidgetPlayPause() {
        try {
            if (exoPlayer.mediaItemCount == 0) {
                val lastState = settingsRepository.getLastPlaybackState().first()
                val deviceAudios = libRepo.getAllAudioFiles().first()

                if (deviceAudios.isNotEmpty()) {
                    val filter = settingsRepository.getFilterOption().first()
                    val appSettings = settingsRepository.getAppSettings().first()
                    val repeatMode = appSettings.repeatMode
                    val sortedAudios = when (filter) {
                        FilterOption.DATE_ADDED_ASC -> deviceAudios.sortedBy { it.dateAdded }
                        FilterOption.DATE_ADDED_DESC -> deviceAudios.sortedByDescending { it.dateAdded }
                        FilterOption.LENGTH_ASC -> deviceAudios.sortedBy { it.duration }
                        FilterOption.LENGTH_DESC -> deviceAudios.sortedByDescending { it.duration }
                        FilterOption.ALPHABETICAL_ASC -> deviceAudios.sortedBy { it.title.lowercase() }
                        FilterOption.ALPHABETICAL_DESC -> deviceAudios.sortedByDescending { it.title.lowercase() }
                    }
                    Log.d(TAG, "Widget: applied sort order $filter -> queue size ${sortedAudios.size}")

                    val playingQueue = lastState.queueIds?.takeIf { it.isNotEmpty() }?.let { ids ->
                        val idToAudio = deviceAudios.associateBy { it.id }
                        ids.mapNotNull { idToAudio[it] }.takeIf { it.isNotEmpty() } ?: sortedAudios
                    } ?: sortedAudios

                    val isResuming = lastState.audioId != null
                    var audioToPlay: AudioFile? = lastState.audioId?.let { id ->
                        playingQueue.find { it.id == id }
                    }
                    val resumePositionMs = if (audioToPlay != null) lastState.positionMs else C.TIME_UNSET

                    if (audioToPlay == null) {
                        audioToPlay = playingQueue.firstOrNull()
                        if (isResuming) {
                            settingsRepository.saveLastPlaybackState(LastPlaybackState(null))
                            Log.w(TAG, "Widget: last audio ID ${lastState.audioId} not found; cleared state.")
                        }
                    }

                    if (audioToPlay != null) {
                        sharedAudioDataSource.setPlayingQueue(playingQueue)
                        playbackController.setRepeatMode(repeatMode)
                        playbackController.setShuffleMode(ShuffleMode.OFF)
                        Log.d(TAG, "Widget: set repeat=$repeatMode shuffle=OFF and initiating playback uri=${audioToPlay.uri} resume=$resumePositionMs")

                        // IMPORTANT: Use controller API to set queue and apply resume pos deterministically
                        playbackController.initiatePlayback(audioToPlay.uri, resumePositionMs)
                    }
                } else {
                    Log.w(TAG, "Widget: no device audios available")
                }
            } else {
                playbackController.playPause()
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleWidgetPlayPause error: ${e.message}", e)
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
                    serviceScope.launch {
                        // use the new service-internal handler which calls playbackController directly
                        handleWidgetPlayPause()
                        val idleInfo = if (exoPlayer.currentMediaItem == null) lastIdleDisplayInfo else null
                        val idleRepeat = if (exoPlayer.currentMediaItem == null) getIdleRepeatMode() else Player.REPEAT_MODE_OFF
                        // debounced update for user interactions
                        WidgetUpdater.updateWidget(this@PlaybackService, exoPlayer, idleInfo, idleRepeat, widgetThemeAware)
                    }
                }
                ACTION_WIDGET_NEXT -> {
                    try {
                        exoPlayer.seekToNextMediaItem()
                    } catch (_: Exception) {}
                    val idleInfo = if (exoPlayer.currentMediaItem == null) lastIdleDisplayInfo else null
                    val idleRepeat = if (exoPlayer.currentMediaItem == null) getIdleRepeatMode() else Player.REPEAT_MODE_OFF
                    WidgetUpdater.updateWidget(this, exoPlayer, idleInfo, idleRepeat, widgetThemeAware)
                }
                ACTION_WIDGET_PREV -> {
                    try {
                        exoPlayer.seekToPreviousMediaItem()
                    } catch (_: Exception) {}
                    val idleInfo = if (exoPlayer.currentMediaItem == null) lastIdleDisplayInfo else null
                    val idleRepeat = if (exoPlayer.currentMediaItem == null) getIdleRepeatMode() else Player.REPEAT_MODE_OFF
                    WidgetUpdater.updateWidget(this, exoPlayer, idleInfo, idleRepeat, widgetThemeAware)
                }
                ACTION_REFRESH_WIDGET -> {
                    val idleInfo = if (exoPlayer.currentMediaItem == null) lastIdleDisplayInfo else null
                    val idleRepeat = if (exoPlayer.currentMediaItem == null) getIdleRepeatMode() else Player.REPEAT_MODE_OFF
                    WidgetUpdater.updateWidget(this, exoPlayer, idleInfo, idleRepeat, widgetThemeAware)
                }
                ACTION_WIDGET_REPEAT -> {
                    // reuse existing repeat toggle
                    PlaybackActions.handleRepeatToggle(exoPlayer, settingsRepository, serviceScope)
                    val idleInfo = if (exoPlayer.currentMediaItem == null) lastIdleDisplayInfo else null
                    val idleRepeat = if (exoPlayer.currentMediaItem == null) getIdleRepeatMode() else Player.REPEAT_MODE_OFF
                    WidgetUpdater.updateWidget(this, exoPlayer, idleInfo, idleRepeat, widgetThemeAware)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "onStartCommand action handling error: ${e.message}")
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
