package com.engfred.musicplayer.feature_library.presentation.screens

import android.app.Activity
import android.app.PendingIntent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.engfred.musicplayer.core.ui.CustomTopBar
import com.engfred.musicplayer.feature_library.presentation.viewmodel.EditSongUiState
import com.engfred.musicplayer.feature_library.presentation.viewmodel.EditSongViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun EditSongScreenContainer(
    audioId: Long,
    onFinish: () -> Unit,
    viewModel: EditSongViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    // Launcher to pick an image
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> uri?.let { viewModel.pickImage(it) } }
    )

    // Launcher for IntentSender (used for PendingIntent / createWriteRequest)
    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            // after user grants per-file access, proceed with saving flow
            viewModel.continueSaveAfterPermission(context)
        } else {
            Toast.makeText(context, "Access to song denied. Cannot edit.", Toast.LENGTH_LONG).show()
            onFinish()
        }
    }

    // Launcher for runtime permission (general read/write)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            // If Q+ we need to request per-file write access via createWriteRequest -> PendingIntent
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioId.toString())
                val pendingIntent: PendingIntent =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
                    } else {
                        TODO("VERSION.SDK_INT < R")
                    }
                pendingIntent.intentSender.let { sender ->
                    val req = IntentSenderRequest.Builder(sender).build()
                    intentSenderLauncher.launch(req)
                } ?: run {
                    // Fallback: inform user we couldn't build the write request but keep UI visible
                    Toast.makeText(context, "Could not build write request; continuing without per-file access.", Toast.LENGTH_SHORT).show()
                }
            } else {
                // pre-Q nothing else required; user can edit
                Toast.makeText(context, "Permission granted", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Storage permission denied. Cannot edit song.", Toast.LENGTH_LONG).show()
            onFinish()
        }
    }

    // Collect ViewModel events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is EditSongViewModel.Event.Success -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    onFinish()
                }
                is EditSongViewModel.Event.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is EditSongViewModel.Event.RequestWritePermission -> {
                    // If ViewModel provides an IntentSender for a recoverable SecurityException,
                    // launch it directly (system prompt). We can't style the system UI; it's owned by the OS.
                    val req = IntentSenderRequest.Builder(event.intentSender)
                        .setFillInIntent(null)
                        .build()
                    intentSenderLauncher.launch(req)
                }
            }
        }
    }

    // Request permissions/upfront flows on enter (but UI is always visible)
    LaunchedEffect(audioId) {
        viewModel.loadAudioFile(audioId)

        // Determine runtime permission to request
        val perm = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> android.Manifest.permission.READ_MEDIA_AUDIO
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> android.Manifest.permission.READ_EXTERNAL_STORAGE
            else -> android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        }

        val granted = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            // Proceed to per-file if Q+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioId.toString())
                val pendingIntent: PendingIntent =
                    MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
                pendingIntent.intentSender.let { sender ->
                    val req = IntentSenderRequest.Builder(sender).build()
                    intentSenderLauncher.launch(req)
                } ?: run {
                    // Fallback: inform user but keep UI visible
                    Toast.makeText(context, "Could not build write request; continuing.", Toast.LENGTH_SHORT).show()
                }
            } else {
                // pre-Q no further action required
            }
        } else {
            // Launch runtime permission request; the result handler will handle the rest.
            permissionLauncher.launch(perm)
        }
    }

    // ALWAYS show the Edit UI (no gating)
    EditSongScreen(
        uiState = state,
        onPickImage = { pickImageLauncher.launch("image/*") },
        onTitleChange = viewModel::updateTitle,
        onArtistChange = viewModel::updateArtist,
        onSave = { viewModel.saveChanges(audioId, context) },
        onCancel = onFinish
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSongScreen(
    uiState: EditSongUiState,
    onPickImage: () -> Unit,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(
        topBar = {
            CustomTopBar("Edit Song", showNavigationIcon = true, onNavigateBack = {
                onCancel()
            }, modifier = Modifier.statusBarsPadding())
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val imageUri = uiState.albumArtPreviewUri
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Album art preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Rounded.Image,
                        contentDescription = "No album art",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            FilledTonalButton(onClick = onPickImage, modifier = Modifier.fillMaxWidth(0.8f)) {
                Text(text = "Change Album Art")
            }
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = uiState.title,
                onValueChange = onTitleChange,
                label = { Text("Song Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.artist,
                onValueChange = onArtistChange,
                label = { Text("Artist") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            if (uiState.isSaving) CircularProgressIndicator() else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onSave) { Text("Save changes") }
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Editing metadata in Music. Title & artist changes will be applied system-wide. Album art is stored and used by Music.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
