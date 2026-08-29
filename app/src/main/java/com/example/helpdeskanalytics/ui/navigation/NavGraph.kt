package com.example.helpdeskanalytics.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.helpdeskanalytics.domain.repository.AuthRepository
import com.example.helpdeskanalytics.ui.screens.MainScreen
import com.example.helpdeskanalytics.ui.screens.auth.LoginScreen
import org.koin.compose.koinInject

@Composable
fun AppNavGraph(
    pendingTicketId: String? = null,
    onPendingTicketHandled: () -> Unit = {}
) {
    val authRepository: AuthRepository = koinInject()
    val startDestination = remember {
        if (authRepository.isLoggedIn()) Screen.Main.route else Screen.Login.route
    }

    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Main.route) {
            MainScreen(
                pendingTicketId = pendingTicketId,
                onPendingTicketHandled = onPendingTicketHandled,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
