package com.localllm.app.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localllm.app.inference.ModelLoadingState
import com.localllm.app.ui.viewmodel.CodeAction
import com.localllm.app.ui.viewmodel.CodeCompanionViewModel
import com.localllm.app.ui.viewmodel.CodeLanguage

/**
 * Code Companion Screen
 * Provides code explanation, debugging, optimization, and conversion
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeCompanionScreen(
    viewModel: CodeCompanionViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToModels: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val loadingState by viewModel.loadingState.collectAsState()
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showTargetLanguageMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Code Companion") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState.code.isNotEmpty() || uiState.result.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearAll() }) {
                            Icon(Icons.Default.DeleteSweep, "Clear all")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Model status
            when (loadingState) {
                is ModelLoadingState.NotLoaded -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "No model loaded",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    "Load a model to use Code Companion",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            TextButton(onClick = onNavigateToModels) {
                                Text("Select")
                            }
                        }
                    }
                }
                is ModelLoadingState.Loading -> {
                    LinearProgressIndicator(
                        progress = (loadingState as ModelLoadingState.Loading).progress,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> {}
            }

            // Error message
            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = error,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Default.Close, "Dismiss")
                        }
                    }
                }
            }

            // Language selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Language:",
                    style = MaterialTheme.typography.labelLarge
                )

                Box {
                    FilterChip(
                        selected = true,
                        onClick = { showLanguageMenu = true },
                        label = { Text(uiState.selectedLanguage.displayName) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false }
                    ) {
                        CodeLanguage.entries.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(language.displayName) },
                                onClick = {
                                    viewModel.updateLanguage(language)
                                    showLanguageMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Code input area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .heightIn(min = 150.dp, max = 300.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Code Input",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.code.isNotEmpty()) {
                            TextButton(onClick = { viewModel.updateCode("") }) {
                                Text("Clear")
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        BasicTextField(
                            value = uiState.code,
                            onValueChange = { viewModel.updateCode(it) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (uiState.code.isEmpty()) {
                                        Text(
                                            "Paste or type your code here...",
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Action buttons
            Text(
                "Actions",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            // Action buttons grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CodeAction.entries.forEach { action ->
                    val isConvert = action == CodeAction.CONVERT

                    if (isConvert) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ActionButton(
                                action = action,
                                isGenerating = uiState.isGenerating,
                                isSelected = uiState.selectedAction == action,
                                enabled = viewModel.isModelLoaded && !uiState.isGenerating,
                                onClick = { viewModel.performAction(action) }
                            )

                            Spacer(Modifier.height(4.dp))

                            // Target language selector for Convert action
                            Box {
                                TextButton(
                                    onClick = { showTargetLanguageMenu = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        "→ ${uiState.targetLanguage.displayName}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }

                                DropdownMenu(
                                    expanded = showTargetLanguageMenu,
                                    onDismissRequest = { showTargetLanguageMenu = false }
                                ) {
                                    CodeLanguage.entries.forEach { language ->
                                        DropdownMenuItem(
                                            text = { Text(language.displayName) },
                                            onClick = {
                                                viewModel.updateTargetLanguage(language)
                                                showTargetLanguageMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        ActionButton(
                            action = action,
                            isGenerating = uiState.isGenerating,
                            isSelected = uiState.selectedAction == action,
                            enabled = viewModel.isModelLoaded && !uiState.isGenerating,
                            onClick = { viewModel.performAction(action) }
                        )
                    }
                }
            }

            // Stop button when generating
            if (uiState.isGenerating) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.stopGeneration() },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Stop Generation")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Result area
            if (uiState.result.isNotEmpty() || uiState.isGenerating) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .heightIn(min = 200.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    uiState.selectedAction?.displayName ?: "Result",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (uiState.isGenerating) {
                                    Spacer(Modifier.width(8.dp))
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }

                            if (uiState.result.isNotEmpty() && !uiState.isGenerating) {
                                IconButton(onClick = { viewModel.clearResult() }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear result",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Divider()

                        SelectionContainer {
                            Text(
                                text = uiState.result.ifEmpty { "Generating..." },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                                    .horizontalScroll(rememberScrollState()),
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ActionButton(
    action: CodeAction,
    isGenerating: Boolean,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val icon: ImageVector = when (action) {
        CodeAction.EXPLAIN -> Icons.Default.Lightbulb
        CodeAction.DEBUG -> Icons.Default.BugReport
        CodeAction.OPTIMIZE -> Icons.Default.Speed
        CodeAction.CONVERT -> Icons.Default.SwapHoriz
        CodeAction.DOCUMENT -> Icons.Default.Description
        CodeAction.TEST -> Icons.Default.Science
        CodeAction.REFACTOR -> Icons.Default.Construction
    }

    ElevatedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = if (isSelected && isGenerating) {
            ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            ButtonDefaults.elevatedButtonColors()
        }
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(action.displayName, style = MaterialTheme.typography.labelMedium)
    }
}
