package com.localllm.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.localllm.app.ui.navigation.LocalLLMBottomNavBar
import com.localllm.app.ui.navigation.LocalLLMNavHost
import com.localllm.app.ui.theme.LocalLLMTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            LocalLLMTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    
                    // List of routes that should show the bottom navigation bar
                    val bottomNavRoutes = listOf(
                        "home",
                        "new_chat",
                        "history",
                        "library"
                    )
                    
                    // Show bottom bar only on main navigation screens
                    val showBottomBar = currentRoute in bottomNavRoutes
                    
                    Scaffold(
                        bottomBar = {
                            if (showBottomBar) {
                                LocalLLMBottomNavBar(navController = navController)
                            }
                        }
                    ) { innerPadding ->
                        LocalLLMNavHost(
                            navController = navController,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
