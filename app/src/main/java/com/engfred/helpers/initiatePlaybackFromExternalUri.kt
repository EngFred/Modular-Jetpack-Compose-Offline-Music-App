package com.engfred.helpers

//import android.content.Context
//import android.net.Uri
//import android.util.Log
//import android.widget.Toast
//import com.engfred.musicplayer.TAG
//import com.engfred.musicplayer.core.common.Resource
//import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
//import com.engfred.musicplayer.core.domain.repository.LibraryRepository
//import com.engfred.musicplayer.core.domain.repository.PlaybackController
//import com.engfred.musicplayer.core.domain.repository.PlaybackState
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.withContext
//
//suspend fun initiatePlaybackFromExternalUri(
//    uri: Uri,
//    context: Context,
//    playbackController: PlaybackController,
//    libraryRepository: LibraryRepository,
//    sharedAudioDataSource: SharedAudioDataSource,
//    playbackState: PlaybackState
//): Boolean {
//    try {
//        Log.d(TAG, "Attempt to initiate playback for external URI: $uri")
//
//        // Wait for player to become ready with a reasonable timeout handled inside controller
//        if (!playbackController.waitUntilReady()) {
//            Log.e(TAG, "Player not ready in time for external playback.")
//            withContext(Dispatchers.Main) {
//                Toast.makeText(context, "Player not ready. Please try again.", Toast.LENGTH_LONG).show()
//            }
//            return false
//        }
//
//        val audioFileFetchStatus = libraryRepository.getAudioFileByUri(uri)
//        when (audioFileFetchStatus) {
//            is Resource.Error -> {
//                Log.e(TAG, "Failed to fetch audio file for external URI: ${audioFileFetchStatus.message}")
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(context, "Failed to play selected file: ${audioFileFetchStatus.message}", Toast.LENGTH_LONG).show()
//                }
//                return false
//            }
//            is Resource.Loading -> {
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(context, "Opening file in Music..", Toast.LENGTH_SHORT).show()
//                }
//                return false
//            }
//            is Resource.Success -> {
//                val audioFile = audioFileFetchStatus.data ?: run {
//                    Log.e(TAG, "Audio File not found!")
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(context, "Audio File not found!", Toast.LENGTH_LONG).show()
//                    }
//                    return false
//                }
//
//                sharedAudioDataSource.setPlayingQueue(listOf(audioFile))
//                playbackController.initiatePlayback(uri)
//
//                // Wait briefly for playback state update
//                val startTime = System.currentTimeMillis()
//                var success = false
//                while (System.currentTimeMillis() - startTime < 3_000 && !success) {
//                    if (playbackState.currentAudioFile != null && (playbackState.isPlaying || playbackState.isLoading)) {
//                        success = true
//                    }
//                    delay(200)
//                }
//
//                if (!success) {
//                    Log.w(TAG, "Playback did not start successfully within timeout.")
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(context, "Failed to start playback.", Toast.LENGTH_SHORT).show()
//                    }
//                }
//                return success
//            }
//        }
//    } catch (e: Exception) {
//        Log.e(TAG, "Failed to start playback for external URI: ${e.message}", e)
//        withContext(Dispatchers.Main) {
//            Toast.makeText(context, "Failed to play selected file: ${e.message}", Toast.LENGTH_LONG).show()
//        }
//        return false
//    }
//}