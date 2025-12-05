package com.localllm.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localllm.app.inference.InferenceEngine
import com.localllm.app.inference.ModelLoadingState
import com.localllm.app.inference.ModelManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Supported programming languages
 */
enum class CodeLanguage(val displayName: String, val extension: String) {
    KOTLIN("Kotlin", "kt"),
    JAVA("Java", "java"),
    PYTHON("Python", "py"),
    JAVASCRIPT("JavaScript", "js"),
    TYPESCRIPT("TypeScript", "ts"),
    CPP("C++", "cpp"),
    C("C", "c"),
    CSHARP("C#", "cs"),
    RUST("Rust", "rs"),
    GO("Go", "go"),
    SWIFT("Swift", "swift"),
    HTML("HTML", "html"),
    CSS("CSS", "css"),
    SQL("SQL", "sql"),
    JSON("JSON", "json"),
    XML("XML", "xml"),
    SHELL("Shell", "sh"),
    OTHER("Other", "txt")
}

/**
 * Code actions available in Code Companion
 */
enum class CodeAction(val displayName: String, val description: String) {
    EXPLAIN("Explain", "Get a detailed explanation of the code"),
    DEBUG("Debug", "Find bugs and potential issues"),
    OPTIMIZE("Optimize", "Improve performance and efficiency"),
    CONVERT("Convert", "Convert to another language"),
    DOCUMENT("Document", "Add comments and documentation"),
    TEST("Test", "Generate unit tests"),
    REFACTOR("Refactor", "Improve code structure")
}

/**
 * UI State for Code Companion
 */
data class CodeCompanionUiState(
    val code: String = "",
    val selectedLanguage: CodeLanguage = CodeLanguage.KOTLIN,
    val targetLanguage: CodeLanguage = CodeLanguage.PYTHON,
    val result: String = "",
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val selectedAction: CodeAction? = null
)

/**
 * ViewModel for Code Companion feature
 * Provides code explanation, debugging, optimization, and conversion
 */
@HiltViewModel
class CodeCompanionViewModel @Inject constructor(
    private val inferenceEngine: InferenceEngine,
    private val modelManager: ModelManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CodeCompanionUiState())
    val uiState: StateFlow<CodeCompanionUiState> = _uiState.asStateFlow()

    val loadingState: StateFlow<ModelLoadingState> = modelManager.loadingState

    val isModelLoaded: Boolean
        get() = modelManager.isModelLoaded

    private var generationJob: Job? = null

    /**
     * Update code input
     */
    fun updateCode(code: String) {
        _uiState.value = _uiState.value.copy(code = code)
    }

    /**
     * Update source language
     */
    fun updateLanguage(language: CodeLanguage) {
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
    }

    /**
     * Update target language for conversion
     */
    fun updateTargetLanguage(language: CodeLanguage) {
        _uiState.value = _uiState.value.copy(targetLanguage = language)
    }

    /**
     * Perform a code action
     */
    fun performAction(action: CodeAction) {
        val code = _uiState.value.code.trim()
        if (code.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter some code first")
            return
        }

        _uiState.value = _uiState.value.copy(
            isGenerating = true,
            errorMessage = null,
            selectedAction = action,
            result = ""
        )

        generationJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val prompt = buildPromptForAction(action, code)
                val resultBuilder = StringBuilder()

                inferenceEngine.generateStream(
                    prompt = prompt,
                    onTokenGenerated = { token ->
                        resultBuilder.append(token)
                        _uiState.value = _uiState.value.copy(result = resultBuilder.toString())
                    }
                )

                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    result = resultBuilder.toString()
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }

    private fun buildPromptForAction(action: CodeAction, code: String): String {
        val language = _uiState.value.selectedLanguage.displayName
        val targetLang = _uiState.value.targetLanguage.displayName

        return when (action) {
            CodeAction.EXPLAIN -> """You are an expert programmer. Explain the following $language code in detail.

Explain:
1. What the code does overall
2. How each section works
3. Key concepts used
4. Any potential issues

CODE:
```$language
$code
```

EXPLANATION:"""

            CodeAction.DEBUG -> """You are an expert code reviewer. Analyze the following $language code for bugs, issues, and potential improvements.

Look for:
1. Bugs and logical errors
2. Security vulnerabilities
3. Performance issues
4. Edge cases not handled
5. Best practice violations

CODE:
```$language
$code
```

ANALYSIS:"""

            CodeAction.OPTIMIZE -> """You are an expert programmer specializing in performance optimization. Optimize the following $language code for better performance and efficiency.

Consider:
1. Time complexity improvements
2. Memory usage optimization
3. Code simplification
4. Better algorithms

Original CODE:
```$language
$code
```

Provide the optimized code with explanations:

OPTIMIZED CODE:"""

            CodeAction.CONVERT -> """You are an expert programmer fluent in multiple languages. Convert the following $language code to $targetLang.

Maintain the same functionality while following $targetLang best practices and idioms.

SOURCE CODE ($language):
```$language
$code
```

CONVERTED CODE ($targetLang):"""

            CodeAction.DOCUMENT -> """You are an expert programmer. Add comprehensive documentation comments to the following $language code.

Include:
1. Function/method documentation
2. Parameter descriptions
3. Return value documentation
4. Inline comments for complex logic
5. Usage examples where appropriate

CODE:
```$language
$code
```

DOCUMENTED CODE:"""

            CodeAction.TEST -> """You are an expert software tester. Generate comprehensive unit tests for the following $language code.

Include tests for:
1. Normal/expected behavior
2. Edge cases
3. Error handling
4. Boundary conditions

CODE:
```$language
$code
```

UNIT TESTS:"""

            CodeAction.REFACTOR -> """You are an expert programmer specializing in clean code and design patterns. Refactor the following $language code to improve its structure, readability, and maintainability.

Apply:
1. SOLID principles
2. Appropriate design patterns
3. Clean code practices
4. Better naming conventions
5. Code organization

Original CODE:
```$language
$code
```

REFACTORED CODE:"""
        }
    }

    /**
     * Stop ongoing generation
     */
    fun stopGeneration() {
        generationJob?.cancel()
        inferenceEngine.cancelGeneration()
        _uiState.value = _uiState.value.copy(isGenerating = false)
    }

    /**
     * Clear result
     */
    fun clearResult() {
        _uiState.value = _uiState.value.copy(result = "", selectedAction = null)
    }

    /**
     * Dismiss error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Clear all input and output
     */
    fun clearAll() {
        generationJob?.cancel()
        _uiState.value = CodeCompanionUiState()
    }
}
