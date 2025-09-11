package com.engfred.musicplayer.feature_library.presentation.viewmodel

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.core.common.Resource
import com.engfred.musicplayer.feature_library.domain.usecases.EditAudioMetadataUseCase
import com.engfred.musicplayer.feature_library.domain.usecases.GetAllAudioFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditSongUiState(
    val title: String = "",
    val artist: String = "",
    val albumArtPreviewUri: Uri? = null,
    val isSaving: Boolean = false
)

@HiltViewModel
class EditSongViewModel @Inject constructor(
    private val getAllAudioFilesUseCase: GetAllAudioFilesUseCase,
    private val editAudioMetadataUseCase: EditAudioMetadataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditSongUiState())
    val uiState: StateFlow<EditSongUiState> = _uiState

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    // Original values for comparison (to detect changes)
    private var originalTitle: String? = null
    private var originalArtist: String? = null
    private var originalAlbumArtUri: Uri? = null

    // Pending save information
    private var pendingAudioId: Long? = null
    private var pendingTitle: String? = null
    private var pendingArtist: String? = null
    private var pendingAlbumArt: ByteArray? = null

    sealed class Event {
        data class Success(val message: String) : Event()
        data class Error(val message: String) : Event()
        data class RequestWritePermission(val intentSender: IntentSender) : Event()
    }

    fun loadAudioFile(audioId: Long) {
        viewModelScope.launch {
            getAllAudioFilesUseCase().collect { resource ->
                if (resource is Resource.Success) {
                    val audioFile = resource.data?.find { it.id == audioId }
                    if (audioFile != null) {
                        originalTitle = audioFile.title
                        originalArtist = audioFile.artist ?: "Unknown Artist"
                        originalAlbumArtUri = audioFile.albumArtUri
                        _uiState.update {
                            it.copy(
                                title = originalTitle ?: "",
                                artist = originalArtist ?: "",
                                albumArtPreviewUri = originalAlbumArtUri
                            )
                        }
                    } else {
                        _events.emit(Event.Error("Audio file not found."))
                    }
                } else if (resource is Resource.Error) {
                    _events.emit(Event.Error(resource.message ?: "Error loading audio files."))
                }
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun updateArtist(newArtist: String) {
        _uiState.update { it.copy(artist = newArtist) }
    }

    fun pickImage(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(albumArtPreviewUri = imageUri) }
        }
    }

    fun saveChanges(audioId: Long, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value

            // Read album art bytes if changed
            var albumArtBytes: ByteArray? = null
            if (state.albumArtPreviewUri != originalAlbumArtUri) {
                state.albumArtPreviewUri?.let { uri ->
                    try {
                        albumArtBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    } catch (e: Exception) {
                        _events.emit(Event.Error("Failed to read album art image: ${e.message ?: "unknown"}"))
                        _uiState.update { it.copy(isSaving = false) }
                        return@launch
                    }
                }
            }

            // Set pending only if changed or explicitly set (even blank)
            pendingAudioId = audioId
            pendingTitle = if (state.title != originalTitle) state.title else null
            pendingArtist = if (state.artist != originalArtist) state.artist else null
            pendingAlbumArt = albumArtBytes

            // Skip if no changes
            if (pendingTitle == null && pendingArtist == null && pendingAlbumArt == null) {
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(Event.Success("No changes to save."))
                clearPending()
                return@launch
            }

            try {
                val result = editAudioMetadataUseCase(
                    id = audioId,
                    title = pendingTitle,
                    artist = pendingArtist,
                    albumArt = pendingAlbumArt,
                    context = context
                )
                _uiState.update { it.copy(isSaving = false) }
                when (result) {
                    is Resource.Success -> {
                        clearPending()
                        _events.emit(Event.Success("Song metadata updated successfully."))
                    }
                    is Resource.Error -> {
                        _events.emit(Event.Error(result.message ?: "Failed to update song metadata."))
                    }
                    else -> {
                        _events.emit(Event.Error("Unexpected state while saving metadata."))
                    }
                }
            } catch (rse: RecoverableSecurityException) {
                _uiState.update { it.copy(isSaving = false) }
                val intentSender = rse.userAction.actionIntent.intentSender
                if (intentSender != null) {
                    _events.emit(Event.RequestWritePermission(intentSender))
                } else {
                    _events.emit(Event.Error("Need permission to edit this audio file."))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(Event.Error(e.message ?: "Failed to update song metadata."))
            }
        }
    }

    fun continueSaveAfterPermission(context: Context) {
        val id = pendingAudioId ?: run {
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val result = editAudioMetadataUseCase(
                    id = id,
                    title = pendingTitle,
                    artist = pendingArtist,
                    albumArt = pendingAlbumArt,
                    context = context
                )
                _uiState.update { it.copy(isSaving = false) }
                when (result) {
                    is Resource.Success -> {
                        clearPending()
                        _events.emit(Event.Success("Song metadata updated successfully."))
                    }
                    is Resource.Error -> {
                        _events.emit(Event.Error(result.message ?: "Failed to update song metadata."))
                    }
                    else -> {
                        _events.emit(Event.Error("Unexpected state while saving metadata."))
                    }
                }
            } catch (rse: RecoverableSecurityException) {
                _uiState.update { it.copy(isSaving = false) }
                val intentSender = rse.userAction.actionIntent.intentSender
                if (intentSender != null) {
                    _events.emit(Event.RequestWritePermission(intentSender))
                } else {
                    _events.emit(Event.Error("Need permission to edit this audio file."))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(Event.Error(e.message ?: "Failed to update song metadata."))
            }
        }
    }

    private fun clearPending() {
        pendingAudioId = null
        pendingTitle = null
        pendingArtist = null
        pendingAlbumArt = null
    }
}