package io.github.kaulith.helpdeskanalytics.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import io.github.kaulith.helpdeskanalytics.ui.screens.analytics.AnalyticsScreen
import io.github.kaulith.helpdeskanalytics.ui.screens.dashboard.DashboardScreen
import io.github.kaulith.helpdeskanalytics.ui.screens.leaderboard.LeaderboardScreen
import io.github.kaulith.helpdeskanalytics.ui.screens.reports.ReportBuilderScreen
import io.github.kaulith.helpdeskanalytics.ui.screens.reports.ReportsListScreen
import io.github.kaulith.helpdeskanalytics.ui.screens.settings.SettingsScreen
import io.github.kaulith.helpdeskanalytics.ui.screens.tickets.TicketDetailScreen
import io.github.kaulith.helpdeskanalytics.ui.screens.tickets.TicketsScreen

@Composable
fun BottomNavGraph(navController: NavHostController, onLogout: () -> Unit = {}) {
    NavHost(
        navController = navController,
        startDestination = BottomNavScreen.Dashboard.route
    ) {
        composable(
            route = BottomNavScreen.Dashboard.route,
            deepLinks = listOf(navDeepLink { uriPattern = "helpdesk://dashboard" }),
        ) { DashboardScreen() }
        composable(
            route = BottomNavScreen.Tickets.route,
            deepLinks = listOf(navDeepLink { uriPattern = "helpdesk://tickets" }),
        ) {
            TicketsScreen(
                onTicketClick = { ticketId ->
                    navController.navigate("ticket_detail/$ticketId")
                }
            )
        }
        composable(
            route = BottomNavScreen.Analytics.route,
            deepLinks = listOf(navDeepLink { uriPattern = "helpdesk://analytics" }),
        ) { AnalyticsScreen() }
        composable(BottomNavScreen.Leaderboard.route) { LeaderboardScreen() }
        composable(BottomNavScreen.Settings.route) {
            SettingsScreen(
                onLogout = onLogout,
                onOpenReports = { navController.navigate("reports") }
            )
        }
        composable(
            route = "ticket_detail/{ticketId}",
            arguments = listOf(navArgument("ticketId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "helpdesk://ticket/{ticketId}" }),
        ) { backStackEntry ->
            val ticketId = backStackEntry.arguments?.getString("ticketId") ?: return@composable
            TicketDetailScreen(
                ticketId = ticketId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("reports") {
            ReportsListScreen(
                onBack = { navController.popBackStack() },
                onOpenTemplate = { templateId ->
                    navController.navigate("report_builder?templateId=${templateId ?: -1L}")
                }
            )
        }
        composable(
            route = "report_builder?templateId={templateId}",
            arguments = listOf(
                navArgument("templateId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("templateId") ?: -1L
            ReportBuilderScreen(
                templateId = id.takeIf { it >= 0L },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
