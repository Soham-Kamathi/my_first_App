package com.localllm.app.ui.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.localllm.app.ui.viewmodel.AudioScribeViewModel
import com.localllm.app.ui.viewmodel.AudioState

/**
 * Audio Scribe Screen - Audio transcription and translation
 * Allows users to record audio or upload files for transcription.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioScribeScreen(
    viewModel: AudioScribeViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    val audioState by viewModel.audioState.collectAsState()
    val transcription by viewModel.transcription.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val recordingDuration by viewModel.recordingDuration.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val translateToEnglish by viewModel.translateToEnglish.collectAsState()
    val isWhisperSupported by viewModel.isWhisperSupported.collectAsState()
    val hasAudioPermission by viewModel.hasAudioPermission.collectAsState()
    val downloadedWhisperModels by viewModel.downloadedWhisperModels.collectAsState()
    val currentWhisperModel by viewModel.currentWhisperModel.collectAsState()
    val whisperLoadingState by viewModel.whisperLoadingState.collectAsState()
    val transcriptionProgress by viewModel.transcriptionProgress.collectAsState()
    
    var showModelPicker by remember { mutableStateOf(false) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setAudioPermission(granted)
    }

    // File picker for audio files
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.loadAudioFile(it) }
    }

    // Request permission on first launch
    LaunchedEffect(Unit) {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Scribe") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.clearAll() },
                        enabled = transcription.isNotEmpty() || audioState != AudioState.IDLE
                    ) {
                        Icon(Icons.Default.Refresh, "Clear all")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Whisper Model Status
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isWhisperSupported) 
                        MaterialTheme.colorScheme.primaryContainer
                    else 
                        MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = downloadedWhisperModels.isNotEmpty()) { 
                            showModelPicker = true 
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (isWhisperSupported) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isWhisperSupported) 
                                MaterialTheme.colorScheme.primary
                            else 
                                MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                if (isWhisperSupported) "Whisper Ready" else "No Whisper Model",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                currentWhisperModel?.name ?: "Download a Whisper model from Model Library",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (downloadedWhisperModels.size > 1) {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Change model",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Recording Control Area
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Recording Animation/Status
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (audioState) {
                            AudioState.RECORDING -> {
                                // Pulsing animation
                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                val scale by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = 1.2f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(800),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "scale"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .scale(scale)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                                )
                            }
                            AudioState.PROCESSING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(100.dp),
                                    strokeWidth = 4.dp
                                )
                            }
                            AudioState.HAS_AUDIO -> {
                                Icon(
                                    Icons.Default.AudioFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            else -> {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Main Record Button
                        FilledIconButton(
                            onClick = {
                                when (audioState) {
                                    AudioState.IDLE -> viewModel.startRecording()
                                    AudioState.RECORDING -> viewModel.stopRecording()
                                    else -> {}
                                }
                            },
                            modifier = Modifier.size(80.dp),
                            enabled = hasAudioPermission && audioState != AudioState.PROCESSING,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = when (audioState) {
                                    AudioState.RECORDING -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        ) {
                            Icon(
                                imageVector = when (audioState) {
                                    AudioState.RECORDING -> Icons.Default.Stop
                                    else -> Icons.Default.Mic
                                },
                                contentDescription = when (audioState) {
                                    AudioState.RECORDING -> "Stop recording"
                                    else -> "Start recording"
                                },
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Status Text
                    Text(
                        text = when (audioState) {
                            AudioState.IDLE -> "Tap to record"
                            AudioState.RECORDING -> formatDuration(recordingDuration)
                            AudioState.PROCESSING -> "Processing..."
                            AudioState.HAS_AUDIO -> "Audio ready"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = if (audioState == AudioState.RECORDING) FontWeight.Bold else FontWeight.Normal,
                        color = if (audioState == AudioState.RECORDING) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )

                    // Permission Warning
                    if (!hasAudioPermission) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Grant microphone permission")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Alternative Input Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { audioPickerLauncher.launch("audio/*") },
                    modifier = Modifier.weight(1f),
                    enabled = audioState == AudioState.IDLE
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Audio")
                }
                OutlinedButton(
                    onClick = { viewModel.clearAudio() },
                    modifier = Modifier.weight(1f),
                    enabled = audioState == AudioState.HAS_AUDIO
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Settings Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Language Selection
                    var languageExpanded by remember { mutableStateOf(false) }
                    val languages = listOf(
                        "auto" to "Auto-detect",
                        "en" to "English",
                        "es" to "Spanish",
                        "fr" to "French",
                        "de" to "German",
                        "it" to "Italian",
                        "pt" to "Portuguese",
                        "zh" to "Chinese",
                        "ja" to "Japanese",
                        "ko" to "Korean",
                        "ru" to "Russian",
                        "ar" to "Arabic",
                        "hi" to "Hindi"
                    )

                    ExposedDropdownMenuBox(
                        expanded = languageExpanded,
                        onExpandedChange = { languageExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = languages.find { it.first == selectedLanguage }?.second ?: "Auto-detect",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Source Language") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = languageExpanded,
                            onDismissRequest = { languageExpanded = false }
                        ) {
                            languages.forEach { (code, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        viewModel.setLanguage(code)
                                        languageExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Translate to English option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.setTranslateToEnglish(!translateToEnglish) }
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Translate to English",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "Automatically translate transcription",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = translateToEnglish,
                            onCheckedChange = { viewModel.setTranslateToEnglish(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transcribe Button
            Button(
                onClick = { viewModel.transcribe() },
                modifier = Modifier.fillMaxWidth(),
                enabled = audioState == AudioState.HAS_AUDIO && !isProcessing && isWhisperSupported
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Transcribing...")
                } else {
                    Icon(Icons.Default.Transcribe, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Transcribe")
                }
            }

            // Transcription Output
            if (transcription.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Transcription",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row {
                                IconButton(onClick = { viewModel.copyTranscription() }) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(onClick = { viewModel.clearTranscription() }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SelectionContainer {
                            Text(
                                text = transcription,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            // Transcription progress
            if (isProcessing && transcriptionProgress > 0f) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Transcribing...",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${(transcriptionProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = transcriptionProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Model Picker Dialog
    if (showModelPicker && downloadedWhisperModels.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showModelPicker = false },
            title = { Text("Select Whisper Model") },
            text = {
                Column {
                    downloadedWhisperModels.forEach { model ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.loadWhisperModel(model)
                                    showModelPicker = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (model.id == currentWhisperModel?.id)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (model.id == currentWhisperModel?.id) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Column {
                                    Text(
                                        model.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (model.id == currentWhisperModel?.id) 
                                            FontWeight.Bold 
                                        else 
                                            FontWeight.Normal
                                    )
                                    Text(
                                        "${model.formattedFileSize()} • ${model.parameterCount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelPicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}

@Composable
private fun SelectionContainer(content: @Composable () -> Unit) {
    androidx.compose.foundation.text.selection.SelectionContainer(content = content)
}
