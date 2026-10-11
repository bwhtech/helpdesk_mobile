package io.github.kaulith.helpdeskanalytics.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.kaulith.helpdeskanalytics.domain.repository.AuthRepository
import io.github.kaulith.helpdeskanalytics.ui.screens.MainScreen
import io.github.kaulith.helpdeskanalytics.ui.screens.auth.LoginScreen
import io.github.kaulith.helpdeskanalytics.ui.screens.auth.OAuthRedirectHolder
import org.koin.compose.koinInject

@Composable
fun AppNavGraph(
    pendingTicket: PendingTicket? = null,
    onPendingTicketHandled: () -> Unit = {}
) {
    val authRepository: AuthRepository = koinInject()
    val oAuthRedirectHolder: OAuthRedirectHolder = koinInject()
    val startDestination = remember {
        // A redirect that restarted the app is a sign-in to another site left to finish.
        val isSigningIn = oAuthRedirectHolder.redirect.value != null
        if (authRepository.isLoggedIn() && !isSigningIn) Screen.Main.route else Screen.Login.route
    }

    val navController = rememberNavController()
    // Everything on screen belongs to the site that was in use, so none of it is kept.
    val showMain = {
        navController.navigate(Screen.Main.route) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            // Opened over a site in use, it is adding another and can be backed out of.
            val isAddingSite = navController.previousBackStackEntry != null
            LoginScreen(
                onLoginSuccess = showMain,
                onCancel = if (isAddingSite) ({ navController.popBackStack(); Unit }) else null
            )
        }
        composable(Screen.Main.route) {
            MainScreen(
                pendingTicket = pendingTicket,
                onPendingTicketHandled = onPendingTicketHandled,
                onAddSite = { navController.navigate(Screen.Login.route) },
                onSiteChanged = showMain,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
