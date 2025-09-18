package com.engfred.helpers

//import android.content.Context
//import android.net.Uri
//import android.util.Log
//import com.engfred.musicplayer.TAG
//
//fun Context.tryOpenUriStream(
//    uri: Uri,
//): Boolean {
//    return try {
//        contentResolver.openInputStream(uri)?.use { }
//        true
//    } catch (e: SecurityException) {
//        Log.w(TAG, "No permission to open URI: ${e.message}")
//        false
//    } catch (e: Exception) {
//        Log.w(TAG, "Could not open URI stream: ${e.message}")
//        false
//    }
//}