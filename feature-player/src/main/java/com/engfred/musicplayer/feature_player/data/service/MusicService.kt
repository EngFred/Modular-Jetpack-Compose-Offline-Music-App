package com.engfred.musicplayer.feature_player.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
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

import com.engfred.musicplayer.core.data.session.AudioSessionIdPublisher

const val MUSIC_NOTIFICATION_CHANNEL_ID = "music_playback_channel"
const val MUSIC_NOTIFICATION_ID = 101

@UnstableApi
@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject
    lateinit var exoPlayer: ExoPlayer

    @Inject
    lateinit var musicNotificationProvider: MusicNotificationProvider

    @Inject
    lateinit var audioSessionIdPublisher: AudioSessionIdPublisher

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val playerListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            super.onAudioSessionIdChanged(audioSessionId)
            Log.d("MusicService", "ExoPlayer audioSessionId changed: $audioSessionId")
            audioSessionIdPublisher.updateAudioSessionId(audioSessionId)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val startTime = System.currentTimeMillis()
        Log.d("MusicService", "onCreate started")

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
                Log.d("MusicService", "Foreground started: ${System.currentTimeMillis() - startTime}ms")
            } catch (e: Exception) {
                Log.e("MusicService", "Failed to start foreground service: ${e.message}")
                stopSelf()
                return
            }
        }

        try {
            val initStart = System.currentTimeMillis()

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            exoPlayer.setAudioAttributes(audioAttributes, true)
            exoPlayer.setHandleAudioBecomingNoisy(true)
            exoPlayer.addListener(playerListener) // Keep this listener!
            Log.d("MusicService", "ExoPlayer configured: ${System.currentTimeMillis() - initStart}ms")

            mediaSession = MediaSession.Builder(this, exoPlayer)
                .build()
            setMediaNotificationProvider(musicNotificationProvider)
            Log.d("MusicService", "MediaSession created: ${System.currentTimeMillis() - initStart}ms")

        } catch (e: Exception) {
            Log.e("MusicService", "Initialization failed: ${e.message}", e)
            stopSelf()
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
        Log.d("MusicService", "onDestroy called")
        try {
            serviceScope.cancel()
            exoPlayer.removeListener(playerListener)
            audioSessionIdPublisher.updateAudioSessionId(C.AUDIO_SESSION_ID_UNSET)

            mediaSession?.run {
                exoPlayer.release()
                release()
                mediaSession = null
            }
            Log.d("MusicService", "ExoPlayer and MediaSession released")
        } catch (e: Exception) {
            Log.e("MusicService", "Error during onDestroy: ${e.message}", e)
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