package com.localllm.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.localllm.app.data.model.AppTheme
import com.localllm.app.data.model.GenerationConfig
import com.localllm.app.ui.viewmodel.SettingsViewModel

/**
 * Settings screen for configuring app preferences and inference parameters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val userPreferences by viewModel.userPreferences.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()
    val deviceSummary by viewModel.deviceSummary.collectAsState()
    
    var showSystemPromptDialog by remember { mutableStateOf(false) }
    var showDeviceInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Appearance Section
            SettingsSection(title = "Appearance") {
                // Theme selection
                SettingsItem(
                    title = "Theme",
                    subtitle = userPreferences.theme.name.lowercase().replaceFirstChar { it.uppercase() },
                    icon = Icons.Default.Palette
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = userPreferences.theme.name.lowercase().replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .width(150.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            AppTheme.entries.forEach { theme ->
                                DropdownMenuItem(
                                    text = { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        viewModel.updateTheme(theme)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                SettingsSwitchItem(
                    title = "Keep screen on during generation",
                    subtitle = "Prevent screen from turning off",
                    icon = Icons.Default.ScreenLockPortrait,
                    checked = userPreferences.keepScreenOnDuringGeneration,
                    onCheckedChange = { viewModel.updateKeepScreenOn(it) }
                )
                
                SettingsSwitchItem(
                    title = "Show tokens per second",
                    subtitle = "Display generation speed",
                    icon = Icons.Default.Speed,
                    checked = userPreferences.showTokensPerSecond,
                    onCheckedChange = { viewModel.updateShowTokensPerSecond(it) }
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Generation Section
            SettingsSection(title = "Generation") {
                // Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { viewModel.applyCreativePreset() },
                        label = { Text("Creative") },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp)) }
                    )
                    AssistChip(
                        onClick = { viewModel.applyPrecisePreset() },
                        label = { Text("Precise") },
                        leadingIcon = { Icon(Icons.Default.Psychology, null, Modifier.size(18.dp)) }
                    )
                    AssistChip(
                        onClick = { viewModel.applyCodePreset() },
                        label = { Text("Code") },
                        leadingIcon = { Icon(Icons.Default.Code, null, Modifier.size(18.dp)) }
                    )
                }
                
                // Temperature slider
                SettingsSliderItem(
                    title = "Temperature",
                    value = userPreferences.defaultGenerationConfig.temperature,
                    valueRange = 0f..2f,
                    steps = 19,
                    valueFormat = { String.format("%.1f", it) },
                    onValueChange = { viewModel.updateTemperature(it) }
                )
                
                // Max tokens slider
                SettingsSliderItem(
                    title = "Max Tokens",
                    value = userPreferences.defaultGenerationConfig.maxTokens.toFloat(),
                    valueRange = 64f..2048f,
                    steps = 30,
                    valueFormat = { it.toInt().toString() },
                    onValueChange = { viewModel.updateMaxTokens(it.toInt()) }
                )
                
                // Top P slider
                SettingsSliderItem(
                    title = "Top P",
                    value = userPreferences.defaultGenerationConfig.topP,
                    valueRange = 0f..1f,
                    steps = 19,
                    valueFormat = { String.format("%.2f", it) },
                    onValueChange = { viewModel.updateTopP(it) }
                )
                
                // Top K slider
                SettingsSliderItem(
                    title = "Top K",
                    value = userPreferences.defaultGenerationConfig.topK.toFloat(),
                    valueRange = 1f..100f,
                    steps = 98,
                    valueFormat = { it.toInt().toString() },
                    onValueChange = { viewModel.updateTopK(it.toInt()) }
                )
                
                // Repeat penalty slider
                SettingsSliderItem(
                    title = "Repeat Penalty",
                    value = userPreferences.defaultGenerationConfig.repeatPenalty,
                    valueRange = 1f..2f,
                    steps = 19,
                    valueFormat = { String.format("%.2f", it) },
                    onValueChange = { viewModel.updateRepeatPenalty(it) }
                )
                
                // Reset button
                TextButton(
                    onClick = { viewModel.resetGenerationConfig() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reset to Defaults")
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // System Prompt Section
            SettingsSection(title = "System Prompt") {
                SettingsItem(
                    title = "Default System Prompt",
                    subtitle = userPreferences.defaultSystemPrompt.take(50) + "...",
                    icon = Icons.Default.Description,
                    onClick = { showSystemPromptDialog = true }
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Performance Section
            SettingsSection(title = "Performance") {
                // Thread count
                SettingsItem(
                    title = "Thread Count",
                    subtitle = if (userPreferences.threadCount == 0) "Auto (${viewModel.getOptimalThreadCount()})" 
                              else userPreferences.threadCount.toString(),
                    icon = Icons.Default.Memory
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = if (userPreferences.threadCount == 0) "Auto" 
                                   else userPreferences.threadCount.toString(),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .width(100.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Auto") },
                                onClick = {
                                    viewModel.updateThreadCount(0)
                                    expanded = false
                                }
                            )
                            viewModel.getThreadCountOptions().forEach { count ->
                                DropdownMenuItem(
                                    text = { Text(count.toString()) },
                                    onClick = {
                                        viewModel.updateThreadCount(count)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                if (viewModel.isNNAPISupported()) {
                    SettingsSwitchItem(
                        title = "Use NNAPI",
                        subtitle = "Hardware acceleration (experimental)",
                        icon = Icons.Default.Bolt,
                        checked = userPreferences.useNNAPI,
                        onCheckedChange = { viewModel.updateNNAPIEnabled(it) }
                    )
                }
                
                SettingsSwitchItem(
                    title = "Use Memory Mapping",
                    subtitle = "Faster model loading",
                    icon = Icons.Default.Storage,
                    checked = userPreferences.useMmap,
                    onCheckedChange = { viewModel.updateMmapEnabled(it) }
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Downloads Section
            SettingsSection(title = "Downloads") {
                SettingsSwitchItem(
                    title = "Download on Wi-Fi only",
                    subtitle = "Save mobile data",
                    icon = Icons.Default.Wifi,
                    checked = userPreferences.downloadOnWifiOnly,
                    onCheckedChange = { viewModel.updateDownloadOnWifiOnly(it) }
                )
                
                SettingsSwitchItem(
                    title = "Auto-load last model",
                    subtitle = "Load previous model on startup",
                    icon = Icons.Default.Autorenew,
                    checked = userPreferences.autoLoadLastModel,
                    onCheckedChange = { viewModel.updateAutoLoadLastModel(it) }
                )
                
                storageStats?.let { stats ->
                    SettingsItem(
                        title = "Storage Used",
                        subtitle = "${stats.downloadedModelsCount} models • ${stats.formattedSize()}",
                        icon = Icons.Default.Folder
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // About Section
            SettingsSection(title = "About") {
                SettingsItem(
                    title = "Device Info",
                    subtitle = "View hardware capabilities",
                    icon = Icons.Default.PhoneAndroid,
                    onClick = { showDeviceInfoDialog = true }
                )
                
                SettingsItem(
                    title = "Version",
                    subtitle = "1.0.0",
                    icon = Icons.Default.Info
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // System Prompt Dialog
    if (showSystemPromptDialog) {
        var promptText by remember { mutableStateOf(userPreferences.defaultSystemPrompt) }
        
        AlertDialog(
            onDismissRequest = { showSystemPromptDialog = false },
            title = { Text("System Prompt") },
            text = {
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    placeholder = { Text("Enter system prompt...") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateSystemPrompt(promptText)
                    showSystemPromptDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSystemPromptDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Device Info Dialog
    if (showDeviceInfoDialog) {
        AlertDialog(
            onDismissRequest = { showDeviceInfoDialog = false },
            title = { Text("Device Information") },
            text = {
                Text(
                    text = deviceSummary,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeviceInfoDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            trailing?.invoke()
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsItem(
        title = title,
        subtitle = subtitle,
        icon = icon,
        onClick = { onCheckedChange(!checked) }
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsSliderItem(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueFormat: (Float) -> String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                valueFormat(value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}
