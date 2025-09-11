package com.engfred.musicplayer.feature_library.presentation.viewmodel

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.core.common.Resource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.repository.FavoritesRepository
import com.engfred.musicplayer.core.domain.repository.PlaylistRepository
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
    val isSaving: Boolean = false,
    val audioFile: AudioFile? = null
)

@HiltViewModel
class EditSongViewModel @Inject constructor(
    private val getAllAudioFilesUseCase: GetAllAudioFilesUseCase,
    private val editAudioMetadataUseCase: EditAudioMetadataUseCase,
    private val playlistRepository: PlaylistRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditSongUiState())
    val uiState: StateFlow<EditSongUiState> = _uiState

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    // Original values for comparison to detect changes
    private var originalTitle: String? = null
    private var originalArtist: String? = null
    private var originalAlbumArtUri: Uri? = null

    // State to hold pending changes after a permission request
    private var pendingAudioId: Long? = null
    private var pendingTitle: String? = null
    private var pendingArtist: String? = null
    private var pendingAlbumArt: ByteArray? = null

    sealed class Event {
        data class Success(val message: String) : Event()
        data class Error(val message: String) : Event()
        data class RequestWritePermission(val intentSender: IntentSender) : Event()
    }

    /**
     * Loads a specific audio file by its ID and updates the UI state.
     * Note: this function will collect the flow; it does NOT reload after save per your request.
     */
    fun loadAudioFile(audioId: Long) {
        viewModelScope.launch {
            getAllAudioFilesUseCase().collect { resource ->
                if (resource is Resource.Success) {
                    val audioFile = resource.data?.find { it.id == audioId }
                    audioFile?.let {
                        updateOriginalValues(it)
                        _uiState.update { state ->
                            state.copy(
                                title = originalTitle ?: "",
                                artist = originalArtist ?: "Unknown Artist",
                                albumArtPreviewUri = originalAlbumArtUri,
                                audioFile = it
                            )
                        }
                    } ?: _events.emit(Event.Error("Audio file not found."))
                } else if (resource is Resource.Error) {
                    _events.emit(Event.Error(resource.message ?: "Error loading audio files."))
                }
            }
        }
    }

    /**
     * Update title being edited.
     */
    fun updateTitle(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    /**
     * Update artist being edited.
     */
    fun updateArtist(newArtist: String) {
        _uiState.update { it.copy(artist = newArtist) }
    }

    /**
     * User picked an image for preview; we store the preview Uri.
     */
    fun pickImage(imageUri: Uri) {
        _uiState.update { it.copy(albumArtPreviewUri = imageUri) }
    }

    /**
     * Initiates saving changes to the media store using the EditAudioMetadataUseCase.
     */
    fun saveChanges(audioId: Long, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val currentState = _uiState.value
            val albumArtBytes = getAlbumArtBytes(currentState.albumArtPreviewUri, context)

            // If user selected a new album art but we can't read it -> fail early.
            if (albumArtBytes == null && currentState.albumArtPreviewUri != originalAlbumArtUri) {
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(Event.Error("Failed to read album art image."))
                return@launch
            }

            // Save pending values only if changed compared to originals
            pendingAudioId = audioId
            pendingTitle = currentState.title.takeIf { it != originalTitle }
            pendingArtist = currentState.artist.takeIf { it != originalArtist }
            pendingAlbumArt = albumArtBytes.takeIf { currentState.albumArtPreviewUri != originalAlbumArtUri }

            // Nothing to change
            if (pendingTitle == null && pendingArtist == null && pendingAlbumArt == null) {
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(Event.Success("No changes to save."))
                clearPending()
                return@launch
            }

            performSave(audioId, context)
        }
    }

    /**
     * Continue save after permission granted flow.
     */
    fun continueSaveAfterPermission(context: Context) {
        val audioId = pendingAudioId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            performSave(audioId, context)
        }
    }

    /**
     * Performs the save and handles permissions/errors.
     */
    private suspend fun performSave(audioId: Long, context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            _uiState.update { it.copy(isSaving = false) }
            _events.emit(Event.Error("Editing metadata is not supported on this Android version."))
            clearPending()
            return
        }

        try {
            val result = editAudioMetadataUseCase(
                id = audioId,
                title = pendingTitle,
                artist = pendingArtist,
                albumArt = pendingAlbumArt,
                context = context
            )
            handleSaveResult(result)
        } catch (e: RecoverableSecurityException) {
            _uiState.update { it.copy(isSaving = false) }
            val intentSender = e.userAction.actionIntent.intentSender
            if (intentSender != null) {
                _events.emit(Event.RequestWritePermission(intentSender))
            } else {
                _events.emit(Event.Error("Need permission to edit this audio file."))
                clearPending()
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isSaving = false) }
            _events.emit(Event.Error(e.message ?: "Failed to update song metadata."))
            clearPending()
        }
    }

    /**
     * Called after editAudioMetadataUseCase completes.
     * We DO NOT reload from the audio files flow; instead we build an updated AudioFile object locally
     * by merging the current audio file with the pending changes, update UI state/originals, and pass
     * that updated object to playlists/favorites repositories.
     */
    private suspend fun handleSaveResult(result: Resource<Unit>) {
        _uiState.update { it.copy(isSaving = false) }

        when (result) {
            is Resource.Success -> {
                // Build the canonical updated AudioFile locally (do NOT reload)
                val currentAudio = _uiState.value.audioFile
                if (currentAudio == null) {
                    // Defensive: if there is no audio loaded in state, return an error rather than passing wrong data.
                    _events.emit(Event.Error("Updated, but local audio was not available to reflect changes."))
                    clearPending()
                    return
                }

                // Create updated audio by applying pending changes.
                // Note: for albumArtUri we use the current preview Uri (selected by user) if they changed album art.
                val updatedAudio = currentAudio.copy(
                    title = pendingTitle ?: currentAudio.title,
                    artist = pendingArtist ?: currentAudio.artist,
                    albumArtUri = if (pendingAlbumArt != null) _uiState.value.albumArtPreviewUri else currentAudio.albumArtUri
                )

                // Update UI with new canonical object and update original comparison values
                updateOriginalValues(updatedAudio)
                _uiState.update { state ->
                    state.copy(
                        title = updatedAudio.title,
                        artist = updatedAudio.artist ?: "Unknown Artist",
                        albumArtPreviewUri = updatedAudio.albumArtUri,
                        audioFile = updatedAudio
                    )
                }

                // Now pass the updatedAudio to repositories (they now receive the updated data immediately)
                try {
                    playlistRepository.updateSongInAllPlaylists(updatedAudio)
                } catch (e: Exception) {
                    // Log or handle as needed; don't fail the entire flow just because repo update failed.
                    _events.emit(Event.Error("Updated metadata but failed to update playlists: ${e.message}"))
                    // still attempt favorites below
                }

                try {
                    favoritesRepository.updateFavoriteAudioFile(updatedAudio)
                } catch (e: Exception) {
                    _events.emit(Event.Error("Updated metadata but failed to update favorites: ${e.message}"))
                }

                _events.emit(Event.Success("Song info updated successfully."))
                clearPending()
            }

            is Resource.Error -> {
                _events.emit(Event.Error(result.message ?: "Failed to update song metadata."))
                clearPending()
            }

            else -> {
                _events.emit(Event.Error("Unexpected state while saving metadata."))
                clearPending()
            }
        }
    }

    /**
     * Reads the album art bytes from a given URI.
     */
    private fun getAlbumArtBytes(uri: Uri?, context: Context): ByteArray? {
        return uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    inputStream.readBytes()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Update original values used for "changed?" comparisons.
     */
    private fun updateOriginalValues(audioFile: AudioFile) {
        originalTitle = audioFile.title
        originalArtist = audioFile.artist ?: "Unknown Artist"
        originalAlbumArtUri = audioFile.albumArtUri
    }

    /**
     * Clears pending save state.
     */
    private fun clearPending() {
        pendingAudioId = null
        pendingTitle = null
        pendingArtist = null
        pendingAlbumArt = null
    }
}
