package com.localllm.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.localllm.app.data.model.DownloadState
import com.localllm.app.data.model.ModelInfo
import com.localllm.app.inference.ModelLoadingState
import com.localllm.app.ui.components.ModelCard
import com.localllm.app.ui.components.DeviceCapabilityCard
import com.localllm.app.ui.viewmodel.ModelLibraryTab
import com.localllm.app.ui.viewmodel.ModelLibraryViewModel

/**
 * Model library screen for browsing, downloading, and loading models.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelLibraryScreen(
    onNavigateBack: () -> Unit,
    viewModel: ModelLibraryViewModel = hiltViewModel()
) {
    val downloadedModels by viewModel.downloadedModels.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()
    val modelLoadingState by viewModel.modelLoadingState.collectAsState()
    val currentModel by viewModel.currentModel.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf<ModelInfo?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Model Library") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshCatalog() },
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Device capabilities card
            DeviceCapabilityCard(
                deviceInfo = deviceInfo,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Storage info
            storageStats?.let { stats ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${stats.downloadedModelsCount} models downloaded",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = stats.formattedSize(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Tab row
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == ModelLibraryTab.DOWNLOADED,
                    onClick = { viewModel.setSelectedTab(ModelLibraryTab.DOWNLOADED) },
                    text = { Text("Downloaded (${downloadedModels.size})") }
                )
                Tab(
                    selected = selectedTab == ModelLibraryTab.AVAILABLE,
                    onClick = { viewModel.setSelectedTab(ModelLibraryTab.AVAILABLE) },
                    text = { Text("Available (${availableModels.size})") }
                )
            }
            
            // Error message
            errorMessage?.let { error ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss")
                        }
                    }
                }
            }
            
            // Model list
            val models = when (selectedTab) {
                ModelLibraryTab.DOWNLOADED -> downloadedModels
                ModelLibraryTab.AVAILABLE -> availableModels
            }
            
            if (models.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (selectedTab == ModelLibraryTab.DOWNLOADED) {
                                Icons.Default.FolderOff
                            } else {
                                Icons.Default.CloudOff
                            },
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTab == ModelLibraryTab.DOWNLOADED) {
                                "No downloaded models"
                            } else {
                                "No models available"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedTab == ModelLibraryTab.DOWNLOADED) {
                                "Browse available models to download"
                            } else {
                                "Pull to refresh the catalog"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(models, key = { it.id }) { model ->
                        ModelCard(
                            model = model,
                            downloadState = downloadStates[model.id],
                            isLoaded = currentModel?.id == model.id,
                            isLoading = modelLoadingState is ModelLoadingState.Loading && 
                                       currentModel?.id == model.id,
                            canRun = viewModel.canRunModel(model),
                            onDownload = { viewModel.downloadModel(model) },
                            onCancelDownload = { viewModel.cancelDownload(model.id) },
                            onDelete = { showDeleteDialog = model },
                            onLoad = { viewModel.loadModel(model) },
                            onUnload = { viewModel.unloadModel() }
                        )
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    showDeleteDialog?.let { model ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Model") },
            text = { 
                Text("Are you sure you want to delete ${model.name}? " +
                     "This will free ${model.formattedFileSize()} of storage.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteModel(model)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
