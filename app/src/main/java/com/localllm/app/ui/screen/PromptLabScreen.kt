package com.localllm.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.localllm.app.ui.viewmodel.PromptLabViewModel

/**
 * Prompt template data class
 */
data class PromptTemplate(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val category: String,
    val template: String,
    val placeholders: List<String> = emptyList()
)

/**
 * Prompt Lab Screen - Sandbox for experimenting with AI prompts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptLabScreen(
    onNavigateBack: () -> Unit,
    onNavigateToModels: () -> Unit,
    viewModel: PromptLabViewModel = hiltViewModel()
) {
    val prompt by viewModel.prompt.collectAsState()
    val output by viewModel.output.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val temperature by viewModel.temperature.collectAsState()
    val maxTokens by viewModel.maxTokens.collectAsState()
    val selectedTemplate by viewModel.selectedTemplate.collectAsState()
    val isModelLoaded by viewModel.isModelLoaded.collectAsState()
    
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    
    var showTemplates by remember { mutableStateOf(false) }
    var showParameters by remember { mutableStateOf(false) }
    
    val templates = remember { getPromptTemplates() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Prompt Lab",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showParameters = !showParameters }) {
                        Icon(
                            Icons.Outlined.Tune,
                            contentDescription = "Parameters",
                            tint = if (showParameters) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.onSurface
                        )
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
            // Parameters Panel (collapsible)
            AnimatedVisibility(visible = showParameters) {
                ParametersPanel(
                    temperature = temperature,
                    maxTokens = maxTokens,
                    onTemperatureChange = viewModel::setTemperature,
                    onMaxTokensChange = viewModel::setMaxTokens
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Template selector
                TemplateSelector(
                    templates = templates,
                    selectedTemplate = selectedTemplate,
                    onTemplateClick = { template ->
                        viewModel.selectTemplate(template)
                        showTemplates = false
                    },
                    expanded = showTemplates,
                    onExpandChange = { showTemplates = it }
                )
                
                // Prompt Input
                PromptInputSection(
                    prompt = prompt,
                    onPromptChange = viewModel::setPrompt,
                    onClear = viewModel::clearPrompt,
                    enabled = !isGenerating
                )
                
                // Action Buttons
                ActionButtonsRow(
                    isGenerating = isGenerating,
                    isModelLoaded = isModelLoaded,
                    onGenerate = {
                        focusManager.clearFocus()
                        viewModel.generate()
                    },
                    onStop = viewModel::stopGeneration,
                    onClear = viewModel::clearAll
                )
                
                // Output Section
                if (output.isNotEmpty() || isGenerating) {
                    OutputSection(
                        output = output,
                        isGenerating = isGenerating,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(output))
                        }
                    )
                }
                
                // Model not loaded warning
                if (!isModelLoaded) {
                    NoModelWarning()
                }
            }
        }
    }
}

@Composable
private fun ParametersPanel(
    temperature: Float,
    maxTokens: Int,
    onTemperatureChange: (Float) -> Unit,
    onMaxTokensChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Generation Parameters",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            // Temperature slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Temperature",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = String.format("%.2f", temperature),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = temperature,
                    onValueChange = onTemperatureChange,
                    valueRange = 0f..2f,
                    steps = 19
                )
                Text(
                    text = "Lower = more focused, Higher = more creative",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Max tokens slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Max Tokens",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = maxTokens.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = maxTokens.toFloat(),
                    onValueChange = { onMaxTokensChange(it.toInt()) },
                    valueRange = 64f..2048f,
                    steps = 30
                )
                Text(
                    text = "Maximum length of generated response",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TemplateSelector(
    templates: List<PromptTemplate>,
    selectedTemplate: PromptTemplate?,
    onTemplateClick: (PromptTemplate) -> Unit,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit
) {
    val categories = templates.groupBy { it.category }
    
    Column {
        // Selected template or selector button
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandChange(!expanded) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = selectedTemplate?.icon ?: Icons.Outlined.TextFields,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = selectedTemplate?.name ?: "Free-form Prompt",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = selectedTemplate?.description ?: "Write any prompt you want",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand"
                )
            }
        }
        
        // Template list
        AnimatedVisibility(visible = expanded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    // Free-form option
                    TemplateItem(
                        template = null,
                        isSelected = selectedTemplate == null,
                        onClick = { onTemplateClick(PromptTemplate(
                            id = "free",
                            name = "Free-form",
                            description = "Write any prompt",
                            icon = Icons.Outlined.TextFields,
                            category = "General",
                            template = ""
                        )) }
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    categories.forEach { (category, categoryTemplates) ->
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                        )
                        
                        categoryTemplates.forEach { template ->
                            TemplateItem(
                                template = template,
                                isSelected = selectedTemplate?.id == template.id,
                                onClick = { onTemplateClick(template) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateItem(
    template: PromptTemplate?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = template?.icon ?: Icons.Outlined.TextFields,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = template?.name ?: "Free-form",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = template?.description ?: "Write any prompt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PromptInputSection(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onClear: () -> Unit,
    enabled: Boolean
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Prompt",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (prompt.isNotEmpty()) {
                TextButton(onClick = onClear) {
                    Icon(
                        Icons.Outlined.Clear,
                        contentDescription = "Clear",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }
            }
        }
        
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp),
            enabled = enabled,
            placeholder = {
                Text("Enter your prompt here...\n\nTry: \"Summarize the following text:\"\nor \"Write a Python function that...\"")
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            shape = RoundedCornerShape(12.dp)
        )
        
        Text(
            text = "${prompt.length} characters",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ActionButtonsRow(
    isGenerating: Boolean,
    isModelLoaded: Boolean,
    onGenerate: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isGenerating) {
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop")
            }
        } else {
            Button(
                onClick = onGenerate,
                modifier = Modifier.weight(1f),
                enabled = isModelLoaded
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate")
            }
        }
        
        OutlinedButton(
            onClick = onClear,
            enabled = !isGenerating
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = "Reset")
        }
    }
}

@Composable
private fun OutputSection(
    output: String,
    isGenerating: Boolean,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Output",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                if (output.isNotEmpty() && !isGenerating) {
                    IconButton(onClick = onCopy) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            SelectionContainer {
                Text(
                    text = output.ifEmpty { "Generating..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (output.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun NoModelWarning() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Column {
                Text(
                    text = "No Model Loaded",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Please load a model from the Models screen to use Prompt Lab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/**
 * Get predefined prompt templates
 */
