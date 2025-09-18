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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject
import androidx.core.net.toUri
import com.engfred.musicplayer.core.data.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.repository.ShuffleMode
import java.util.Locale

const val MUSIC_NOTIFICATION_CHANNEL_ID = "music_playback_channel"
const val MUSIC_NOTIFICATION_ID = 101

@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var exoPlayer: ExoPlayer

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

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var equalizer: Equalizer? = null

    companion object {
        // NOTE: we use string actions that match your app module's PlayerWidgetProvider actions.
        // Keep these strings identical to the ones in the provider class in the app module.
        const val ACTION_WIDGET_PLAY_PAUSE = "com.engfred.musicplayer.ACTION_WIDGET_PLAY_PAUSE"
        const val ACTION_WIDGET_NEXT = "com.engfred.musicplayer.ACTION_WIDGET_NEXT"
        const val ACTION_WIDGET_PREV = "com.engfred.musicplayer.ACTION_WIDGET_PREV"
        // Internal: request widget refresh
        const val ACTION_REFRESH_WIDGET = "com.engfred.musicplayer.ACTION_REFRESH_WIDGET"
        const val ACTION_WIDGET_REPEAT = "com.engfred.musicplayer.ACTION_WIDGET_REPEAT"
        // Fully qualified class name of the widget provider (in the app module)
        const val WIDGET_PROVIDER_CLASS = "com.engfred.musicplayer.widget.PlayerWidgetProvider"
        private const val TAG = "PlaybackService"
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
            } catch (e: Exception) {
                stopSelf()
                return
            }
        }

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
            exoPlayer.setAudioAttributes(audioAttributes, true)
            exoPlayer.setHandleAudioBecomingNoisy(true)

            // Create PendingIntent to launch MainActivity when notification is clicked
            val intent = Intent().setClassName(this, "${packageName}.MainActivity")
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)

            mediaSession = MediaSession.Builder(this, exoPlayer)
                .setSessionActivity(pendingIntent)
                .build()

            setMediaNotificationProvider(musicNotificationProvider)

            // Listen for player changes so we can refresh widget UI
            exoPlayer.addListener(object : Player.Listener {
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

            // Start periodic update for duration
            serviceScope.launch {
                while (true) {
                    delay(1000)
                    if (exoPlayer.isPlaying) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            updateWidget()
                        }
                    }
                }
            }

            // Set up equalizer
            val audioSessionId = exoPlayer.audioSessionId
            equalizer = Equalizer(0, audioSessionId)

            // Observe app settings and apply audio preset
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
                        else -> 0 // Normal/Fallback
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
                    handlePlayPauseFromWidget()
                    updateWidget()
                }
                ACTION_WIDGET_NEXT -> {
                    try {
                        exoPlayer.seekToNextMediaItem()
                    } catch (_: Exception) {
                    }
                    updateWidget()
                }
                ACTION_WIDGET_PREV -> {
                    try {
                        exoPlayer.seekToPreviousMediaItem()
                    } catch (_: Exception) {
                    }
                    updateWidget()
                }
                ACTION_REFRESH_WIDGET -> updateWidget()
                ACTION_WIDGET_REPEAT -> {
                    handleRepeatToggle()
                    updateWidget()
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    /**
     * sort application for fresh starts and resumptions.
     * - Sort deviceAudios into sortedAudios FIRST, based on current filter (ensures last sort order applied).
     * - For resumption: Find target in sortedAudios by ID (unique); if not found, clear state.
     * - For fresh: Use sortedAudios.first() (ensures plays from sorted order, not raw deviceAudios order).
     * - Then set queue (sorted), apply repeat/shuffle modes BEFORE initiatePlayback (guarantees modes active on start).
     * - Initiate with audioToPlay.uri (from sorted, so indexOf finds correct position in sorted queue).
     * - Seek after ready for resumption.
     * - This restores original behavior (sorted.first() for fresh) while supporting resumption in sorted context.
     * - Modes applied before playback, as before.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private fun handlePlayPauseFromWidget() {
        try {
            serviceScope.launch {
                if (exoPlayer.mediaItemCount == 0) {
                    // Fetch last state for potential resumption
                    val lastState = settingsRepository.getLastPlaybackState().first()
                    val deviceAudios = libRepo.getAllAudioFiles().first() // Accessible files only (from repo)

                    if (deviceAudios.isNotEmpty()) {
                        //Apply sort FIRST to ensure last sort order is used for queue and start position
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
                        Log.d(TAG, "Applied sort order: $filter to create queue of ${sortedAudios.size} items")

                        val playingQueue = lastState.queueIds?.takeIf { it.isNotEmpty() }?.let { ids ->
                            val idToAudio = deviceAudios.associateBy { it.id }
                            ids.mapNotNull { idToAudio[it] }.takeIf { it.isNotEmpty() } ?: sortedAudios
                        } ?: sortedAudios

                        val isResuming = lastState.audioId != null
                        var audioToPlay: AudioFile? = lastState.audioId?.let { id ->
                            playingQueue.find { it.id == id }
                        }
                        var resumePositionMs = if (audioToPlay != null) lastState.positionMs else 0L

                        if (audioToPlay == null) {
                            audioToPlay = playingQueue.firstOrNull()
                            if (isResuming) {
                                settingsRepository.saveLastPlaybackState(LastPlaybackState(null))
                                Log.w(TAG, "Last audio ID ${lastState.audioId} not found; cleared state and falling back to first song")
                            }
                        }

                        if (audioToPlay != null) {
                            sharedAudioDataSource.setPlayingQueue(playingQueue)
                            playbackController.setRepeatMode(repeatMode)
                            playbackController.setShuffleMode(ShuffleMode.OFF)
                            Log.d(TAG, "Set repeat: $repeatMode, shuffle: OFF for playback")

                            playbackController.initiatePlayback(audioToPlay.uri) // Starts at correct index in queue, plays from 0

                            //For resumption, seek to saved position after player is ready
                            if (resumePositionMs > 0) {
                                if (playbackController.waitUntilReady(3000L)) { // 3s timeout for prepare/seek
                                    withContext(Dispatchers.Main) {
                                        playbackController.seekTo(resumePositionMs)
                                    }
                                    Log.d(TAG, "Seeked to resume position ${resumePositionMs}ms")
                                } else {
                                    Log.w(TAG, "Player not ready in time for resume seek; continuing from start")
                                }
                            }
                        }
                        // If no songs or error, do nothing (widget remains "No song playing" until user adds songs via app)
                    } else {
                        Log.w(TAG, "No device audios available; cannot start playback")
                    }
                } else {
                    playbackController.playPause() // Use controller for consistency
                }
            }
        } catch (e: Exception) {
            // Ignore errors to prevent crashes
            Log.e(TAG, "Error in handlePlayPauseFromWidget: ${e.message}", e)
        }
        updateWidget() // Explicit update after toggle (listeners will handle further changes)
    }

    private fun handleRepeatToggle() {
        val current = exoPlayer.repeatMode
        val next = when (current) {
            Player.REPEAT_MODE_OFF -> {
                serviceScope.launch {
                    settingsRepository.updateRepeatMode(RepeatMode.ALL)
                }
                Player.REPEAT_MODE_ALL
            }
            Player.REPEAT_MODE_ALL -> {
                serviceScope.launch {
                    settingsRepository.updateRepeatMode(RepeatMode.ONE)
                }
                Player.REPEAT_MODE_ONE
            }
            Player.REPEAT_MODE_ONE -> {
                serviceScope.launch {
                    settingsRepository.updateRepeatMode(RepeatMode.OFF)
                }
                Player.REPEAT_MODE_OFF
            }
            else -> Player.REPEAT_MODE_OFF
        }
        exoPlayer.repeatMode = next
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

    /**
     * Save last playback state before cleanup.
     * - Async launch to avoid blocking onDestroy (fire-and-forget; best-effort).
     * - Extracts audio ID from currentMediaItem.mediaId (String to Long).
     * - Saves position only if valid item; clears if none (e.g., stopped cleanly).
     * - Moved serviceScope.cancel() after save launch for execution.
     */
    override fun onDestroy() {
        try {
            //Save last state synchronously before cleanup
            runBlocking {
                val currentItem = exoPlayer.currentMediaItem
                val audioId = currentItem?.mediaId?.toLongOrNull()
                val positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                val queueIds = (0 until exoPlayer.mediaItemCount).mapNotNull { exoPlayer.getMediaItemAt(it).mediaId.toLongOrNull() }
                val state = if (audioId != null && queueIds.isNotEmpty()) LastPlaybackState(audioId, positionMs, queueIds) else LastPlaybackState(null)
                settingsRepository.saveLastPlaybackState(state)
                Log.d(TAG, "Saved last playback state: ID=${state.audioId}, pos=${state.positionMs}ms, queue size=${state.queueIds?.size ?: 0}")
            }

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

    private fun formatDuration(ms: Long): String {
        if (ms < 0) return "00:00"
        val seconds = (ms / 1000) % 60
        val minutes = (ms / 1000) / 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    /**
     * Enhanced for idle state (no song loaded).
     * - If no current media item: Use minimal idle layout (widget_player_idle) with centered play button only.
     *   - Loads layout dynamically; sets play icon and unique pending intent.
     *   - Ignores small widget sizing (single element).
     * - Else: Full layout as before (with song details).
     * - Fallback: If idle layout missing, hide non-play elements in full layout (no centering, but functional).
     * - Ensures play always shows play icon in idle (not playing).
     * - Per-widget uniqueness preserved.
     */
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

            val current = exoPlayer.currentMediaItem
            val isIdle = current == null
            val isPlaying = exoPlayer.isPlaying

            //Declare common vars early (with defaults for idle) to resolve references in fallback call
            val metadata = current?.mediaMetadata
            val currentPositionMs = if (isIdle) 0L else exoPlayer.currentPosition
            val totalDurationMs = if (isIdle) 0L else exoPlayer.duration
            val durationText = if (isIdle) "00:00 / 00:00" else "${formatDuration(currentPositionMs)} / ${formatDuration(totalDurationMs)}"

            // Common IDs (shared across layouts)
            val idPlayPause = resources.getIdentifier("widget_play_pause", "id", packageName)
            val idNext = resources.getIdentifier("widget_next", "id", packageName)
            val idPrev = resources.getIdentifier("widget_prev", "id", packageName)
            val idRepeat = resources.getIdentifier("widget_repeat", "id", packageName)
            val idAlbumArt = resources.getIdentifier("widget_album_art", "id", packageName)
            val idTitle = resources.getIdentifier("widget_title", "id", packageName)
            val idArtist = resources.getIdentifier("widget_artist", "id", packageName)
            val idDuration = resources.getIdentifier("widget_duration", "id", packageName)

            if (isIdle) {
                //Idle state - minimal centered play button
                if (idleLayoutId == 0) {
                    Log.w(TAG, "widget_player_idle layout missing; falling back to full with hides")
                    // Fallback: Use full layout, hide everything except play
                    buildFullWidgetWithHides(RemoteViews(packageName, fullLayoutId), idPlayPause)
                } else {
                    val baseViews = RemoteViews(packageName, idleLayoutId)
                    // Set play icon (always play in idle)
                    val playDrawableId = resources.getIdentifier("ic_play_arrow_24", "drawable", packageName)
                    if (idPlayPause != 0 && playDrawableId != 0) {
                        baseViews.setImageViewResource(idPlayPause, playDrawableId)
                    }
                    // Update per-widget with unique play intent (no other buttons)
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
                    return // Early return for idle
                }
            }

            // Full state (song loaded) - original logic
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

            // Album art handling (circular + default)
            var artBitmap: Bitmap? = null
            try {
                val artUri: Uri? = metadata?.artworkUri
                if (artUri != null) {
                    contentResolver.openInputStream(artUri)?.use { input ->
                        artBitmap = BitmapFactory.decodeStream(input)
                    }
                }
            } catch (_: Exception) {
            }

            if (artBitmap == null) {
                val defaultId = resources.getIdentifier("ic_music_note_24", "drawable", packageName)
                if (defaultId != 0) {
                    artBitmap = BitmapFactory.decodeResource(resources, defaultId)
                } else {
                    // Fallback to launcher icon if music note not found
                    val fallbackId = resources.getIdentifier("ic_launcher_round", "mipmap", packageName)
                    if (fallbackId != 0) artBitmap = BitmapFactory.decodeResource(resources, fallbackId)
                }
            }
            if (artBitmap != null && idAlbumArt != 0) {
                val circularBitmap = createCircularBitmap(artBitmap)
                baseViews.setImageViewBitmap(idAlbumArt, circularBitmap)
            }

            // Fixed icon tints (no palette)
            if (idPrev != 0) baseViews.setInt(idPrev, "setColorFilter", Color.WHITE)
            if (idNext != 0) baseViews.setInt(idNext, "setColorFilter", Color.WHITE)

            // Repeat icon
            val repeatMode = exoPlayer.repeatMode
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

            // For each widget id create a copy of RemoteViews and set per-widget unique PendingIntents
            ids.forEach { appWidgetId ->
                try {
                    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                    val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                    val viewsCopy = RemoteViews(baseViews)

                    // Unique request codes and unique intent data per widget id
                    val playReq = ACTION_WIDGET_PLAY_PAUSE.hashCode() xor appWidgetId
                    val nextReq = ACTION_WIDGET_NEXT.hashCode() xor appWidgetId
                    val prevReq = ACTION_WIDGET_PREV.hashCode() xor appWidgetId
                    val repeatReq = ACTION_WIDGET_REPEAT.hashCode() xor appWidgetId

                    if (idPlayPause != 0) viewsCopy.setOnClickPendingIntent(idPlayPause, perWidgetBroadcast(ACTION_WIDGET_PLAY_PAUSE, playReq, appWidgetId))
                    if (idNext != 0) viewsCopy.setOnClickPendingIntent(idNext, perWidgetBroadcast(ACTION_WIDGET_NEXT, nextReq, appWidgetId))
                    if (idPrev != 0) viewsCopy.setOnClickPendingIntent(idPrev, perWidgetBroadcast(ACTION_WIDGET_PREV, prevReq, appWidgetId))
                    if (idRepeat != 0) viewsCopy.setOnClickPendingIntent(idRepeat, perWidgetBroadcast(ACTION_WIDGET_REPEAT, repeatReq, appWidgetId))

                    // Also make the album/title open app unique per widget
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

                    if (minWidth < 250) { // Small widget, hide prev and artist
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

    /**
     * Simplified fallback helper for idle state if widget_player_idle missing.
     * - Now only takes baseViews and idPlayPause (unused params removed - hides all non-play elements).
     * - Hides all non-play elements in full layout (no centering, but clean).
     * - Sets play icon and per-widget play intent.
     */
    @SuppressLint("UseKtx")
    @RequiresApi(Build.VERSION_CODES.P)
    private fun buildFullWidgetWithHides(baseViews: RemoteViews, idPlayPause: Int) {
        // Hide non-play elements
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

        // Set play icon (always play in fallback idle)
        val playDrawableId = resources.getIdentifier("ic_play_arrow_24", "drawable", packageName)
        if (idPlayPause != 0 && playDrawableId != 0) {
            baseViews.setImageViewResource(idPlayPause, playDrawableId)
        }

        // Per-widget play intent (as in idle)
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

    /**
     * Build per-widget unique PendingIntent that targets the provider broadcast receiver in the app package.
     */
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

    /**
     * Safe getString that uses runtime resource lookup for app_name in case R from this module differs.
     */
    private fun getStringSafe(name: String): String {
        val resId = resources.getIdentifier(name, "string", packageName)
        return if (resId != 0) resources.getString(resId) else "Music Player"
    }
}