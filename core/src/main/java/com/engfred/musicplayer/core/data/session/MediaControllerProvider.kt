//import android.content.ComponentName
//import android.content.Context
//import android.content.Intent
//import android.util.Log
//import androidx.annotation.OptIn
//import androidx.core.content.ContextCompat
//import androidx.media3.common.util.UnstableApi
//import androidx.media3.session.MediaController
//import androidx.media3.session.SessionToken
//import dagger.hilt.android.qualifiers.ApplicationContext
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.SupervisorJob
//import kotlinx.coroutines.cancel
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import java.util.concurrent.TimeUnit
//import javax.inject.Inject
//import javax.inject.Singleton
//
//private const val TAG = "MediaControllerProvider"
//
//@Singleton
//@OptIn(UnstableApi::class)
//class MediaControllerProvider @Inject constructor(
//    @ApplicationContext private val context: Context
//) {
//    private val _mediaController = MutableStateFlow<MediaController?>(null)
//    val mediaController: StateFlow<MediaController?> = _mediaController.asStateFlow()
//
//    private val providerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
//    private var isConnecting = false
//    private val musicServiceComponent = ComponentName(
//        context.packageName,
//        "com.engfred.musicplayer.feature_player.data.service.MusicService"
//    )
//
//    init {
//        providerScope.launch {
//            ensureControllerConnected()
//        }
//    }
//
//    suspend fun ensureControllerConnected() {
//        if (_mediaController.value != null || isConnecting) {
//            Log.d(TAG, "Controller already connected or connecting")
//            return
//        }
//        isConnecting = true
//        try {
//            _mediaController.value?.release()
//            _mediaController.value = null
//
//            var attempt = 0
//            val maxAttempts = 3 // Kept as-is; it's robust but not overkill for Android services.
//            val baseDelayMs = 1000L
//
//            while (attempt < maxAttempts && _mediaController.value == null) {
//                Log.d(TAG, "Attempting connection (attempt $attempt)")
//                try {
//                    val serviceIntent = Intent().setComponent(musicServiceComponent)
//                    ContextCompat.startForegroundService(context, serviceIntent)
//
//                    val sessionToken = SessionToken(context, musicServiceComponent)
//                    val controllerFuture = withContext(Dispatchers.Main.immediate) {
//                        MediaController.Builder(context, sessionToken)
//                            .setListener(MainControllerCallback())
//                            .buildAsync()
//                    }
//                    val controller = controllerFuture.get(10, TimeUnit.SECONDS)
//
//                    if (controller != null && controller.isConnected) {
//                        _mediaController.value = controller
//                        isConnecting = false
//                        Log.d(TAG, "Connected to MusicService")
//                        // Removed audioSessionId log (redundant post-equalizer).
//                        return
//                    }
//                    Log.w(TAG, "Controller null or not connected on attempt $attempt")
//                } catch (e: Exception) {
//                    Log.e(TAG, "Connection attempt $attempt failed: ${e.message}", e)
//                }
//                attempt++
//                if (attempt < maxAttempts) delay(baseDelayMs * (1 shl attempt))
//            }
//            Log.e(TAG, "Failed to connect after $maxAttempts attempts")
//            _mediaController.value = null
//            isConnecting = false
//        } catch (e: Exception) {
//            Log.e(TAG, "Unexpected error in ensureControllerConnected: ${e.message}", e)
//            _mediaController.value = null
//            isConnecting = false
//        }
//    }
//
//    private inner class MainControllerCallback : MediaController.Listener {
//        // Removed onCustomCommand (redundant if no custom commands are used).
//
//        override fun onDisconnected(controller: MediaController) {
//            Log.d(TAG, "MediaController disconnected")
//            _mediaController.value = null
//            providerScope.launch { ensureControllerConnected() }
//        }
//    }
//
//    suspend fun release() {
//        Log.d(TAG, "MediaControllerProvider release called.")
//        providerScope.cancel()
//        _mediaController.value?.let { controller ->
//            withContext(Dispatchers.Main.immediate) {
//                controller.release()
//                _mediaController.value = null
//                Log.d(TAG, "MediaController released in release()")
//            }
//        } ?: run {
//            _mediaController.value = null
//        }
//        Log.d(TAG, "MediaControllerProvider released completed.")
//    }
//}