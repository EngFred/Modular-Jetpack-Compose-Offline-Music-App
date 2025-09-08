package com.engfred.musicplayer.feature_player.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent  // Add this import
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
            // Removed initStart timing log.

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            exoPlayer.setAudioAttributes(audioAttributes, true)
            exoPlayer.setHandleAudioBecomingNoisy(true)

            // Create PendingIntent to launch MainActivity when notification is clicked
//            val intent = Intent(this, MainActivity::class.java)
            val intent = Intent().setClassName(this, "${packageName}.MainActivity")
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)

            mediaSession = MediaSession.Builder(this, exoPlayer)
                .setSessionActivity(pendingIntent)  // Add this line
                .build()
            setMediaNotificationProvider(musicNotificationProvider)

        } catch (e: Exception) {
            stopSelf()
        }
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
}