private fun getPromptTemplates(): List<PromptTemplate> = listOf(
    // Text Processing
    PromptTemplate(
        id = "summarize",
        name = "Summarize",
        description = "Condense text into key points",
        icon = Icons.Outlined.Compress,
        category = "Text Processing",
        template = "Please summarize the following text in a concise manner, highlighting the key points:\n\n{text}"
    ),
    PromptTemplate(
        id = "rewrite",
        name = "Rewrite",
        description = "Rephrase text in different style",
        icon = Icons.Outlined.Edit,
        category = "Text Processing",
        template = "Please rewrite the following text to be more {style}:\n\n{text}"
    ),
    PromptTemplate(
        id = "expand",
        name = "Expand",
        description = "Elaborate on given text",
        icon = Icons.Outlined.Expand,
        category = "Text Processing",
        template = "Please expand on the following text with more details and explanations:\n\n{text}"
    ),
    PromptTemplate(
        id = "proofread",
        name = "Proofread",
        description = "Fix grammar and spelling",
        icon = Icons.Outlined.Spellcheck,
        category = "Text Processing",
        template = "Please proofread the following text, fix any grammar or spelling errors, and improve clarity:\n\n{text}"
    ),
    
    // Code
    PromptTemplate(
        id = "code_gen",
        name = "Generate Code",
        description = "Write code from description",
        icon = Icons.Outlined.Code,
        category = "Coding",
        template = "Write a {language} function that {description}. Include comments explaining the code."
    ),
    PromptTemplate(
        id = "code_explain",
        name = "Explain Code",
        description = "Explain what code does",
        icon = Icons.Outlined.Help,
        category = "Coding",
        template = "Please explain what the following code does, step by step:\n\n```\n{code}\n```"
    ),
    PromptTemplate(
        id = "code_debug",
        name = "Debug Code",
        description = "Find and fix bugs",
        icon = Icons.Outlined.BugReport,
        category = "Coding",
        template = "Please analyze the following code for bugs and suggest fixes:\n\n```\n{code}\n```\n\nThe issue I'm experiencing: {issue}"
    ),
    PromptTemplate(
        id = "code_convert",
        name = "Convert Code",
        description = "Translate between languages",
        icon = Icons.Outlined.Transform,
        category = "Coding",
        template = "Please convert the following {source_lang} code to {target_lang}:\n\n```\n{code}\n```"
    ),
    
    // Creative
    PromptTemplate(
        id = "story",
        name = "Write Story",
        description = "Generate creative stories",
        icon = Icons.Outlined.AutoStories,
        category = "Creative",
        template = "Write a short {genre} story about {topic}. Make it engaging and creative."
    ),
    PromptTemplate(
        id = "email",
        name = "Write Email",
        description = "Draft professional emails",
        icon = Icons.Outlined.Email,
        category = "Creative",
        template = "Write a {tone} email to {recipient} about {topic}. Keep it professional and clear."
    ),
    
    // Analysis
    PromptTemplate(
        id = "analyze",
        name = "Analyze",
        description = "Deep analysis of content",
        icon = Icons.Outlined.Analytics,
        category = "Analysis",
        template = "Please provide a detailed analysis of the following:\n\n{content}\n\nConsider: key themes, implications, and insights."
    ),
    PromptTemplate(
        id = "compare",
        name = "Compare",
        description = "Compare two or more items",
        icon = Icons.Outlined.CompareArrows,
        category = "Analysis",
        template = "Please compare and contrast the following:\n\n1. {item1}\n2. {item2}\n\nHighlight similarities, differences, and which might be better for different use cases."
    )
)
