package com.engfred.musicplayer.feature_player.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.lang.Exception
import javax.inject.Inject

const val MUSIC_NOTIFICATION_CHANNEL_ID = "music_playback_channel"
const val MUSIC_NOTIFICATION_ID = 101

@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var exoPlayer: ExoPlayer

    @Inject
    lateinit var musicNotificationProvider: MusicNotificationProvider

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        // NOTE: we use string actions that match your app module's PlayerWidgetProvider actions.
        // Keep these strings identical to the ones in the provider class in the app module.
        const val ACTION_WIDGET_PLAY_PAUSE = "com.engfred.musicplayer.ACTION_WIDGET_PLAY_PAUSE"
        const val ACTION_WIDGET_NEXT = "com.engfred.musicplayer.ACTION_WIDGET_NEXT"
        const val ACTION_WIDGET_PREV = "com.engfred.musicplayer.ACTION_WIDGET_PREV"

        // Internal: request widget refresh
        const val ACTION_REFRESH_WIDGET = "com.engfred.musicplayer.ACTION_REFRESH_WIDGET"

        // Fully qualified class name of the widget provider (in the app module)
        const val WIDGET_PROVIDER_CLASS = "com.engfred.musicplayer.widget.PlayerWidgetProvider"
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
            exoPlayer.addListener(object : androidx.media3.common.Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    updateWidget()
                }

                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    updateWidget()
                }

                override fun onPositionDiscontinuity(reason: Int) {
                    super.onPositionDiscontinuity(reason)
                    updateWidget()
                }
            })

        } catch (e: Exception) {
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                ACTION_WIDGET_PLAY_PAUSE -> {
                    handlePlayPauseFromWidget()
                    updateWidget()
                }
                ACTION_WIDGET_NEXT -> {
                    try { exoPlayer.seekToNextMediaItem() } catch (_: Exception) {}
                    updateWidget()
                }
                ACTION_WIDGET_PREV -> {
                    try { exoPlayer.seekToPreviousMediaItem() } catch (_: Exception) {}
                    updateWidget()
                }
                ACTION_REFRESH_WIDGET -> updateWidget()
            }
        } catch (e: Exception) {
            // ignore
        }

        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun handlePlayPauseFromWidget() {
        try {
            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
        } catch (e: Exception) {
        }
    }

    override fun onDestroy() {
        try {
            serviceScope.cancel()

            mediaSession?.run {
                exoPlayer.release()
                release()
                mediaSession = null
            }
        } catch (e: Exception) {
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

    /**
     * Update widget UI without having a compile-time dependency on the app module.
     * Use resource name lookups (resources.getIdentifier) and target the widget via
     * ComponentName(packageName, WIDGET_PROVIDER_CLASS).
     */
    private fun updateWidget() {
        try {
            // ComponentName pointing to provider in app module (string avoids compile-time dependency)
            val providerComponent = ComponentName(packageName, WIDGET_PROVIDER_CLASS)
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val ids = appWidgetManager.getAppWidgetIds(providerComponent)
            if (ids.isEmpty()) return

            // Dynamically find layout id in the app package
            val layoutId = resources.getIdentifier("widget_player", "layout", packageName)
            if (layoutId == 0) return // layout not found in app package

            val views = RemoteViews(packageName, layoutId)

            // Current metadata & playback state
            val isPlaying = exoPlayer.isPlaying
            val current = exoPlayer.currentMediaItem
            val metadata: MediaMetadata? = current?.mediaMetadata

            val title = metadata?.title?.toString() ?: getStringSafe("app_name")
            val artist = metadata?.artist?.toString() ?: ""

            // Dynamically resolve ids used inside the widget layout
            val idTitle = resources.getIdentifier("widget_title", "id", packageName)
            val idArtist = resources.getIdentifier("widget_artist", "id", packageName)
            val idPlayPause = resources.getIdentifier("widget_play_pause", "id", packageName)
            val idNext = resources.getIdentifier("widget_next", "id", packageName)
            val idPrev = resources.getIdentifier("widget_prev", "id", packageName)
            val idAlbumArt = resources.getIdentifier("widget_album_art", "id", packageName)

            if (idTitle != 0) views.setTextViewText(idTitle, title)
            if (idArtist != 0) views.setTextViewText(idArtist, artist)

            // Play/pause drawable resource names in the app module
            val playDrawableResName = if (isPlaying) "ic_pause_24" else "ic_play_arrow_24"
            val playDrawableId = resources.getIdentifier(playDrawableResName, "drawable", packageName)
            if (idPlayPause != 0 && playDrawableId != 0) {
                views.setImageViewResource(idPlayPause, playDrawableId)
            }

            // Try artwork Uri from metadata first (some launchers support setImageViewUri)
            var setArt = false
            try {
                val artUri: Uri? = metadata?.artworkUri
                if (artUri != null && idAlbumArt != 0) {
                    views.setImageViewUri(idAlbumArt, artUri)
                    setArt = true
                }
            } catch (_: Exception) {
            }

            // fallback app icon if artwork not set
            if (!setArt && idAlbumArt != 0) {
                // Try to resolve mipmap/ic_launcher or fallback to android icon
                val mipmapId = resources.getIdentifier("ic_launcher", "mipmap", packageName)
                val fallback = if (mipmapId != 0) {
                    BitmapFactory.decodeResource(resources, mipmapId)
                } else {
                    BitmapFactory.decodeResource(resources, android.R.mipmap.sym_def_app_icon)
                }
                views.setImageViewBitmap(idAlbumArt, fallback)
            }

            // Build PendingIntents that target the provider broadcast receiver in the app package.
            fun pendingBroadcastFor(action: String, requestCode: Int): PendingIntent {
                val i = Intent().apply {
                    component = ComponentName(packageName, WIDGET_PROVIDER_CLASS)
                    this.action = action
                }
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
                return PendingIntent.getBroadcast(this, requestCode, i, flags)
            }

            if (idPlayPause != 0) views.setOnClickPendingIntent(idPlayPause, pendingBroadcastFor(ACTION_WIDGET_PLAY_PAUSE, ACTION_WIDGET_PLAY_PAUSE.hashCode()))
            if (idNext != 0) views.setOnClickPendingIntent(idNext, pendingBroadcastFor(ACTION_WIDGET_NEXT, ACTION_WIDGET_NEXT.hashCode()))
            if (idPrev != 0) views.setOnClickPendingIntent(idPrev, pendingBroadcastFor(ACTION_WIDGET_PREV, ACTION_WIDGET_PREV.hashCode()))

            // Update the widget(s)
            appWidgetManager.updateAppWidget(ids, views)
        } catch (e: Exception) {
            // ignore widget update errors so service won't crash
        }
    }

    /**
     * Safe getString that uses runtime resource lookup for app_name in case R from this module differs.
     */
    private fun getStringSafe(name: String): String {
        val resId = resources.getIdentifier(name, "string", packageName)
        return if (resId != 0) resources.getString(resId) else "Music Player"
    }
}
