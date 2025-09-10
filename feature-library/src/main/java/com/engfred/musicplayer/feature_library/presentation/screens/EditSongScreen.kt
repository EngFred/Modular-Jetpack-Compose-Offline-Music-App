package com.engfred.musicplayer.feature_library.presentation.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.engfred.musicplayer.feature_library.presentation.viewmodel.EditSongUiState
import com.engfred.musicplayer.feature_library.presentation.viewmodel.EditSongViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun EditSongScreenContainer(
    audioId: Long,
    onFinish: () -> Unit,
    viewModel: EditSongViewModel = hiltViewModel()
) {
    LaunchedEffect(audioId) { viewModel.loadAudioFile(audioId) }

    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Events: success / error
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
            }
        }
    }

    EditSongScreen(
        uiState = state,
        onPickImage = { uri -> viewModel.pickImage(uri) },
        onTitleChange = viewModel::updateTitle,
        onArtistChange = viewModel::updateArtist,
        onSave = viewModel::saveChanges,
        onCancel = onFinish
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSongScreen(
    uiState: EditSongUiState,
    onPickImage: (Uri) -> Unit,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> uri?.let(onPickImage) }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Edit Song", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") }
                }
            )
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
                    AsyncImage(model = imageUri, contentDescription = "Album art preview", modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Rounded.Image, contentDescription = "No album art", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FilledTonalButton(onClick = { pickImageLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth(0.8f)) {
                Text(text = "Change Album Art")
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(value = uiState.title, onValueChange = onTitleChange, label = { Text("Song Title") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = uiState.artist, onValueChange = onArtistChange, label = { Text("Artist") }, modifier = Modifier.fillMaxWidth())

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
