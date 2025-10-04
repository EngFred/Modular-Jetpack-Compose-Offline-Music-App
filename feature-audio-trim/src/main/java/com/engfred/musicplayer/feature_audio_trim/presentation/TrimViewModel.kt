package com.engfred.musicplayer.feature_audio_trim.presentation

import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.core.common.Resource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.repository.LibraryRepository
import com.engfred.musicplayer.feature_audio_trim.domain.model.TrimResult
import com.engfred.musicplayer.feature_audio_trim.domain.usecase.TrimAudioUseCase
import com.engfred.musicplayer.feature_audio_trim.utils.PreviewPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "TrimViewModel"

data class TrimUiState(
    val audioFile: AudioFile? = null,
    val isLoading: Boolean = false,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L,
    val isTrimming: Boolean = false,
    val trimResult: TrimResult? = null,
    val error: String? = null
)

@HiltViewModel
class TrimViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val libraryRepository: LibraryRepository,
    private val trimAudioUseCase: TrimAudioUseCase,
    private val previewPlayerManager: PreviewPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrimUiState())
    val uiState: StateFlow<TrimUiState> = _uiState.asStateFlow()

    // Expose preview playback state to UI
    val isPreviewPlaying: StateFlow<Boolean> = previewPlayerManager.isPlaying
    val previewPositionMs: StateFlow<Long> = previewPlayerManager.positionMs

    private companion object {
        private const val MAX_FILE_SIZE_BYTES = 10_000_000L
        private const val MIN_TRIM_DURATION_MS = 30_000L
    }

    init {
        // Collect preview errors and update UI state (filter transients for UX)
        viewModelScope.launch {
            previewPlayerManager.error
                .filterNotNull()
                .filter { !it.contains("interrupted") }  // Suppress auto-recoverable transients
                .collect { error ->
                    Log.e(TAG, "Preview player error: $error")
                    _uiState.value = _uiState.value.copy(error = error)
                }
        }

        val audioUriString = savedStateHandle.get<String>("audioUri")
        if (audioUriString.isNullOrEmpty()) {
            Log.w(TAG, "No audio URI provided in SavedStateHandle")
            _uiState.value = _uiState.value.copy(error = "No audio URI provided")
        } else {
            loadAudioFile(audioUriString)
        }
    }

    private fun loadAudioFile(audioUriString: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val uri = audioUriString.toUri()
                val result = libraryRepository.getAudioFileByUri(uri)
                _uiState.value = _uiState.value.copy(isLoading = false)
                when (result) {
                    is Resource.Success -> {
                        val audioFile = result.data
                        if (audioFile != null && audioFile.duration > 0L) {  // Add duration check
                            val isTooLarge = (audioFile.size ?: 0L) > MAX_FILE_SIZE_BYTES
                            val newError = if (isTooLarge) {
                                "File too large. Maximum size is 10 MB."
                            } else null
                            _uiState.value = _uiState.value.copy(
                                audioFile = audioFile,
                                endTimeMs = audioFile.duration.coerceAtLeast(0L),  // Allow full preview even if <30s
                                error = newError
                            )
                            Log.d(TAG, "Audio file loaded successfully: ${audioFile.title}, duration: ${audioFile.duration}ms")
                        } else {
                            val errorMsg = if (audioFile?.duration == 0L) "Audio file has invalid duration" else "Invalid audio file"
                            Log.e(TAG, errorMsg)
                            _uiState.value = _uiState.value.copy(error = errorMsg)
                        }
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "Error loading audio file: ${result.message}")
                        _uiState.value = _uiState.value.copy(error = result.message ?: "Failed to load audio file")
                    }
                    else -> {
                        // Handle unexpected Resource states if any
                        val errorMsg = "Unexpected error loading audio file"
                        Log.e(TAG, errorMsg)
                        _uiState.value = _uiState.value.copy(error = errorMsg)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadAudioFile", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load audio file: ${e.message}"
                )
            }
        }
    }

    fun updateStartTime(ms: Long) {
        val current = _uiState.value
        val newStart = ms.coerceIn(0L, (current.endTimeMs - 1L).coerceAtLeast(0L))  // Allow 1ms min for preview
        if (newStart != current.startTimeMs) {
            _uiState.value = current.copy(startTimeMs = newStart)
            Log.d(TAG, "Updated start time to ${newStart}ms")
            // Clear transient errors on valid change
            if (current.error?.contains("valid preview range") == true) {
                clearError()
            }
            // Auto-start preview for the new range
            viewModelScope.launch {
                startPreview()
            }
        }
    }

    fun updateEndTime(ms: Long) {
        val current = _uiState.value
        val newEnd = ms.coerceIn((current.startTimeMs + 1L), current.audioFile?.duration ?: Long.MAX_VALUE)
        if (newEnd != current.endTimeMs) {
            _uiState.value = current.copy(endTimeMs = newEnd)
            Log.d(TAG, "Updated end time to ${newEnd}ms")
            // Clear transient errors on valid change
            if (current.error?.contains("valid preview range") == true) {
                clearError()
            }
            // Auto-start preview for the new range
            viewModelScope.launch {
                startPreview()
            }
        }
    }

    // ===== Preview controls =====
    fun togglePreview() {
        viewModelScope.launch {
            try {
                val playing = previewPlayerManager.isPlaying.value
                if (playing) {
                    previewPlayerManager.pause()
                    Log.d(TAG, "Preview paused")
                } else {
                    startPreview()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling preview", e)
                _uiState.value = _uiState.value.copy(error = "Preview failed. Please try again.")
            }
        }
    }

    private fun startPreview() {
        val state = _uiState.value
        val audioFile = state.audioFile ?: run {
            Log.w(TAG, "Cannot start preview: no audio file")
            _uiState.value = state.copy(error = "No audio file loaded")
            return
        }
        if (audioFile.uri == null) {
            Log.w(TAG, "Cannot start preview: audio URI missing")
            _uiState.value = state.copy(error = "Audio URI is missing")
            return
        }
        if (audioFile.duration <= 0L) {
            Log.w(TAG, "Cannot start preview: invalid audio duration")
            _uiState.value = state.copy(error = "Invalid audio duration")
            return
        }

        if (state.startTimeMs >= state.endTimeMs) {
            Log.w(TAG, "Cannot start preview: invalid trim range")
            _uiState.value = state.copy(error = "Please set a valid preview range.")
            return
        }

        // Clear preview-specific errors before starting
        if (state.error?.contains("Playback") == true || state.error?.contains("valid preview") == true) {
            clearError()
        }

        previewPlayerManager.playClip(
            uri = audioFile.uri,
            startMs = state.startTimeMs,
            endMs = state.endTimeMs
        )
        Log.d(TAG, "Preview started/resumed for clip ${state.startTimeMs}ms - ${state.endTimeMs}ms")
    }

    fun stopPreview() {
        try {
            previewPlayerManager.stop()
            Log.d(TAG, "Preview stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping preview", e)
        }
    }

    fun seekPreviewToStart() {
        try {
            previewPlayerManager.seekToStartOfClip()
            Log.d(TAG, "Preview seeked to clip start")
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking preview", e)
            _uiState.value = _uiState.value.copy(error = "Seek failed. Please try playing again.")
        }
    }

    // ===== Trim functions =====
    private var trimJob: Job? = null

    fun trimAudio() {
        val current = _uiState.value
        val audioFile = current.audioFile ?: run {
            val errorMsg = "No audio file loaded"
            Log.w(TAG, errorMsg)
            _uiState.value = current.copy(error = errorMsg)
            return
        }
        if (audioFile.uri == null) {
            val errorMsg = "Audio URI is missing"
            Log.w(TAG, errorMsg)
            _uiState.value = current.copy(error = errorMsg)
            return
        }
        if (current.startTimeMs >= current.endTimeMs) {
            val errorMsg = "Invalid trim range"
            Log.w(TAG, errorMsg)
            _uiState.value = current.copy(error = errorMsg)
            return
        }

        val trimDurationMs = current.endTimeMs - current.startTimeMs
        if (trimDurationMs < MIN_TRIM_DURATION_MS) {
            val errorMsg = "Trim too short (min 30 seconds)"
            Log.w(TAG, errorMsg)
            _uiState.value = current.copy(error = errorMsg)
            return
        }
        if (current.error != null && !current.error!!.contains("File too large")) {  // Allow trim if only size error
            Log.w(TAG, "Trim aborted due to existing error: ${current.error}")
            return
        }

        // Stop any ongoing preview before trimming
        stopPreview()

        _uiState.value = current.copy(isTrimming = true, error = null, trimResult = null)
        Log.d(TAG, "Starting trim operation for ${trimDurationMs}ms clip")
        trimJob = viewModelScope.launch {
            try {
                trimAudioUseCase.execute(audioFile, current.startTimeMs, current.endTimeMs)
                    .collect { result ->
                        val state = _uiState.value
                        when (result) {
                            is TrimResult.Success -> {
                                _uiState.value = state.copy(
                                    isTrimming = false,
                                    trimResult = result
                                )
                                trimJob = null
                            }
                            is TrimResult.Error -> {
                                Log.e(TAG, "Trim error: ${result.message}")
                                _uiState.value = state.copy(
                                    isTrimming = false,
                                    error = "Trim failed: ${result.message}",
                                    trimResult = null
                                )
                                trimJob = null
                            }
                            is TrimResult.PermissionDenied -> {
                                Log.w(TAG, "Trim permission denied")
                                _uiState.value = state.copy(
                                    isTrimming = false,
                                    error = "Write permission required to save trimmed file",
                                    trimResult = null
                                )
                                trimJob = null
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during trim collection", e)
                _uiState.value = _uiState.value.copy(
                    isTrimming = false,
                    error = "Trim failed unexpectedly: ${e.message}",
                    trimResult = null
                )
                trimJob = null
            }
        }
    }

    fun cancelTrim() {
        trimJob?.cancel()
        trimJob = null
        val current = _uiState.value
        val errorMsg = "Trimming cancelled by user"
        Log.d(TAG, errorMsg)
        _uiState.value = current.copy(
            isTrimming = false,
            error = errorMsg,
            trimResult = null
        )
    }

    fun clearError() {
        val current = _uiState.value
        _uiState.value = current.copy(error = null, trimResult = null)
        Log.d(TAG, "Error cleared")
    }

    fun resetTrim() {
        val audioFile = _uiState.value.audioFile ?: return
        val currentError = _uiState.value.error // Preserve file size error if present
        _uiState.value = _uiState.value.copy(
            startTimeMs = 0L,
            endTimeMs = audioFile.duration.coerceAtLeast(0L),
            error = if (currentError?.contains("File too large") == true) currentError else null  // Clear non-critical errors
        )
        stopPreview()  // Reset preview on trim reset
        Log.d(TAG, "Trim reset to full duration")
    }

    override fun onCleared() {
        super.onCleared()
        trimJob?.cancel()
        stopPreview() // Safe stop before release
        previewPlayerManager.release()
        Log.d(TAG, "ViewModel cleared; resources released")
    }
}