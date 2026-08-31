package io.github.kaulith.helpdeskanalytics.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.kaulith.helpdeskanalytics.data.local.preferences.PreferencesManager
import io.github.kaulith.helpdeskanalytics.data.update.AppUpdate
import io.github.kaulith.helpdeskanalytics.data.update.UpdateChecker
import io.github.kaulith.helpdeskanalytics.domain.repository.AgentRepository
import io.github.kaulith.helpdeskanalytics.ui.components.AgentChip
import io.github.kaulith.helpdeskanalytics.ui.components.OfflineIndicator
import io.github.kaulith.helpdeskanalytics.ui.components.UpdateBanner
import io.github.kaulith.helpdeskanalytics.ui.components.openWebLink
import io.github.kaulith.helpdeskanalytics.ui.navigation.BottomNavGraph
import io.github.kaulith.helpdeskanalytics.ui.navigation.BottomNavScreen
import io.github.kaulith.helpdeskanalytics.ui.theme.FrappeMotion
import io.github.kaulith.helpdeskanalytics.util.NetworkMonitor
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    pendingTicketId: String? = null,
    onPendingTicketHandled: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val cs = MaterialTheme.colorScheme

    val bottomNavRoutes = remember { BottomNavScreen.items.map { it.route }.toSet() }
    val showBottomBar = currentRoute in bottomNavRoutes

    val networkMonitor: NetworkMonitor = koinInject()
    val preferencesManager: PreferencesManager = koinInject()
    val agentRepository: AgentRepository = koinInject()
    val updateChecker: UpdateChecker = koinInject()

    val isOnline by networkMonitor.isOnline.collectAsState(initial = true)
    val lastSync by preferencesManager.lastSync.collectAsState(initial = 0L)

    val activeAgentFlow = remember { agentRepository.getActiveAgent() }
    val activeAgent by activeAgentFlow.collectAsState(initial = null)

    val dismissedUpdate by preferencesManager.dismissedUpdateVersion.collectAsState(initial = null)
    var latestUpdate by remember { mutableStateOf<AppUpdate?>(null) }
    LaunchedEffect(Unit) { latestUpdate = updateChecker.availableUpdate() }
    val pendingUpdate = latestUpdate?.takeIf { it.versionName != dismissedUpdate }

    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    val currentScreenLabel = remember(currentRoute) {
        BottomNavScreen.items.find { it.route == currentRoute }?.label ?: ""
    }

    // Adaptive nav placement: bottom bar under 600dp, side rail above.
    val widthDp = LocalConfiguration.current.screenWidthDp
    val useRail = widthDp >= 600

    fun navigateTo(route: String) {
        navController.navigate(route) {
            navController.graph.startDestinationRoute?.let { popUpTo(it) { saveState = true } }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(pendingTicketId) {
        val ticketId = pendingTicketId ?: return@LaunchedEffect
        navController.navigate("ticket_detail/$ticketId") { launchSingleTop = true }
        onPendingTicketHandled()
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = if (showBottomBar)
            Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier,
        containerColor = cs.surface,
        topBar = {
            if (showBottomBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = currentScreenLabel,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurface,
                        )
                    },
                    actions = {
                        AgentChip(agent = activeAgent)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = cs.surface,
                        titleContentColor = cs.onSurface,
                    ),
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar && !useRail,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = FrappeMotion.spatialOffset
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = FrappeMotion.spatialOffset
                )
            ) {
                Column {
                    HorizontalDivider(color = cs.outlineVariant)
                    NavigationBar(
                        containerColor = cs.surface,
                        tonalElevation = 0.dp
                    ) {
                        BottomNavScreen.items.forEach { screen ->
                            val selected = currentRoute == screen.route
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.label,
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                selected = selected,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = cs.onSecondaryContainer,
                                    selectedTextColor = cs.onSurface,
                                    indicatorColor = cs.secondaryContainer,
                                    unselectedIconColor = cs.onSurfaceVariant,
                                    unselectedTextColor = cs.onSurfaceVariant,
                                ),
                                onClick = { navigateTo(screen.route) }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (showBottomBar && useRail) {
                NavigationRail(
                    containerColor = cs.surface,
                ) {
                    BottomNavScreen.items.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationRailItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.label,
                                )
                            },
                            label = {
                                Text(
                                    text = screen.label,
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            selected = selected,
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = cs.onSecondaryContainer,
                                selectedTextColor = cs.onSurface,
                                indicatorColor = cs.secondaryContainer,
                                unselectedIconColor = cs.onSurfaceVariant,
                                unselectedTextColor = cs.onSurfaceVariant,
                            ),
                            onClick = { navigateTo(screen.route) },
                        )
                    }
                }
                HorizontalDivider(
                    color = cs.outlineVariant,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                OfflineIndicator(isOffline = !isOnline, lastSyncTimestamp = lastSync)
                UpdateBanner(
                    update = pendingUpdate,
                    onOpen = { uriHandler.openWebLink(it.releaseUrl) },
                    onDismiss = { update ->
                        scope.launch { preferencesManager.setDismissedUpdateVersion(update.versionName) }
                    },
                )
                BottomNavGraph(navController = navController, onLogout = onLogout)
            }
        }
    }
}
