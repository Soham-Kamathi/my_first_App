package com.localllm.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.localllm.app.data.model.AppTheme
import com.localllm.app.data.model.AppearanceStyle
import com.localllm.app.data.model.GenerationConfig
import com.localllm.app.data.model.WebSearchProvider
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
                
                // Appearance Style selection (Default vs Nothing OS inspired)
                AppearanceStyleSelector(
                    currentStyle = userPreferences.appearanceStyle,
                    onStyleSelected = { viewModel.updateAppearanceStyle(it) }
                )
                
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
            
            // AI Features Section (NEW)
            SettingsSection(title = "AI Features") {
                SettingsSwitchItem(
                    title = "Thinking Mode",
                    subtitle = "Show AI reasoning with <think> tags",
                    icon = Icons.Default.Psychology,
                    checked = userPreferences.thinkingModeEnabled,
                    onCheckedChange = { viewModel.updateThinkingMode(it) }
                )
                
                SettingsSwitchItem(
                    title = "Web Search",
                    subtitle = "Search web before answering questions",
                    icon = Icons.Default.Search,
                    checked = userPreferences.webSearchEnabled,
                    onCheckedChange = { viewModel.updateWebSearch(it) }
                )
                
                // Web Search Configuration (shown when enabled)
                if (userPreferences.webSearchEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Search Provider Selection
                    var providerExpanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Search Provider",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = when (userPreferences.webSearchProvider) {
                                    WebSearchProvider.AUTO -> "Auto (Tavily if configured)"
                                    WebSearchProvider.TAVILY -> "Tavily API"
                                    WebSearchProvider.DUCKDUCKGO -> "DuckDuckGo"
                                    WebSearchProvider.WIKIPEDIA -> "Wikipedia"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        ExposedDropdownMenuBox(
                            expanded = providerExpanded,
                            onExpandedChange = { providerExpanded = !providerExpanded }
                        ) {
                            OutlinedTextField(
                                value = userPreferences.webSearchProvider.name,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .width(140.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = providerExpanded,
                                onDismissRequest = { providerExpanded = false }
                            ) {
                                WebSearchProvider.entries.forEach { provider ->
                                    DropdownMenuItem(
                                        text = { Text(provider.name) },
                                        onClick = {
                                            viewModel.updateWebSearchProvider(provider)
                                            providerExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Tavily API Key Input
                    var showApiKey by remember { mutableStateOf(false) }
                    var apiKeyText by remember(userPreferences.tavilyApiKey) { 
                        mutableStateOf(userPreferences.tavilyApiKey) 
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        OutlinedTextField(
                            value = apiKeyText,
                            onValueChange = { 
                                apiKeyText = it
                                viewModel.updateTavilyApiKey(it)
                            },
                            label = { Text("Tavily API Key") },
                            placeholder = { Text("tvly-...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Icon(
                                        imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showApiKey) "Hide" else "Show"
                                    )
                                }
                            }
                        )
                    }
                    
                    // API Key help text
                    Text(
                        text = "Get your free API key at tavily.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 56.dp, end = 16.dp, bottom = 8.dp)
                    )
                    
                    // Status indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(40.dp))
                        if (userPreferences.tavilyApiKey.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tavily API configured",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Using DuckDuckGo fallback (no API key needed)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
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
            
            // GPU/Hardware Acceleration Section
            SettingsSection(title = "Hardware Acceleration") {
                val hardwareProfile by viewModel.hardwareProfile.collectAsState()
                
                // Device capability info
                hardwareProfile?.let { profile ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Device: ${profile.deviceModel}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Chipset: ${profile.chipset}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "RAM: ${String.format("%.1f", profile.totalRamGB)} GB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Capability: ${profile.capabilityLevel.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = when(profile.capabilityLevel) {
                                    com.localllm.app.util.HardwareCapabilityDetector.CapabilityLevel.ULTRA -> MaterialTheme.colorScheme.primary
                                    com.localllm.app.util.HardwareCapabilityDetector.CapabilityLevel.HIGH -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            if (profile.hasVulkanSupport) {
                                Text(
                                    text = "✓ Vulkan GPU Supported",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                SettingsSwitchItem(
                    title = "GPU Acceleration",
                    subtitle = if (hardwareProfile?.hasVulkanSupport == true) 
                        "Offload layers to GPU (Vulkan)" 
                    else 
                        "Not available on this device",
                    icon = Icons.Default.Speed,
                    checked = userPreferences.gpuAccelerationEnabled,
                    onCheckedChange = { viewModel.updateGpuAcceleration(it) },
                    enabled = hardwareProfile?.hasVulkanSupport == true
                )
                
                if (userPreferences.gpuAccelerationEnabled && hardwareProfile?.hasVulkanSupport == true) {
                    SettingsSliderItem(
                        title = "GPU Layers",
                        value = userPreferences.gpuLayers.toFloat(),
                        valueRange = 0f..99f,
                        steps = 98,
                        valueFormat = { 
                            val v = it.toInt()
                            if (v == 0) "Auto (${hardwareProfile?.recommendedGpuLayers ?: 0})" 
                            else v.toString() 
                        },
                        onValueChange = { viewModel.updateGpuLayers(it.toInt()) }
                    )
                    
                    Text(
                        text = "Higher = more GPU usage. Recommended: ${hardwareProfile?.recommendedGpuLayers ?: 0} layers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
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
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    SettingsItem(
        title = title,
        subtitle = subtitle,
        icon = icon,
        onClick = { if (enabled) onCheckedChange(!checked) }
    ) {
        Switch(
            checked = checked,
            onCheckedChange = { if (enabled) onCheckedChange(it) },
            enabled = enabled
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

/**
 * Appearance Style Selector with visual preview cards
 * Allows users to switch between Default and Nothing OS-inspired themes
 */
@Composable
private fun AppearanceStyleSelector(
    currentStyle: AppearanceStyle,
    onStyleSelected: (AppearanceStyle) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Style,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Appearance Style",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Choose your preferred visual design",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Style selection cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Default Style Card
            StylePreviewCard(
                title = "Default",
                subtitle = "Modern & Vibrant",
                isSelected = currentStyle == AppearanceStyle.DEFAULT,
                onClick = { onStyleSelected(AppearanceStyle.DEFAULT) },
                previewColors = listOf(
                    androidx.compose.ui.graphics.Color(0xFF00BCD4), // Cyan primary
                    androidx.compose.ui.graphics.Color(0xFF121212), // Dark surface
                    androidx.compose.ui.graphics.Color(0xFF00E5FF)  // Accent
                ),
                modifier = Modifier.weight(1f)
            )
            
            // Nothing Style Card
            StylePreviewCard(
                title = "Nothing",
                subtitle = "Minimal & Bold",
                isSelected = currentStyle == AppearanceStyle.NOTHING,
                onClick = { onStyleSelected(AppearanceStyle.NOTHING) },
                previewColors = listOf(
                    androidx.compose.ui.graphics.Color(0xFFD92027), // Nothing Red
                    androidx.compose.ui.graphics.Color(0xFF000000), // True Black
                    androidx.compose.ui.graphics.Color(0xFFFFFFFF)  // White
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Style preview card showing a mini preview of the theme
 */
@Composable
private fun StylePreviewCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    previewColors: List<androidx.compose.ui.graphics.Color>,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }
    
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    
    Surface(
        modifier = modifier
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Mini preview area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        color = previewColors.getOrElse(1) { androidx.compose.ui.graphics.Color.Gray },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Accent color circle
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = previewColors.getOrElse(0) { androidx.compose.ui.graphics.Color.Cyan },
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                // Text preview lines
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(6.dp)
                            .background(
                                color = previewColors.getOrElse(2) { androidx.compose.ui.graphics.Color.White }.copy(alpha = 0.9f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(3.dp)
                            )
                    )
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(4.dp)
                            .background(
                                color = previewColors.getOrElse(2) { androidx.compose.ui.graphics.Color.White }.copy(alpha = 0.5f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
            
            // Title and selection indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
