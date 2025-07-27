package com.engfred.musicplayer.core.data.session

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle // Keep Bundle if you use it for other MediaController.Listener methods
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand // Keep if you have other custom commands
import androidx.media3.session.SessionResult // Keep if you have other custom commands
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.Futures // Keep if you have other custom commands
import com.google.common.util.concurrent.ListenableFuture // Keep if you have other custom commands
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MediaControllerProvider"

@Singleton
@OptIn(UnstableApi::class)
class MediaControllerProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _mediaController = MutableStateFlow<MediaController?>(null)
    val mediaController: StateFlow<MediaController?> = _mediaController.asStateFlow()

    private val providerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isConnecting = false
    private val musicServiceComponent = ComponentName(
        context.packageName,
        "com.engfred.musicplayer.feature_player.data.service.MusicService"
    )

    init {
        providerScope.launch {
            ensureControllerConnected()
        }
    }

    suspend fun ensureControllerConnected() {
        if (_mediaController.value != null || isConnecting) {
            Log.d(TAG, "Controller already connected or connecting")
            return
        }
        isConnecting = true
        try {
            _mediaController.value?.release()
            _mediaController.value = null

            var attempt = 0
            val maxAttempts = 3
            val baseDelayMs = 1000L

            while (attempt < maxAttempts && _mediaController.value == null) {
                Log.d(TAG, "Attempting connection (attempt $attempt)")
                try {
                    val serviceIntent = Intent().setComponent(musicServiceComponent)
                    ContextCompat.startForegroundService(context, serviceIntent)

                    val sessionToken = SessionToken(context, musicServiceComponent)
                    val controllerFuture = withContext(Dispatchers.Main.immediate) {
                        MediaController.Builder(context, sessionToken)
                            .setListener(MainControllerCallback()) // Still needed for onDisconnected, other general controller events
                            .buildAsync()
                    }
                    val controller = controllerFuture.get(10, TimeUnit.SECONDS)

                    if (controller != null && controller.isConnected) {
                        _mediaController.value = controller
                        isConnecting = false
                        Log.d(TAG, "Connected to MusicService")
                        Log.d(TAG, "AudioSessionId will be provided by AudioSessionIdPublisher.")
                        return
                    }
                    Log.w(TAG, "Controller null or not connected on attempt $attempt")
                } catch (e: Exception) {
                    Log.e(TAG, "Connection attempt $attempt failed: ${e.message}", e)
                }
                attempt++
                if (attempt < maxAttempts) delay(baseDelayMs * (1 shl attempt))
            }
            Log.e(TAG, "Failed to connect after $maxAttempts attempts")
            _mediaController.value = null
            isConnecting = false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in ensureControllerConnected: ${e.message}", e)
            _mediaController.value = null
            isConnecting = false
        }
    }

    private inner class MainControllerCallback : MediaController.Listener {
        override fun onCustomCommand(
            controller: MediaController,
            command: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            Log.d(TAG, "Received custom command: ${command.customAction}. (Not for audioSessionId)")
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onDisconnected(controller: MediaController) {
            Log.d(TAG, "MediaController disconnected")
            _mediaController.value = null
            // No need to reset _audioSessionId here, as the publisher manages it.
            providerScope.launch { ensureControllerConnected() }
        }
    }

    suspend fun release() {
        Log.d(TAG, "MediaControllerProvider release called.")
        providerScope.cancel()
        _mediaController.value?.let { controller ->
            withContext(Dispatchers.Main.immediate) {
                controller.release()
                _mediaController.value = null
                Log.d(TAG, "MediaController released in release()")
            }
        } ?: run {
            _mediaController.value = null
        }
        // No need to reset _audioSessionId here, as the publisher manages it.
        Log.d(TAG, "MediaControllerProvider released completed.")
    }
}