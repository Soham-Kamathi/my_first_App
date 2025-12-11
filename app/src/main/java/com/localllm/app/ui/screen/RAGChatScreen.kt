package com.localllm.app.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.localllm.app.data.model.MessageRole
import com.localllm.app.inference.ModelLoadingState
import com.localllm.app.rag.IndexingState
import com.localllm.app.ui.components.ChatInput
import com.localllm.app.ui.components.MessageBubble
import com.localllm.app.ui.viewmodel.RAGChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RAGChatScreen(
    viewModel: RAGChatViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToModels: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val loadingState by viewModel.loadingState.collectAsState()
    val indexingState by viewModel.indexingState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            val fileName = cursor?.use { c ->
                val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                c.moveToFirst()
                if (nameIndex >= 0) c.getString(nameIndex) else "document"
            } ?: "document"
            viewModel.indexDocument(uri, fileName)
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RAG Chat") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                // RAG toggle
                IconButton(onClick = { viewModel.toggleRAG() }) {
                    Icon(
                        if (uiState.ragEnabled) Icons.Default.Storage else Icons.Default.CloudOff,
                        contentDescription = if (uiState.ragEnabled) "RAG Enabled" else "RAG Disabled",
                        tint = if (uiState.ragEnabled) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }                    // Upload document
                    IconButton(onClick = {
                        documentPickerLauncher.launch(arrayOf(
                            "application/pdf",
                            "text/plain",
                            "text/markdown",
                            "text/*"
                        ))
                    }) {
                        Icon(Icons.Default.UploadFile, "Upload Document")
                    }
                    
                    // Clear all
                    IconButton(onClick = { viewModel.clearMessages() }) {
                        Icon(Icons.Default.Delete, "Clear")
                    }
                }
            )
        },
        bottomBar = {
            ChatInput(
                enabled = viewModel.isModelLoaded && !uiState.isGenerating,
                isGenerating = uiState.isGenerating,
                onSendMessage = { text ->
                    viewModel.updateInput(text)
                    viewModel.sendMessage()
                },
                onStopGeneration = { viewModel.stopGeneration() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Model loading indicator
            if (loadingState is ModelLoadingState.Loading) {
                LinearProgressIndicator(
                    progress = (loadingState as ModelLoadingState.Loading).progress,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Indexing progress
            when (indexingState) {
                is IndexingState.Indexing -> {
                    val state = indexingState as IndexingState.Indexing
                    LinearProgressIndicator(
                        progress = state.progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        "Indexing: ${state.currentChunk}/${state.totalChunks}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                is IndexingState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            (indexingState as IndexingState.Error).message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                else -> {}
            }
            
            // Error message
            uiState.errorMessage?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(error, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Default.Close, "Dismiss")
                        }
                    }
                }
            }
            
            // RAG Status banner
            if (uiState.ragEnabled && uiState.indexedDocuments.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Storage,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "${uiState.indexedDocuments.size} documents indexed",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Messages
            if (uiState.messages.isEmpty()) {
                EmptyRAGState(
                    onUploadDocument = {
                        documentPickerLauncher.launch(arrayOf(
                            "application/pdf",
                            "text/plain",
                            "text/markdown",
                            "text/*"
                        ))
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(
                        items = uiState.messages,
                        key = { index, msg -> "${index}_${msg.id}" }
                    ) { _, message ->
                        MessageBubble(
                            message = message,
                            showTokensPerSecond = false,
                            onCopy = {},
                            onDelete = {},
                            onRegenerate = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRAGState(
    onUploadDocument: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.FindInPage,
            null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "RAG-Enhanced Chat",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Upload documents and ask questions with semantic search",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onUploadDocument) {
            Icon(Icons.Default.UploadFile, null)
            Spacer(Modifier.width(8.dp))
            Text("Upload Document")
        }
        Spacer(Modifier.height(24.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Features:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("• TF-IDF vector embeddings")
                Text("• Semantic similarity search")
                Text("• Context-aware responses")
                Text("• Multi-document support")
                Text("• PDF, TXT, MD files")
            }
        }
    }
}
