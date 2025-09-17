package com.engfred.musicplayer.feature_player.data.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.graphics.createBitmap
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.model.AudioPreset
import com.engfred.musicplayer.core.domain.model.FilterOption
import com.engfred.musicplayer.core.domain.model.LastPlaybackState
import com.engfred.musicplayer.core.domain.repository.LibraryRepository
import com.engfred.musicplayer.core.domain.repository.PlaybackController
import com.engfred.musicplayer.core.domain.repository.RepeatMode
import com.engfred.musicplayer.core.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import androidx.core.net.toUri

const val MUSIC_NOTIFICATION_CHANNEL_ID = "music_playback_channel"
const val MUSIC_NOTIFICATION_ID = 101

@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    // Inject provider (factory) — we create and manage concrete players locally
    @Inject
    lateinit var exoPlayerProvider: ExoPlayerProvider

    @Inject
    lateinit var musicNotificationProvider: MusicNotificationProvider

    @Inject
    lateinit var playbackController: PlaybackController

    @Inject
    lateinit var libRepo: LibraryRepository

    @Inject
    lateinit var sharedAudioDataSource: SharedAudioDataSource

    @Inject
    lateinit var settingsRepository: SettingsRepository //for last sort/filter

    private var mediaSession: MediaSession? = null

    // Local player reference — always use this (may be recreated)
    private var player: ExoPlayer? = null

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
    }

    // Tunables
    private val RESUME_TOTAL_WAIT_MS = 30_000L
    private val INITIATE_RETRY_COUNT = 2
    private val INITIATE_RETRY_DELAY_MS = 800L

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Start foreground as before
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
            } catch (e: Exception) {
                stopSelf()
                return
            }
        }

        try {
            // Create a fresh player for this service instance
            player = exoPlayerProvider.create()
            configurePlayerAndSession(player!!)

            // Add listeners (widget refresh)
            player?.addListener(object : Player.Listener {
                @RequiresApi(Build.VERSION_CODES.P)
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    updateWidget()
                }

                @RequiresApi(Build.VERSION_CODES.P)
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    updateWidget()
                }

                @RequiresApi(Build.VERSION_CODES.P)
                override fun onPositionDiscontinuity(reason: Int) {
                    super.onPositionDiscontinuity(reason)
                    updateWidget()
                }
            })

            // Periodic widget updates (safe)
            serviceScope.launch {
                while (true) {
                    delay(1000)
                    try {
                        if (player?.isPlaying == true) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) updateWidget()
                        }
                    } catch (t: Throwable) {
                        // If dead handler or other player error, recreate and continue
                        Log.w(TAG, "Periodic update threw: ${t.message}")
                        recreatePlayerAndSessionIfNeeded()
                    }
                }
            }

            // Setup equalizer
            equalizer = Equalizer(0, player?.audioSessionId ?: 0)

            // Observe app settings and apply audio preset
            serviceScope.launch {
                settingsRepository.getAppSettings().collect { settings ->
                    applyAudioPreset(settings.audioPreset)
                }
            }

            // Try restore last playback asynchronously
            serviceScope.launch {
                safeStartupRestoreIfAny()
            }
        } catch (e: Exception) {
            Log.e(TAG, "onCreate error: ${e.message}", e)
            stopSelf()
        }
    }

    private fun configurePlayerAndSession(p: ExoPlayer) {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
            p.setAudioAttributes(audioAttributes, true)
            p.setHandleAudioBecomingNoisy(true)

            val intent = Intent().setClassName(this, "${packageName}.MainActivity")
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)

            mediaSession?.release()
            mediaSession = MediaSession.Builder(this, p)
                .setSessionActivity(pendingIntent)
                .build()

            setMediaNotificationProvider(musicNotificationProvider)
        } catch (t: Throwable) {
            Log.w(TAG, "configurePlayerAndSession hit error: ${t.message}")
            // Attempt recreate when configuration fails
            recreatePlayerAndSessionIfNeeded()
        }
    }

    private fun recreatePlayerAndSessionIfNeeded() {
        try {
            // release old
            try {
                player?.release()
            } catch (e: Throwable) {
                Log.w(TAG, "Ignored error releasing old player: ${e.message}")
            }
            player = null

            // create a fresh instance
            val newPlayer = exoPlayerProvider.create()
            player = newPlayer
            configurePlayerAndSession(newPlayer)

            // re-attach small listener for widgets
            newPlayer.addListener(object : Player.Listener {
                @RequiresApi(Build.VERSION_CODES.P)
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    updateWidget()
                }
                @RequiresApi(Build.VERSION_CODES.P)
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    updateWidget()
                }
                @RequiresApi(Build.VERSION_CODES.P)
                override fun onPositionDiscontinuity(reason: Int) {
                    super.onPositionDiscontinuity(reason)
                    updateWidget()
                }
            })

            // reapply equalizer with new audioSessionId
            equalizer?.release()
            equalizer = Equalizer(0, newPlayer.audioSessionId)

            // Attempt to restore queue + position
            serviceScope.launch {
                safeStartupRestoreIfAny()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to recreate player & session: ${e.message}", e)
        }
    }

    /**
     * Attempt to restore queue and position when service starts or after recreate.
     * Non-blocking; best-effort.
     */
    private suspend fun safeStartupRestoreIfAny() {
        try {
            val lastState = settingsRepository.getLastPlaybackState().first()
            val deviceAudios = libRepo.getAllAudioFiles().first()
            if (deviceAudios.isEmpty()) return

            val filter = settingsRepository.getFilterOption().first()
            val appSettings = settingsRepository.getAppSettings().first()
            val repeatMode = appSettings.repeatMode
            val shuffleMode = appSettings.shuffleMode

            val sortedAudios = when (filter) {
                FilterOption.DATE_ADDED_ASC -> deviceAudios.sortedBy { it.dateAdded }
                FilterOption.DATE_ADDED_DESC -> deviceAudios.sortedByDescending { it.dateAdded }
                FilterOption.LENGTH_ASC -> deviceAudios.sortedBy { it.duration }
                FilterOption.LENGTH_DESC -> deviceAudios.sortedByDescending { it.duration }
                FilterOption.ALPHABETICAL_ASC -> deviceAudios.sortedBy { it.title.lowercase() }
                FilterOption.ALPHABETICAL_DESC -> deviceAudios.sortedByDescending { it.title.lowercase() }
            }

            // Apply queue + modes
            sharedAudioDataSource.setPlayingQueue(sortedAudios)
            playbackController.setRepeatMode(repeatMode)
            playbackController.setShuffleMode(shuffleMode)

            val target: AudioFile? = lastState?.audioId?.let { id -> sortedAudios.find { it.id == id } }

            if (target != null) {
                var initiated = false
                var attempts = 0
                while (!initiated && attempts <= INITIATE_RETRY_COUNT) {
                    try {
                        playbackController.initiatePlayback(target.uri)
                        initiated = true
                    } catch (t: Throwable) {
                        attempts++
                        Log.w(TAG, "startup restore: initiatePlayback failed (attempt $attempts) -> ${t.message}")
                        delay(INITIATE_RETRY_DELAY_MS)
                    }
                }

                if (initiated) {
                    if (playbackController.waitUntilReady(RESUME_TOTAL_WAIT_MS)) {
                        try {
                            playbackController.seekTo(lastState.positionMs)
                            // keep paused after seeking, if you prefer resume on click, pause here:
                            player?.pause()
                            Log.d(TAG, "Startup restore: seeked to ${lastState.positionMs}ms for audio ${lastState.audioId}")
                        } catch (t: Throwable) {
                            Log.w(TAG, "Startup restore: seek failed -> ${t.message}")
                        }
                    } else {
                        Log.w(TAG, "Startup restore: controller not ready to seek within timeout.")
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "safeStartupRestoreIfAny threw: ${t.message}")
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
                    // safe wrapper: detect dead handler and recreate once if necessary
                    safeExecute {
                        handlePlayPauseFromWidgetInternal()
                    }
                    updateWidget()
                }
                ACTION_WIDGET_NEXT -> {
                    safeExecute { player?.seekToNextMediaItem() }
                    updateWidget()
                }
                ACTION_WIDGET_PREV -> {
                    safeExecute { player?.seekToPreviousMediaItem() }
                    updateWidget()
                }
                ACTION_REFRESH_WIDGET -> updateWidget()
                ACTION_WIDGET_REPEAT -> {
                    handleRepeatToggle()
                    updateWidget()
                }
            }
        } catch (e: Exception) {
            // swallow to keep service stable
        }
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    /**
     * Helper to run a suspend block and on a detected dead-handler IllegalStateException
     * recreate the player/session and retry once.
     */
    private fun safeExecute(block: suspend () -> Unit) {
        serviceScope.launch {
            try {
                block()
            } catch (t: Throwable) {
                if (t is IllegalStateException && t.message?.contains("dead thread") == true) {
                    Log.w(TAG, "Detected dead-thread IllegalStateException; recreating player & media session then retrying.")
                    recreatePlayerAndSessionIfNeeded()
                    try {
                        block()
                    } catch (inner: Throwable) {
                        Log.e(TAG, "Retry after recreate failed: ${inner.message}", inner)
                    }
                } else {
                    Log.w(TAG, "safeExecute caught: ${t.message}")
                }
            }
        }
    }

    /**
     * Internal copy of your widget-play/pause handler (adapted to use local player and controller).
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private suspend fun handlePlayPauseFromWidgetInternal() {
        // if no media items loaded, build queue and attempt to resume
        if (player?.mediaItemCount == 0) {
            val lastState = settingsRepository.getLastPlaybackState().first()
            val deviceAudios = libRepo.getAllAudioFiles().first()
            if (deviceAudios.isNotEmpty()) {
                val filter = settingsRepository.getFilterOption().first()
                val appSettings = settingsRepository.getAppSettings().first()
                val repeatMode = appSettings.repeatMode
                val shuffleMode = appSettings.shuffleMode
                val sortedAudios = when (filter) {
                    FilterOption.DATE_ADDED_ASC -> deviceAudios.sortedBy { it.dateAdded }
                    FilterOption.DATE_ADDED_DESC -> deviceAudios.sortedByDescending { it.dateAdded }
                    FilterOption.LENGTH_ASC -> deviceAudios.sortedBy { it.duration }
                    FilterOption.LENGTH_DESC -> deviceAudios.sortedByDescending { it.duration }
                    FilterOption.ALPHABETICAL_ASC -> deviceAudios.sortedBy { it.title.lowercase() }
                    FilterOption.ALPHABETICAL_DESC -> deviceAudios.sortedByDescending { it.title.lowercase() }
                }

                val isResuming = lastState.audioId != null
                var audioToPlay: AudioFile? = null
                var resumePositionMs = 0L

                if (isResuming) {
                    val targetAudio = sortedAudios.find { it.id == lastState.audioId }
                    if (targetAudio != null) {
                        audioToPlay = targetAudio
                        resumePositionMs = lastState.positionMs
                        Log.d(TAG, "Resuming playback for audio ID ${lastState.audioId} at ${lastState.positionMs}ms in sorted queue")
                    } else {
                        settingsRepository.saveLastPlaybackState(LastPlaybackState(null))
                        Log.w(TAG, "Last audio ID ${lastState.audioId} not found in sorted queue; cleared state and falling back to first sorted song")
                    }
                }

                if (audioToPlay == null) {
                    audioToPlay = sortedAudios.first()
                    Log.d(TAG, "Starting fresh playback from first sorted song")
                }

                // Set queue & modes BEFORE playback
                sharedAudioDataSource.setPlayingQueue(sortedAudios)
                playbackController.setRepeatMode(repeatMode)
                playbackController.setShuffleMode(shuffleMode)
                Log.d(TAG, "Set repeat: $repeatMode, shuffle: $shuffleMode for playback")

                // Wait for controller to attach (long timeout) — improves reliability after long kill
                if (!playbackController.waitUntilReady(RESUME_TOTAL_WAIT_MS)) {
                    Log.w(TAG, "MediaController not ready after wait; proceeding to attempt initiatePlayback anyway.")
                }

                // Try initiate with small retry
                var initiated = false
                var attempt = 0
                while (!initiated && attempt <= INITIATE_RETRY_COUNT) {
                    try {
                        playbackController.initiatePlayback(audioToPlay.uri)
                        initiated = true
                    } catch (t: Throwable) {
                        attempt++
                        Log.w(TAG, "initiatePlayback attempt $attempt failed: ${t.message}")
                        if (attempt <= INITIATE_RETRY_COUNT) delay(INITIATE_RETRY_DELAY_MS)
                    }
                }

                if (initiated && isResuming && audioToPlay.id == lastState.audioId) {
                    if (playbackController.waitUntilReady(RESUME_TOTAL_WAIT_MS)) {
                        try {
                            playbackController.seekTo(resumePositionMs)
                            Log.d(TAG, "Seeked to resume position ${resumePositionMs}ms")
                        } catch (t: Throwable) {
                            Log.w(TAG, "Failed to seek to resume position: ${t.message}")
                        }
                    } else {
                        Log.w(TAG, "Player not ready in time for resume seek; continuing from start")
                    }
                }
            } else {
                Log.w(TAG, "No device audios available; cannot start playback")
            }
        } else {
            playbackController.playPause()
        }
    }

    private fun handleRepeatToggle() {
        val current = player?.repeatMode ?: Player.REPEAT_MODE_OFF
        val next = when (current) {
            Player.REPEAT_MODE_OFF -> {
                serviceScope.launch { settingsRepository.updateRepeatMode(RepeatMode.ALL) }
                Player.REPEAT_MODE_ALL
            }
            Player.REPEAT_MODE_ALL -> {
                serviceScope.launch { settingsRepository.updateRepeatMode(RepeatMode.ONE) }
                Player.REPEAT_MODE_ONE
            }
            Player.REPEAT_MODE_ONE -> {
                serviceScope.launch { settingsRepository.updateRepeatMode(RepeatMode.OFF) }
                Player.REPEAT_MODE_OFF
            }
            else -> Player.REPEAT_MODE_OFF
        }
        try {
            player?.repeatMode = next
        } catch (t: Throwable) {
            Log.w(TAG, "Setting repeat mode threw: ${t.message}. Will recreate player.")
            recreatePlayerAndSessionIfNeeded()
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

    private fun createCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = bitmap.width.coerceAtMost(bitmap.height)
        val output = createBitmap(size, size)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, size, size)
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }

    override fun onDestroy() {
        try {
            // Save last state asynchronously before cleanup
            serviceScope.launch {
                val currentItem = player?.currentMediaItem
                val state = if (currentItem != null) {
                    val idStr = currentItem.mediaId
                    val id = idStr.toLongOrNull()
                    if (id != null) {
                        LastPlaybackState(id, player?.currentPosition ?: 0L)
                    } else null
                } else null

                if (state != null) {
                    settingsRepository.saveLastPlaybackState(state)
                    Log.d(TAG, "Saved last playback state: ID=${state.audioId}, pos=${state.positionMs}ms")
                } else {
                    settingsRepository.saveLastPlaybackState(LastPlaybackState(null))
                    Log.d(TAG, "Cleared last playback state (no current item)")
                }
            }

            serviceScope.cancel()

            try {
                mediaSession?.release()
                mediaSession = null
            } catch (t: Throwable) {
                Log.w(TAG, "Error releasing media session: ${t.message}")
            }

            try {
                player?.release()
            } catch (t: Throwable) {
                Log.w(TAG, "Error releasing player: ${t.message}")
            } finally {
                player = null
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

    private fun formatDuration(ms: Long): String {
        if (ms < 0) return "00:00"
        val seconds = (ms / 1000) % 60
        val minutes = (ms / 1000) / 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    // ---------- widget related methods below ----------
    @SuppressLint("UseKtx")
    @RequiresApi(Build.VERSION_CODES.P)
    private fun updateWidget() {
        try {
            val providerComponent = ComponentName(packageName, WIDGET_PROVIDER_CLASS)
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val ids = appWidgetManager.getAppWidgetIds(providerComponent)
            if (ids.isEmpty()) return

            val fullLayoutId = resources.getIdentifier("widget_player", "layout", packageName)
            val idleLayoutId = resources.getIdentifier("widget_player_idle", "layout", packageName)
            if (fullLayoutId == 0) return

            val current = player?.currentMediaItem
            val isIdle = current == null
            val isPlaying = player?.isPlaying == true

            val metadata = current?.mediaMetadata
            val currentPositionMs = if (isIdle) 0L else player?.currentPosition ?: 0L
            val totalDurationMs = if (isIdle) 0L else player?.duration ?: 0L
            val durationText = if (isIdle) "00:00 / 00:00" else "${formatDuration(currentPositionMs)} / ${formatDuration(totalDurationMs)}"

            val idPlayPause = resources.getIdentifier("widget_play_pause", "id", packageName)
            val idNext = resources.getIdentifier("widget_next", "id", packageName)
            val idPrev = resources.getIdentifier("widget_prev", "id", packageName)
            val idRepeat = resources.getIdentifier("widget_repeat", "id", packageName)
            val idAlbumArt = resources.getIdentifier("widget_album_art", "id", packageName)
            val idTitle = resources.getIdentifier("widget_title", "id", packageName)
            val idArtist = resources.getIdentifier("widget_artist", "id", packageName)
            val idDuration = resources.getIdentifier("widget_duration", "id", packageName)

            if (isIdle) {
                if (idleLayoutId == 0) {
                    Log.w(TAG, "widget_player_idle layout missing; falling back to full with hides")
                    buildFullWidgetWithHides(RemoteViews(packageName, fullLayoutId), idPlayPause)
                } else {
                    val baseViews = RemoteViews(packageName, idleLayoutId)
                    val playDrawableId = resources.getIdentifier("ic_play_arrow_24", "drawable", packageName)
                    if (idPlayPause != 0 && playDrawableId != 0) {
                        baseViews.setImageViewResource(idPlayPause, playDrawableId)
                    }
                    ids.forEach { appWidgetId ->
                        try {
                            val viewsCopy = RemoteViews(baseViews)
                            val playReq = ACTION_WIDGET_PLAY_PAUSE.hashCode() xor appWidgetId
                            viewsCopy.setOnClickPendingIntent(idPlayPause, perWidgetBroadcast(ACTION_WIDGET_PLAY_PAUSE, playReq, appWidgetId))
                            appWidgetManager.updateAppWidget(appWidgetId, viewsCopy)
                        } catch (e: Exception) {
                            Log.w(TAG, "Error updating idle widget id=$appWidgetId: ${e.message}")
                        }
                    }
                    return
                }
            }

            val title = metadata?.title?.toString() ?: getStringSafe("app_name")
            val artist = metadata?.artist?.toString() ?: getStringSafe("app_name")

            val baseViews = RemoteViews(packageName, fullLayoutId)

            if (idTitle != 0) baseViews.setTextViewText(idTitle, title)
            if (idArtist != 0) baseViews.setTextViewText(idArtist, artist)
            if (idDuration != 0) baseViews.setTextViewText(idDuration, durationText)

            val playDrawableResName = if (isPlaying) "ic_pause_24" else "ic_play_arrow_24"
            val playDrawableId = resources.getIdentifier(playDrawableResName, "drawable", packageName)
            if (idPlayPause != 0 && playDrawableId != 0) {
                baseViews.setImageViewResource(idPlayPause, playDrawableId)
            }

            var artBitmap: Bitmap? = null
            try {
                val artUri: Uri? = metadata?.artworkUri
                if (artUri != null) {
                    contentResolver.openInputStream(artUri)?.use { input ->
                        artBitmap = BitmapFactory.decodeStream(input)
                    }
                }
            } catch (_: Exception) { }

            if (artBitmap == null) {
                val defaultId = resources.getIdentifier("ic_music_note_24", "drawable", packageName)
                if (defaultId != 0) {
                    artBitmap = BitmapFactory.decodeResource(resources, defaultId)
                } else {
                    val fallbackId = resources.getIdentifier("ic_launcher_round", "mipmap", packageName)
                    if (fallbackId != 0) artBitmap = BitmapFactory.decodeResource(resources, fallbackId)
                }
            }
            if (artBitmap != null && idAlbumArt != 0) {
                val circularBitmap = createCircularBitmap(artBitmap)
                baseViews.setImageViewBitmap(idAlbumArt, circularBitmap)
            }

            if (idPrev != 0) baseViews.setInt(idPrev, "setColorFilter", Color.WHITE)
            if (idNext != 0) baseViews.setInt(idNext, "setColorFilter", Color.WHITE)

            val repeatMode = player?.repeatMode ?: Player.REPEAT_MODE_OFF
            val repeatDrawableResName = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> "ic_repeat_one_24"
                Player.REPEAT_MODE_ALL -> "ic_repeat_on_24"
                else -> "ic_repeat_24"
            }
            var repeatDrawableId = resources.getIdentifier(repeatDrawableResName, "drawable", packageName)
            if (repeatDrawableId == 0 && repeatMode == Player.REPEAT_MODE_ALL) {
                repeatDrawableId = resources.getIdentifier("ic_repeat_24", "drawable", packageName)
            }
            if (idRepeat != 0 && repeatDrawableId != 0) {
                baseViews.setImageViewResource(idRepeat, repeatDrawableId)
                val tintColor = when (repeatMode) {
                    Player.REPEAT_MODE_OFF -> Color.LTGRAY
                    else -> Color.WHITE
                }
                baseViews.setInt(idRepeat, "setColorFilter", tintColor)
            }

            ids.forEach { appWidgetId ->
                try {
                    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                    val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                    val viewsCopy = RemoteViews(baseViews)

                    val playReq = ACTION_WIDGET_PLAY_PAUSE.hashCode() xor appWidgetId
                    val nextReq = ACTION_WIDGET_NEXT.hashCode() xor appWidgetId
                    val prevReq = ACTION_WIDGET_PREV.hashCode() xor appWidgetId
                    val repeatReq = ACTION_WIDGET_REPEAT.hashCode() xor appWidgetId

                    if (idPlayPause != 0) viewsCopy.setOnClickPendingIntent(idPlayPause, perWidgetBroadcast(ACTION_WIDGET_PLAY_PAUSE, playReq, appWidgetId))
                    if (idNext != 0) viewsCopy.setOnClickPendingIntent(idNext, perWidgetBroadcast(ACTION_WIDGET_NEXT, nextReq, appWidgetId))
                    if (idPrev != 0) viewsCopy.setOnClickPendingIntent(idPrev, perWidgetBroadcast(ACTION_WIDGET_PREV, prevReq, appWidgetId))
                    if (idRepeat != 0) viewsCopy.setOnClickPendingIntent(idRepeat, perWidgetBroadcast(ACTION_WIDGET_REPEAT, repeatReq, appWidgetId))

                    if (idAlbumArt != 0) {
                        val openAppIntent = packageManager.getLaunchIntentForPackage(packageName)
                        openAppIntent?.data = "app://widget/open/$appWidgetId".toUri()
                        val openFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        val openPending = PendingIntent.getActivity(this, appWidgetId, openAppIntent, openFlags)
                        viewsCopy.setOnClickPendingIntent(idAlbumArt, openPending)
                    }
                    if (idTitle != 0) {
                        val openAppIntent = packageManager.getLaunchIntentForPackage(packageName)
                        openAppIntent?.data = "app://widget/open/$appWidgetId/title".toUri()
                        val openFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        val openPending = PendingIntent.getActivity(this, appWidgetId + 1, openAppIntent, openFlags)
                        viewsCopy.setOnClickPendingIntent(idTitle, openPending)
                    }
                    if (idArtist != 0) {
                        val openAppIntent = packageManager.getLaunchIntentForPackage(packageName)
                        openAppIntent?.data = "app://widget/open/$appWidgetId/artist".toUri()
                        val openFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        val openPending = PendingIntent.getActivity(this, appWidgetId + 2, openAppIntent, openFlags)
                        viewsCopy.setOnClickPendingIntent(idArtist, openPending)
                    }

                    if (minWidth < 250) {
                        if (idPrev != 0) viewsCopy.setViewVisibility(idPrev, View.GONE)
                        if (idArtist != 0) viewsCopy.setViewVisibility(idArtist, View.GONE)
                    } else {
                        if (idPrev != 0) viewsCopy.setViewVisibility(idPrev, View.VISIBLE)
                        if (idArtist != 0) viewsCopy.setViewVisibility(idArtist, View.VISIBLE)
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, viewsCopy)
                } catch (e: Exception) {
                    // ignore per-widget errors
                }
            }
        } catch (e: Exception) {
            // ignore widget update errors so service won't crash
        }
    }

    @SuppressLint("UseKtx")
    @RequiresApi(Build.VERSION_CODES.P)
    private fun buildFullWidgetWithHides(baseViews: RemoteViews, idPlayPause: Int) {
        val idAlbumArt = resources.getIdentifier("widget_album_art", "id", packageName)
        val idTitle = resources.getIdentifier("widget_title", "id", packageName)
        val idArtist = resources.getIdentifier("widget_artist", "id", packageName)
        val idDuration = resources.getIdentifier("widget_duration", "id", packageName)
        val idNext = resources.getIdentifier("widget_next", "id", packageName)
        val idPrev = resources.getIdentifier("widget_prev", "id", packageName)
        val idRepeat = resources.getIdentifier("widget_repeat", "id", packageName)

        if (idAlbumArt != 0) baseViews.setViewVisibility(idAlbumArt, View.GONE)
        if (idTitle != 0) baseViews.setViewVisibility(idTitle, View.GONE)
        if (idArtist != 0) baseViews.setViewVisibility(idArtist, View.GONE)
        if (idDuration != 0) baseViews.setViewVisibility(idDuration, View.GONE)
        if (idNext != 0) baseViews.setViewVisibility(idNext, View.GONE)
        if (idPrev != 0) baseViews.setViewVisibility(idPrev, View.GONE)
        if (idRepeat != 0) baseViews.setViewVisibility(idRepeat, View.GONE)

        val playDrawableId = resources.getIdentifier("ic_play_arrow_24", "drawable", packageName)
        if (idPlayPause != 0 && playDrawableId != 0) {
            baseViews.setImageViewResource(idPlayPause, playDrawableId)
        }

        val appWidgetManager = AppWidgetManager.getInstance(this)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(packageName, WIDGET_PROVIDER_CLASS))
        ids.forEach { appWidgetId ->
            try {
                val viewsCopy = RemoteViews(baseViews)
                val playReq = ACTION_WIDGET_PLAY_PAUSE.hashCode() xor appWidgetId
                viewsCopy.setOnClickPendingIntent(idPlayPause, perWidgetBroadcast(ACTION_WIDGET_PLAY_PAUSE, playReq, appWidgetId))
                appWidgetManager.updateAppWidget(appWidgetId, viewsCopy)
            } catch (e: Exception) {
                Log.w(TAG, "Error in fallback idle update for id=$appWidgetId: ${e.message}")
            }
        }
    }

    private fun perWidgetBroadcast(action: String, requestCode: Int, widgetId: Int): PendingIntent {
        val i = Intent().apply {
            component = ComponentName(packageName, WIDGET_PROVIDER_CLASS)
            this.action = action
            data = "app://widget/$action/$widgetId".toUri()
            `package` = packageName
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getBroadcast(this, requestCode, i, flags)
    }

    private fun getStringSafe(name: String): String {
        val resId = resources.getIdentifier(name, "string", packageName)
        return if (resId != 0) resources.getString(resId) else "Music Player"
    }
}
