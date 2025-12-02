package com.localllm.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.localllm.app.data.model.DeviceInfo

/**
 * Card showing device hardware capabilities and recommendations.
 */
@Composable
fun DeviceCapabilityCard(
    deviceInfo: DeviceInfo,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Device Capabilities",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "${deviceInfo.availableRamMb}MB available of ${deviceInfo.totalRamMb}MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                
                // CPU Info
                DeviceInfoRow(
                    icon = Icons.Default.Memory,
                    label = "CPU",
                    value = "${deviceInfo.cpuCores} cores (${deviceInfo.cpuArchitecture})"
                )
                
                // GPU Info
                deviceInfo.gpuInfo?.let { gpu ->
                    DeviceInfoRow(
                        icon = Icons.Default.Gradient,
                        label = "GPU",
                        value = gpu.renderer
                    )
                }
                
                // Storage
                DeviceInfoRow(
                    icon = Icons.Default.Storage,
                    label = "Storage",
                    value = buildString {
                        append("${deviceInfo.internalStorageAvailableMb / 1024}GB available")
                        deviceInfo.externalStorageAvailableMb?.let {
                            append(" (+${it / 1024}GB external)")
                        }
                    }
                )
                
                // Features
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (deviceInfo.supportsNNAPI) {
                        FeatureChip("NNAPI", true)
                    }
                    if (deviceInfo.supportsVulkan) {
                        FeatureChip("Vulkan", true)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Recommendation
                val recommendedSizes = deviceInfo.recommendedModelSizes()
                if (recommendedSizes.isNotEmpty()) {
                    Text(
                        text = "Recommended: ${recommendedSizes.joinToString(", ")} parameter models",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeatureChip(
    name: String,
    supported: Boolean
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (supported) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (supported) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = if (supported) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
