package com.localllm.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.localllm.app.ui.screen.AskImageScreen
import com.localllm.app.ui.screen.AudioScribeScreen
import com.localllm.app.ui.screen.ChatScreen
import com.localllm.app.ui.screen.CodeCompanionScreen
import com.localllm.app.ui.screen.ConversationHistoryScreen
import com.localllm.app.ui.screen.ConversationTemplatesScreen
import com.localllm.app.ui.screen.DocumentChatScreen
import com.localllm.app.ui.screen.FlashcardScreen
import com.localllm.app.ui.screen.HomeScreen
import com.localllm.app.ui.screen.ModelLibraryScreen
import com.localllm.app.ui.screen.PromptLabScreen
import com.localllm.app.ui.screen.QuizScreen
import com.localllm.app.ui.screen.SettingsScreen
import com.localllm.app.ui.viewmodel.AskImageViewModel
import com.localllm.app.ui.viewmodel.AudioScribeViewModel
import com.localllm.app.ui.viewmodel.ChatViewModel
import com.localllm.app.ui.viewmodel.CodeCompanionViewModel
import com.localllm.app.ui.viewmodel.ConversationHistoryViewModel
import com.localllm.app.ui.viewmodel.ConversationTemplatesViewModel
import com.localllm.app.ui.viewmodel.DocumentChatViewModel
import com.localllm.app.ui.viewmodel.FlashcardViewModel
import com.localllm.app.ui.viewmodel.HomeViewModel
import com.localllm.app.ui.viewmodel.ModelLibraryViewModel
import com.localllm.app.ui.viewmodel.PromptLabViewModel
import com.localllm.app.ui.viewmodel.QuizViewModel
import com.localllm.app.ui.viewmodel.SettingsViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object NewChat : Screen("new_chat")
    object History : Screen("history")
    object Library : Screen("library")
    object Chat : Screen("chat?conversationId={conversationId}&template={template}") {
        fun createRoute(conversationId: String? = null, templatePrompt: String? = null): String {
            val params = mutableListOf<String>()
            if (conversationId != null) params.add("conversationId=$conversationId")
            if (templatePrompt != null) params.add("template=${java.net.URLEncoder.encode(templatePrompt, "UTF-8")}")
            return if (params.isEmpty()) "chat" else "chat?${params.joinToString("&")}"
        }
    }
    object PromptLab : Screen("prompt_lab")
    object AskImage : Screen("ask_image")
    object AudioScribe : Screen("audio_scribe")
    object DocumentChat : Screen("document_chat")
    object CodeCompanion : Screen("code_companion")
    object ConversationTemplates : Screen("conversation_templates")
    object Flashcards : Screen("flashcards")
    object Quiz : Screen("quiz")
    object ModelLibrary : Screen("model_library")
    object Settings : Screen("settings")
    object ConversationHistory : Screen("conversation_history")
}

@Composable
fun LocalLLMNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        // Home - Feature Hub
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            
            HomeScreen(
                viewModel = viewModel,
                onNavigateToChat = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId))
                },
                onNavigateToPromptLab = {
                    navController.navigate(Screen.PromptLab.route)
                },
                onNavigateToAskImage = {
                    navController.navigate(Screen.AskImage.route)
                },
                onNavigateToAudioScribe = {
                    navController.navigate(Screen.AudioScribe.route)
                },
                onNavigateToDocumentChat = {
                    navController.navigate(Screen.DocumentChat.route)
                },
                onNavigateToCodeCompanion = {
                    navController.navigate(Screen.CodeCompanion.route)
                },
                onNavigateToTemplates = {
                    navController.navigate(Screen.ConversationTemplates.route)
                },
                onNavigateToFlashcards = {
                    navController.navigate(Screen.Flashcards.route)
                },
                onNavigateToQuiz = {
                    navController.navigate(Screen.Quiz.route)
                },
                onNavigateToModels = {
                    navController.navigate(Screen.ModelLibrary.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.ConversationHistory.route)
                }
            )
        }
        
        // New Chat (from bottom nav) - Redirects to Chat screen
        composable(Screen.NewChat.route) {
            val viewModel: ChatViewModel = hiltViewModel()
            
            ChatScreen(
                viewModel = viewModel,
                onNavigateToModels = {
                    navController.navigate(Screen.ModelLibrary.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.ConversationHistory.route)
                },
                onNavigateBack = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }
        
        // History (from bottom nav)
        composable(Screen.History.route) {
            val viewModel: ConversationHistoryViewModel = hiltViewModel()
            
            ConversationHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onConversationSelected = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId))
                }
            )
        }
        
        // Library/Models (from bottom nav)
        composable(Screen.Library.route) {
            val viewModel: ModelLibraryViewModel = hiltViewModel()
            
            ModelLibraryScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }
        
        // AI Chat
        composable(
            route = "chat?conversationId={conversationId}",
            arguments = listOf(
                navArgument("conversationId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId")
            val viewModel: ChatViewModel = hiltViewModel()
            
            // Load conversation if ID provided
            conversationId?.let { viewModel.setCurrentConversation(it) }
            
            ChatScreen(
                viewModel = viewModel,
                onNavigateToModels = {
                    navController.navigate(Screen.ModelLibrary.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.ConversationHistory.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Prompt Lab
        composable(Screen.PromptLab.route) {
            val viewModel: PromptLabViewModel = hiltViewModel()
            
            PromptLabScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToModels = {
                    navController.navigate(Screen.ModelLibrary.route)
                }
            )
        }
        
        // Ask Image
        composable(Screen.AskImage.route) {
            val viewModel: AskImageViewModel = hiltViewModel()
            
            AskImageScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Audio Scribe
        composable(Screen.AudioScribe.route) {
            val viewModel: AudioScribeViewModel = hiltViewModel()
            
            AudioScribeScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Document Chat
        composable(Screen.DocumentChat.route) {
            val viewModel: DocumentChatViewModel = hiltViewModel()
            
            DocumentChatScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToModels = {
                    navController.navigate(Screen.ModelLibrary.route)
                }
            )
        }
        
        // Code Companion
        composable(Screen.CodeCompanion.route) {
            val viewModel: CodeCompanionViewModel = hiltViewModel()
            
            CodeCompanionScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToModels = {
                    navController.navigate(Screen.ModelLibrary.route)
                }
            )
        }
        
        // Conversation Templates
        composable(Screen.ConversationTemplates.route) {
            val viewModel: ConversationTemplatesViewModel = hiltViewModel()
            
            ConversationTemplatesScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSelectTemplate = { template ->
                    navController.navigate(Screen.Chat.createRoute(templatePrompt = template.prompt)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }
        
        // Flashcards
        composable(Screen.Flashcards.route) {
            val viewModel: FlashcardViewModel = hiltViewModel()
            
            FlashcardScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Quiz
        composable(Screen.Quiz.route) {
            val viewModel: QuizViewModel = hiltViewModel()
            
            QuizScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.ModelLibrary.route) {
            val viewModel: ModelLibraryViewModel = hiltViewModel()
            
            ModelLibraryScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.ConversationHistory.route) {
            val viewModel: ConversationHistoryViewModel = hiltViewModel()
            
            ConversationHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onConversationSelected = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }
    }
}
