package com.engfred.musicplayer.core.util

import android.content.Context
import android.content.Intent
import com.engfred.musicplayer.core.domain.model.AudioFile

/**
 * A private utility function to create and launch an Intent for sharing an AudioFile.
 * This can be a regular function, not necessarily a Composable, since it performs
 * an action and doesn't emit UI.
 *
 * @param context The Android Context to start the activity.
 * @param audioFile The AudioFile containing the URI and metadata to share.
 */
fun shareAudioFile(context: Context, audioFile: AudioFile) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "audio/*"
        putExtra(Intent.EXTRA_STREAM, audioFile.uri)
        putExtra(Intent.EXTRA_TITLE, "Sharing ${audioFile.title} by ${audioFile.artist}")
        putExtra(Intent.EXTRA_TEXT, "Listen to '${audioFile.title}' by ${audioFile.artist}!")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant read permissions to other apps
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Music"))
}