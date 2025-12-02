package com.localllm.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.localllm.app.ui.screen.ChatScreen
import com.localllm.app.ui.screen.ConversationHistoryScreen
import com.localllm.app.ui.screen.ModelLibraryScreen
import com.localllm.app.ui.screen.SettingsScreen
import com.localllm.app.ui.viewmodel.ChatViewModel
import com.localllm.app.ui.viewmodel.ConversationHistoryViewModel
import com.localllm.app.ui.viewmodel.ModelLibraryViewModel
import com.localllm.app.ui.viewmodel.SettingsViewModel

sealed class Screen(val route: String) {
    object Chat : Screen("chat?conversationId={conversationId}") {
        fun createRoute(conversationId: String? = null): String {
            return if (conversationId != null) {
                "chat?conversationId=$conversationId"
            } else {
                "chat"
            }
        }
    }
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
        startDestination = "chat",
        modifier = modifier
    ) {
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
                        popUpTo("chat") { inclusive = true }
                    }
                }
            )
        }
    }
}
