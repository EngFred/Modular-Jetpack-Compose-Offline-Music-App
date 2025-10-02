package com.engfred.musicplayer.feature_audio_trim.presentation

import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.core.common.Resource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.repository.LibraryRepository
import com.engfred.musicplayer.core.domain.repository.PlaybackController
import com.engfred.musicplayer.feature_audio_trim.domain.model.TrimResult
import com.engfred.musicplayer.feature_audio_trim.domain.usecase.TrimAudioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrimUiState(
    val audioFile: AudioFile? = null,
    val isLoading: Boolean = false,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L, // Will be set to duration after load
    val isTrimming: Boolean = false,
    val trimResult: TrimResult? = null,
    val error: String? = null
)

@HiltViewModel
class TrimViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val libraryRepository: LibraryRepository,
    private val trimAudioUseCase: TrimAudioUseCase,
    private val playbackController: PlaybackController
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrimUiState())
    val uiState: StateFlow<TrimUiState> = _uiState.asStateFlow()

    private var wasMainPlaying = false
    private var trimJob: Job? = null

    init {
        val audioUriString = savedStateHandle.get<String>("audioUri")
        if (audioUriString.isNullOrEmpty()) {
            _uiState.value = _uiState.value.copy(error = "No audio URI provided")
        } else {
            loadAudioFile(audioUriString)
        }
    }

    private fun loadAudioFile(audioUriString: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val uri = audioUriString.toUri()
            val result = libraryRepository.getAudioFileByUri(uri)
            _uiState.value = _uiState.value.copy(isLoading = false)
            if (result is Resource.Success) {
                val audioFile = result.data
                if (audioFile != null && audioFile.uri != null) {
                    val maxSizeBytes = 10_000_000L // 10 MB
                    val isTooLarge = (audioFile.size ?: 0L) > maxSizeBytes
                    _uiState.value = _uiState.value.copy(
                        audioFile = audioFile,
                        endTimeMs = audioFile.duration,
                        error = if (isTooLarge) "File too large. Maximum size is 10 MB." else null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(error = "Invalid audio file")
                }
            } else if (result is Resource.Error) {
                _uiState.value = _uiState.value.copy(error = result.message)
            }
        }
    }

    fun updateStartTime(ms: Long) {
        val current = _uiState.value
        val newStart = ms.coerceIn(0L, current.endTimeMs - 30000L) // Ensure min 30s possible
        _uiState.value = current.copy(startTimeMs = newStart)
    }

    fun updateEndTime(ms: Long) {
        val current = _uiState.value
        val newEnd = ms.coerceIn(current.startTimeMs + 30000L, current.audioFile?.duration ?: 0L) // Ensure min 30s
        _uiState.value = current.copy(endTimeMs = newEnd)
    }

    fun trimAudio() {
        val current = _uiState.value
        val audioFile = current.audioFile ?: run {
            _uiState.value = current.copy(error = "No audio file loaded")
            return
        }
        if (audioFile.uri == null) {
            _uiState.value = current.copy(error = "Audio URI is missing")
            return
        }
        if (current.startTimeMs >= current.endTimeMs) {
            _uiState.value = current.copy(error = "Invalid trim range")
            return
        }

        val trimDurationMs = current.endTimeMs - current.startTimeMs
        if (trimDurationMs < 30000L) {
            _uiState.value = current.copy(error = "Trim too short (min 30 seconds)")
            return
        }
        if (current.error != null) {
            return
        }

        _uiState.value = current.copy(isTrimming = true, error = null, trimResult = null)
        trimJob = viewModelScope.launch {
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
                            _uiState.value = state.copy(
                                isTrimming = false,
                                error = result.message,
                                trimResult = null
                            )
                            trimJob = null
                        }
                        is TrimResult.PermissionDenied -> {
                            _uiState.value = state.copy(
                                isTrimming = false,
                                error = "Write permission required",
                                trimResult = null
                            )
                            trimJob = null
                        }
                    }
                }
        }
    }

    fun cancelTrim() {
        trimJob?.cancel()
        trimJob = null
        val current = _uiState.value
        _uiState.value = current.copy(
            isTrimming = false,
            error = "Trimming cancelled by user",
            trimResult = null
        )
    }

    fun clearError() {
        val current = _uiState.value
        // Do not clear file size error
        val audioFile = current.audioFile
        val isFileSizeError = audioFile?.size?.let { it > 10_000_000L } == true && current.error?.contains("File too large") == true
        if (!isFileSizeError) {
            _uiState.value = current.copy(error = null, trimResult = null)
        }
    }

    fun resetTrim() {
        val audioFile = _uiState.value.audioFile ?: return
        _uiState.value = _uiState.value.copy(
            startTimeMs = 0L,
            endTimeMs = audioFile.duration,
            error = _uiState.value.error // Preserve file size error if present
        )
    }

    fun pauseMainPlayerIfPlaying() {
        viewModelScope.launch {
            val state = playbackController.getPlaybackState().first()
            if (state.isPlaying) {
                wasMainPlaying = true
                playbackController.playPause()
            } else {
                wasMainPlaying = false
            }
        }
    }

    fun resumeMainPlayerIfPaused() {
        if (wasMainPlaying) {
            viewModelScope.launch {
                playbackController.playPause() // Toggle back to play
                wasMainPlaying = false
            }
        }
    }
}