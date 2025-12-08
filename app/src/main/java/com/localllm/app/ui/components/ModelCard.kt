package com.localllm.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localllm.app.data.model.DownloadState
import com.localllm.app.data.model.ModelInfo

/**
 * Card component for displaying model information with download/load actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelCard(
    model: ModelInfo,
    downloadState: DownloadState?,
    isLoaded: Boolean,
    isLoading: Boolean,
    canRun: Boolean,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDelete: () -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val effectiveDownloadState = downloadState ?: if (model.isDownloaded) {
        DownloadState.Completed(model.localPath ?: "")
    } else {
        DownloadState.NotStarted
    }
    
    // Enhanced card with gradient border
    val cardGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E1E1E),
            Color(0xFF252525)
        )
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.background(cardGradient)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = model.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.White
                            )
                            if (isLoaded) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = Color(0xFF00E5FF).copy(alpha = 0.2f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(Color(0xFF00E5FF))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "LOADED",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = Color(0xFF00E5FF)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${model.parameterCount} • ${model.quantization}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB0B0B0)
                        )
                    }
                    
                    // Compatibility indicator
                    if (!canRun && !model.isDownloaded) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = Color(0xFFFF1744).copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFFFF1744)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Low RAM",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color(0xFFFF1744)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Description
                Text(
                    text = model.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0B0B0),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Enhanced stats row with gradient chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EnhancedModelStat(
                        icon = Icons.Default.Storage,
                        value = model.formattedFileSize()
                    )
                    EnhancedModelStat(
                        icon = Icons.Default.Memory,
                        value = model.formattedRamRequirement()
                    )
                    EnhancedModelStat(
                        icon = Icons.Default.TextFields,
                        value = "${model.contextLength} ctx"
                    )
                }
                
                // Tags
                if (model.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        model.tags.take(3).forEach { tag ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = Color(0xFF00E5FF).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = Color(0xFF00E5FF)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Download progress or action buttons
                when (effectiveDownloadState) {
                    is DownloadState.Downloading -> {
                        Column {
                            LinearProgressIndicator(
                                progress = effectiveDownloadState.progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(MaterialTheme.shapes.small),
                                color = Color(0xFF00E5FF),
                                trackColor = Color(0xFF1E1E1E)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = effectiveDownloadState.formattedProgress(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${effectiveDownloadState.formattedSpeed()} • ${effectiveDownloadState.estimatedTimeRemaining()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFB0B0B0)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = onCancelDownload,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFFF1744)
                                )
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cancel")
                            }
                        }
                    }
                
                is DownloadState.Verifying -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying download...")
                    }
                }
                
                is DownloadState.Error -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = effectiveDownloadState.message,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            if (effectiveDownloadState.isResumable) {
                                TextButton(onClick = onDownload) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
                    
                    is DownloadState.Completed -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isLoaded) {
                                OutlinedButton(
                                    onClick = onUnload,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF00E5FF)
                                    )
                                ) {
                                    Icon(Icons.Default.Eject, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Unload", fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Button(
                                    onClick = onLoad,
                                    enabled = canRun && !isLoading,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00E5FF),
                                        contentColor = Color.Black
                                    )
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.Black
                                        )
                                    } else {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (isLoading) "Loading..." else "Load",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFFF1744),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                
                is DownloadState.Paused -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Resume")
                        }
                        OutlinedButton(onClick = onCancelDownload) {
                            Text("Cancel")
                        }
                    }
                }
                    
                    else -> {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E5FF),
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download (${model.formattedFileSize()})", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// Enhanced stat chip with gradient background
@Composable
private fun EnhancedModelStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = Color(0xFF00E5FF).copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF00E5FF)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFF00E5FF)
            )
        }
    }
}

@Composable
private fun ModelStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
