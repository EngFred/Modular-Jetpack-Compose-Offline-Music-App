package com.engfred.musicplayer.feature_library.presentation.viewmodel

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject

data class EditSongUiState(
    val audioFile: AudioFile? = null,
    val title: String = "",
    val artist: String = "",
    val albumArtPreviewUri: Uri? = null,
    val isSaving: Boolean = false
)

@HiltViewModel
class EditSongViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    @ApplicationContext private val appContext: Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditSongUiState())
    val uiState: StateFlow<EditSongUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    sealed class Event {
        data class Success(val message: String) : Event()
        data class Error(val message: String) : Event()
    }

    fun loadAudioFile(audioId: Long) {
        viewModelScope.launch {
            libraryRepository.getAllAudioFiles().collect { list ->
                val audio = list.find { it.id == audioId }
                val preview = getAppAlbumArtFileIfExists(audioId)
                _uiState.value = EditSongUiState(
                    audioFile = audio,
                    title = audio?.title ?: "",
                    artist = audio?.artist ?: "",
                    albumArtPreviewUri = preview
                )
            }
        }
    }

    private fun getAppAlbumArtFileIfExists(audioId: Long): Uri? {
        val file = File(appContext.filesDir, "album_art_$audioId.jpg")
        return if (file.exists()) Uri.fromFile(file) else null
    }

    fun pickImage(imageUri: Uri) {
        val audioId = _uiState.value.audioFile?.id ?: return
        viewModelScope.launch {
            val saved = saveImageToAppFiles(audioId, imageUri)
            if (saved != null) {
                _uiState.value = _uiState.value.copy(albumArtPreviewUri = saved)
            } else {
                _events.emit(Event.Error("Failed to save selected image."))
            }
        }
    }

    @WorkerThread
    private suspend fun saveImageToAppFiles(audioId: Long, imageUri: Uri): Uri? = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = appContext.contentResolver.openInputStream(imageUri)
            inputStream ?: return@withContext null
            val outFile = File(appContext.filesDir, "album_art_$audioId.jpg")
            inputStream.use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return@withContext Uri.fromFile(outFile)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    fun updateTitle(newTitle: String) {
        _uiState.value = _uiState.value.copy(title = newTitle)
    }

    fun updateArtist(newArtist: String) {
        _uiState.value = _uiState.value.copy(artist = newArtist)
    }

    fun saveChanges() {
        val audio = _uiState.value.audioFile ?: run {
            viewModelScope.launch { _events.emit(Event.Error("No audio selected")) }
            return
        }
        val newTitle = _uiState.value.title.trim()
        val newArtist = _uiState.value.artist.trim()

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val updated = updateMediaStoreMetadata(audio.id, newTitle, newArtist)
                if (updated) _events.emit(Event.Success("Song updated successfully")) else _events.emit(Event.Error("Failed to update song metadata"))
            } catch (e: Exception) {
                _events.emit(Event.Error("Failed to save changes: ${e.message}"))
            } finally {
                _uiState.value = _uiState.value.copy(isSaving = false)
            }
        }
    }

    private fun updateMediaStoreMetadata(audioId: Long, newTitle: String, newArtist: String): Boolean {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.TITLE, newTitle)
                put(MediaStore.Audio.Media.ARTIST, newArtist)
            }
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val where = "${MediaStore.Audio.Media._ID} = ?"
            val rows = appContext.contentResolver.update(uri, values, where, arrayOf(audioId.toString()))
            val itemUri = ContentUris.withAppendedId(uri, audioId)
            appContext.contentResolver.notifyChange(itemUri, null)
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
