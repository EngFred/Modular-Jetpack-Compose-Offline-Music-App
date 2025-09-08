package com.engfred.musicplayer.feature_player.data.repository
import android.content.Context
import android.media.AudioManager
import android.media.audiofx.Visualizer
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.repository.PlaybackController
import com.engfred.musicplayer.core.domain.repository.PlaybackState
import com.engfred.musicplayer.core.domain.repository.RepeatMode
import com.engfred.musicplayer.core.domain.repository.ShuffleMode
import com.engfred.musicplayer.core.mapper.AudioFileMapper
import com.engfred.musicplayer.core.domain.usecases.PermissionHandlerUseCase
import com.engfred.musicplayer.core.domain.repository.PlaylistRepository
import com.engfred.musicplayer.feature_player.data.repository.controller.ControllerCallback
import com.engfred.musicplayer.feature_player.data.repository.controller.MediaControllerBuilder
import com.engfred.musicplayer.feature_player.data.repository.controller.PlaybackProgressTracker
import com.engfred.musicplayer.feature_player.data.repository.controller.PlaybackStateUpdater
import com.engfred.musicplayer.feature_player.data.repository.controller.QueueManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
private const val TAG = "PlayerControllerImpl"
@UnstableApi
@Singleton
class PlaybackControllerImpl @Inject constructor(
    private val sharedAudioDataSource: SharedAudioDataSource,
    private val audioFileMapper: AudioFileMapper,
    private val permissionHandlerUseCase: PermissionHandlerUseCase,
    private val playlistRepository: PlaylistRepository,
    @ApplicationContext private val context: Context,
    private val sessionToken: SessionToken,
    private val exoPlayer: ExoPlayer
) : PlaybackController {
    private val mediaController = MutableStateFlow<MediaController?>(null)
    private val _playbackState = MutableStateFlow(PlaybackState())
    override fun getPlaybackState(): Flow<PlaybackState> = _playbackState.asStateFlow()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var attachedController: MediaController? = null
    private var intendedRepeatMode: RepeatMode = RepeatMode.OFF
    private var intendedShuffleMode: ShuffleMode = ShuffleMode.OFF
    private val pendingPlayNextMediaId = MutableStateFlow<String?>(null)
    // Grok: Visualizer for real-time audio visualization
    private var visualizer: Visualizer? = null
    private val beatDetector = BeatDetector()
    // Helpers
    private val stateUpdater = PlaybackStateUpdater(_playbackState, mediaController, sharedAudioDataSource, audioFileMapper)
    private val progressTracker = PlaybackProgressTracker(mediaController, stateUpdater)
    // Grok: Pass beatDetector to ControllerCallback for resetting on song changes
    private val controllerCallback = ControllerCallback(repositoryScope, playlistRepository, progressTracker.currentAudioFilePlaybackProgress, stateUpdater, progressTracker, pendingPlayNextMediaId, sharedAudioDataSource, _playbackState, beatDetector)
    private val mediaControllerBuilder = MediaControllerBuilder(context, sessionToken, mediaController, _playbackState)
    private val queueManager = QueueManager(
        sharedAudioDataSource,
        audioFileMapper,
        permissionHandlerUseCase,
        context,
        mediaController,
        _playbackState,
        stateUpdater,
        progressTracker,
        setRepeatCallback = ::setRepeatMode,
        setShuffleCallback = ::setShuffleMode,
        pendingPlayNextMediaId = pendingPlayNextMediaId
    )
    init {
        Log.d(TAG, "Initializing PlayerControllerImpl")
        repositoryScope.launch {
            mediaControllerBuilder.buildAndConnectController()
        }
        repositoryScope.launch {
            mediaController.collectLatest { newController ->
                withContext(Dispatchers.Main) {
                    attachedController?.removeListener(controllerCallback)
                    if (newController != null) {
                        newController.addListener(controllerCallback)
                        attachedController = newController
                        Log.d(TAG, "PlayerControllerImpl received and attached to shared MediaController.")
                        setRepeatMode(intendedRepeatMode)
                        setShuffleMode(intendedShuffleMode)
                        stateUpdater.updatePlaybackState()
                        progressTracker.updateCurrentAudioFilePlaybackProgress(newController)
                        // Grok: Set up Visualizer with ExoPlayer's audio session ID
                        try {
                            val audioSessionId = exoPlayer.audioSessionId
                            if (audioSessionId != 0) {
                                visualizer = Visualizer(audioSessionId).apply {
                                    captureSize = Visualizer.getCaptureSizeRange()[1]
                                    setDataCaptureListener(
                                        object : Visualizer.OnDataCaptureListener {
                                            override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                                                // Not using waveform data
                                            }
                                            override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                                                fft?.let {
                                                    val intensity = beatDetector.detectBeat(it, samplingRate)
                                                    val bpm = beatDetector.getEstimatedBpm() // Grok: Use getEstimatedBpm
                                                    _playbackState.update { current ->
                                                        current.copy(
                                                            bassIntensity = intensity,
                                                            estimatedBpm = bpm
                                                        )
                                                    }
                                                    Log.v(TAG, "Beat intensity: $intensity, Estimated BPM: $bpm")
                                                }
                                            }
                                        },
                                        Visualizer.getMaxCaptureRate() / 4,
                                        false, // No waveform
                                        true // Enable FFT
                                    )
                                }
                                Log.d(TAG, "Visualizer set up successfully with audio session ID: $audioSessionId")
                            } else {
                                Log.w(TAG, "Invalid audio session ID (0) for Visualizer setup.")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to set up Visualizer: ${e.message}", e)
                        }
                    } else {
                        attachedController = null
                        Log.w(TAG, "PlayerControllerImpl received null MediaController.")
                        _playbackState.update { PlaybackState() }
                        controllerCallback.resetTracking()
                        progressTracker.resetProgress()
                        visualizer?.enabled = false
                        visualizer?.release()
                        visualizer = null
                        beatDetector.reset()
                    }
                }
            }
        }
        repositoryScope.launch {
            progressTracker.startPlaybackPositionUpdates()
        }
        repositoryScope.launch {
            _playbackState.map { it.isPlaying }.distinctUntilChanged().collect { isPlaying ->
                if (isPlaying) {
                    visualizer?.enabled = true
                    Log.d(TAG, "Visualizer enabled during playback.")
                } else {
                    visualizer?.enabled = false
                    _playbackState.update { it.copy(bassIntensity = 0f, estimatedBpm = 120f) }
                    beatDetector.reset()
                    Log.d(TAG, "Visualizer disabled when paused/stopped.")
                }
            }
        }
    }
    override suspend fun initiatePlayback(initialAudioFileUri: android.net.Uri) {
        queueManager.initiatePlayback(initialAudioFileUri, intendedRepeatMode, intendedShuffleMode)
    }
    override suspend fun initiateShufflePlayback(playingQueue: List<AudioFile>) {
        if (playingQueue.isEmpty()) {
            Log.w(TAG, "Cannot initiate shuffle playback: empty queue.")
            return
        }
        setShuffleMode(ShuffleMode.ON)
        val randomAudio = playingQueue.shuffled().first()
        sharedAudioDataSource.setPlayingQueue(playingQueue)
        initiatePlayback(randomAudio.uri)
    }
    override suspend fun playPause() {
        withContext(Dispatchers.Main) {
            mediaController.value?.run {
                if (isPlaying) pause() else play()
            } ?: Log.w(TAG, "MediaController not set when trying to play/pause.")
        }
    }
    override suspend fun skipToNext() {
        withContext(Dispatchers.Main) {
            mediaController.value?.seekToNextMediaItem() ?: Log.w(TAG, "MediaController not set when trying to skip next.")
        }
    }
    override suspend fun skipToPrevious() {
        withContext(Dispatchers.Main) {
            mediaController.value?.seekToPreviousMediaItem() ?: Log.w(TAG, "MediaController not set when trying to skip previous.")
        }
    }
    override suspend fun seekTo(positionMs: Long) {
        withContext(Dispatchers.Main) {
            mediaController.value?.let { controller ->
                controller.seekTo(positionMs)
                stateUpdater.updatePlaybackState()
                progressTracker.updateCurrentAudioFilePlaybackProgress(controller)
            } ?: Log.w(TAG, "MediaController not set when trying to seek.")
        }
    }
    override suspend fun setRepeatMode(mode: RepeatMode) {
        withContext(Dispatchers.Main) {
            intendedRepeatMode = mode
            mediaController.value?.let { controller ->
                controller.repeatMode = when (mode) {
                    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                }
                Log.d(TAG, "Set repeat mode to $mode")
            } ?: Log.w(TAG, "MediaController not set when trying to set repeat mode. Stored $mode for later.")
        }
    }
    override suspend fun setShuffleMode(mode: ShuffleMode) {
        withContext(Dispatchers.Main) {
            intendedShuffleMode = mode
            mediaController.value?.let { controller ->
                controller.shuffleModeEnabled = (mode == ShuffleMode.ON)
                Log.d(TAG, "Set shuffle mode to $mode")
            } ?: Log.w(TAG, "MediaController not set when trying to set shuffle mode. Stored $mode for later.")
        }
    }
    override suspend fun addAudioToQueueNext(audioFile: AudioFile) {
        queueManager.addAudioToQueueNext(audioFile)
    }
    override suspend fun releasePlayer() {
        val controllerToRelease = mediaController.value
        repositoryScope.cancel()
        withContext(Dispatchers.Main) {
            attachedController?.removeListener(controllerCallback)
            attachedController = null
            Log.d(TAG, "PlayerControllerImpl resources released and listener removed.")
            controllerCallback.resetTracking()
            progressTracker.resetProgress()
            try {
                controllerToRelease?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing MediaController: ${e.message}")
            } finally {
                mediaController.value = null
            }
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null
            beatDetector.reset()
            Log.d(TAG, "Visualizer released.")
        }
    }
    override fun clearPlaybackError() {
        _playbackState.update { it.copy(error = null) }
    }
    override suspend fun onAudioFileRemoved(deletedAudioFile: AudioFile) {
        queueManager.onAudioFileRemoved(deletedAudioFile)
    }
    override suspend fun removeFromQueue(audioFile: AudioFile) {
        queueManager.removeFromQueue(audioFile)
    }
    // Grok: Helper class for beat detection
    // Grok: Changed visibility to internal to allow access from ControllerCallback without exposing publicly
    class BeatDetector {
        private var energyHistory = FloatArray(20) { 0f } // Store last 20 energy samples
        private var historyIndex = 0
        private var lastEnergy = 0f
        private var smoothedIntensity = 0f
        private var lastBeatTime = 0L
        private var estimatedBpm = 120f // Default BPM for animation timing
        // Grok: Added for more stable BPM estimation using median of recent intervals
        private val beatIntervals: MutableList<Long> = mutableListOf()
        // Grok: Added for normalizing amplitude to make intensity song-adaptive
        private var maxAmp = 0f
        fun detectBeat(fft: ByteArray, samplingRate: Int): Float {
            val n = fft.size
            val numBins = n / 2
            var energySum = 0f
            var ampSum = 0f // Grok: Added to compute average amplitude for intensity (better for subwoofer scaling)
            val beatFreqMin = 50f // Focus on 50-150 Hz for kick drums/bass hits
            val beatFreqMax = 150f
            val freqPerBin = samplingRate.toFloat() / n
            val beatBinStart = (beatFreqMin / freqPerBin).toInt().coerceAtLeast(1)
            val beatBinEnd = (beatFreqMax / freqPerBin).toInt().coerceAtMost(numBins - 1)
            val numBeatBins = (beatBinEnd - beatBinStart + 1).coerceAtLeast(1)
            // Calculate energy and amplitude in beat frequency range
            for (k in beatBinStart..beatBinEnd) {
                val real = fft[2 * k].toFloat()
                val imag = fft[2 * k + 1].toFloat()
                val magnitude = sqrt(real * real + imag * imag)
                ampSum += magnitude
                energySum += magnitude * magnitude // Use energy (magnitude squared)
            }
            val currentEnergy = energySum / numBeatBins
            val currentAmp = ampSum / numBeatBins
            // Compute avg and variance from past history (before updating)
            val avgEnergy = energyHistory.sum() / energyHistory.size
            val variance = energyHistory.map { (it - avgEnergy) * (it - avgEnergy) }.sum() / energyHistory.size
            // Grok: Improved threshold using standard beat detection formula for better sensitivity
            val c = -0.0025714f * variance + 1.5142857f
            val dynamicThreshold = c * avgEnergy
            // Grok: Detect beat with improved condition (removed strict >1.5*last to rely on dynamic C)
            val isBeat = currentEnergy > dynamicThreshold
            // Update history after detection
            energyHistory[historyIndex] = currentEnergy
            historyIndex = (historyIndex + 1) % energyHistory.size
            lastEnergy = currentEnergy
            // Update BPM estimate based on beat intervals
            if (isBeat) {
                val currentTime = System.currentTimeMillis()
                if (lastBeatTime > 0) {
                    val intervalMs = currentTime - lastBeatTime
                    if (intervalMs in 200..2000) { // Valid BPM range: 30-300
                        beatIntervals.add(intervalMs)
                        if (beatIntervals.size > 20) {
                            beatIntervals.removeAt(0)
                        }
                        // Grok: Use median interval for more stable BPM (less affected by outliers)
                        val sortedIntervals = beatIntervals.sorted()
                        val medianInterval = if (sortedIntervals.isNotEmpty()) {
                            if (sortedIntervals.size % 2 == 0) {
                                (sortedIntervals[sortedIntervals.size / 2 - 1] + sortedIntervals[sortedIntervals.size / 2]) / 2.0
                            } else {
                                sortedIntervals[sortedIntervals.size / 2].toDouble()
                            }
                        } else {
                            500.0 // Default interval if none
                        }
                        estimatedBpm = 60000f / medianInterval.toFloat()
                    }
                }
                lastBeatTime = currentTime
            }
            // Grok: Normalize and smooth amplitude for intensity (mimics subwoofer excursion proportional to amplitude)
            maxAmp *= 0.995f // Slow decay to adapt over time
            maxAmp = max(maxAmp, currentAmp)
            val normalizedAmp = currentAmp / maxAmp.coerceAtLeast(1f)
            smoothedIntensity = 0.6f * smoothedIntensity + 0.4f * normalizedAmp // Adjusted EMA for responsiveness
            return smoothedIntensity.coerceIn(0f, 1f)
        }
        fun getEstimatedBpm(): Float = estimatedBpm
        fun reset() {
            energyHistory.fill(0f)
            historyIndex = 0
            lastEnergy = 0f
            smoothedIntensity = 0f
            lastBeatTime = 0L
            estimatedBpm = 120f
            // Grok: Clear intervals and reset maxAmp on reset
            beatIntervals.clear()
            maxAmp = 0f
        }
    }
}