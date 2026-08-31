package io.github.kaulith.helpdeskanalytics.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.kaulith.helpdeskanalytics.BuildConfig
import io.github.kaulith.helpdeskanalytics.ui.components.AgentSelectorSheet
import io.github.kaulith.helpdeskanalytics.ui.components.InitialsAvatar
import io.github.kaulith.helpdeskanalytics.ui.components.OnResume
import io.github.kaulith.helpdeskanalytics.ui.theme.AppColorScheme
import io.github.kaulith.helpdeskanalytics.ui.theme.FrappeRadius
import io.github.kaulith.helpdeskanalytics.ui.theme.Spacing

import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    onLogout: () -> Unit = {},
    onOpenReports: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    var batteryUnrestricted by remember { mutableStateOf(isBatteryUnrestricted(context)) }
    OnResume { batteryUnrestricted = isBatteryUnrestricted(context) }

    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissLogoutDialog,
            title = { Text("Disconnect") },
            text = { Text("Sign out and clear all cached data?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.logout(onLogout) },
                    colors = ButtonDefaults.textButtonColors(contentColor = cs.error)
                ) { Text("Disconnect") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissLogoutDialog) { Text("Cancel") }
            },
            shape = FrappeRadius.xl2
        )
    }

    if (uiState.showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissClearCacheDialog,
            title = { Text("Clear cache") },
            text = { Text("Remove all locally cached data. Fresh data will be fetched on next sync.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearCache() }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissClearCacheDialog) { Text("Cancel") }
            },
            shape = FrappeRadius.xl2
        )
    }

    if (uiState.showAgentSelector) {
        AgentSelectorSheet(
            agents = uiState.agents,
            activeAgent = uiState.activeAgent,
            onAgentSelected = { agent -> viewModel.setActiveAgent(agent) },
            onDismiss = viewModel::dismissAgentSelector
        )
    }

    uiState.writeKeyPromptAgent?.let { agent ->
        AlertDialog(
            onDismissRequest = viewModel::dismissWriteKeyPrompt,
            title = { Text("Write as ${agent.name}?") },
            text = {
                Text(
                    "Frappe issues ${agent.name} a new API secret so replies and edits are " +
                        "recorded as them. Their old secret stops working, and anything else " +
                        "using it breaks until it is updated.\n\n" +
                        "Read only keeps their key untouched; you can still browse their " +
                        "tickets and receive their notifications."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmWriteKey) { Text("Issue key") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::skipWriteKey) { Text("Read only") }
            },
            shape = FrappeRadius.xl2
        )
    }

    uiState.agentSwitchError?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::dismissAgentSwitchError,
            title = { Text("Couldn't switch agent") },
            text = {
                Text(
                    "$error\n\nThe admin key needs the System Manager role to provision agent keys."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissAgentSwitchError) { Text("OK") }
            },
            shape = FrappeRadius.xl2
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.surface),
        contentPadding = PaddingValues(Spacing.base),
        verticalArrangement = Arrangement.spacedBy(Spacing.base)
    ) {
        item {
            SettingsSection(title = "Account") {
                SettingsRow(
                    icon = Icons.Outlined.Language,
                    label = "Server",
                    value = uiState.serverUrl.ifEmpty { "Not connected" }
                )
                SettingsRow(
                    icon = Icons.Outlined.Person,
                    label = "User",
                    value = uiState.userName.ifEmpty { "Loading..." }
                )
                SettingsRow(
                    icon = Icons.Outlined.Shield,
                    label = "Role",
                    value = uiState.userRole.ifEmpty { "N/A" }
                )
            }
        }

        item {
            SettingsSection(title = "Active agent") {
                ActiveAgentBlock(
                    name = uiState.activeAgent?.name,
                    email = uiState.activeAgent?.email,
                    onSwitch = viewModel::showAgentSelector
                )
            }
        }

        item {
            SettingsSection(title = "Notifications") {
                NavRow(
                    icon = Icons.Outlined.BatteryAlert,
                    label = "Background delivery",
                    description = if (batteryUnrestricted) {
                        "Allowed to run in the background"
                    } else {
                        "Battery saving can hold back pushes while the app is closed"
                    },
                    onClick = { context.startActivity(batteryOptimizationIntent()) }
                )
                NavRow(
                    icon = Icons.Outlined.Notifications,
                    label = "Alert settings",
                    description = "Sounds and channels for ticket alerts",
                    onClick = { context.startActivity(appNotificationSettingsIntent(context.packageName)) }
                )
            }
        }

        item {
            SettingsSection(title = "Appearance") {
                AppearanceBlock(
                    themeMode = uiState.themeMode,
                    onThemeModeChange = viewModel::setThemeMode,
                    colorScheme = uiState.colorScheme,
                    onColorSchemeChange = viewModel::setColorScheme
                )
            }
        }

        item {
            SettingsSection(title = "Tools") {
                NavRow(
                    icon = Icons.Outlined.Assessment,
                    label = "Reports",
                    description = "Build, export and save ticket reports",
                    onClick = onOpenReports
                )
            }
        }

        item {
            SettingsSection(title = "Data") {
                NavRow(
                    icon = Icons.Outlined.Delete,
                    label = "Clear cache",
                    description = "Remove all locally cached data",
                    onClick = viewModel::showClearCacheDialog
                )
            }
        }

        item {
            SettingsSection(title = "About") {
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    label = "Version",
                    value = BuildConfig.VERSION_NAME
                )
                SettingsRow(
                    icon = Icons.Outlined.Build,
                    label = "Build",
                    value = BuildConfig.VERSION_CODE.toString()
                )
            }
        }

        item {
            OutlinedButton(
                onClick = viewModel::showLogoutDialog,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = FrappeRadius.full,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.error)
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(Spacing.sm))
                Text("Disconnect", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(Spacing.md))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = Spacing.sm)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = FrappeRadius.lg,
            colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(Spacing.base)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, label: String, value: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(label, style = MaterialTheme.typography.bodyLarge, color = cs.onSurface)
        }
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NavRow(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = cs.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = cs.onSurface)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = cs.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ActiveAgentBlock(
    name: String?,
    email: String?,
    onSwitch: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        InitialsAvatar(name = name ?: "All", size = 48.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name ?: "All agents",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = email ?: "Viewing data for all agents",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    Spacer(Modifier.height(Spacing.sm))
    OutlinedButton(
        onClick = onSwitch,
        modifier = Modifier.fillMaxWidth(),
        shape = FrappeRadius.full
    ) {
        Icon(Icons.Outlined.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(Spacing.sm))
        Text(if (name != null) "Switch agent" else "Select agent")
    }
}

@Composable
private fun AppearanceBlock(
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    colorScheme: String,
    onColorSchemeChange: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Text(
        "Theme",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = cs.onSurfaceVariant
    )
    Spacer(Modifier.height(Spacing.xs))
    listOf("system" to "System default", "light" to "Light", "dark" to "Dark").forEach { (k, label) ->
        val selected = themeMode == k
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(FrappeRadius.md)
                .selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = { onThemeModeChange(k) }
                )
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = cs.onSurface)
            RadioButton(
                selected = selected,
                onClick = null
            )
        }
    }

    Spacer(Modifier.height(Spacing.md))
    Text(
        "Accent",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = cs.onSurfaceVariant
    )
    Spacer(Modifier.height(Spacing.sm))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        AppColorScheme.entries.forEach { scheme ->
            val isSelected = colorScheme == scheme.key
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onColorSchemeChange(scheme.key) }
                    )
                    .semantics { contentDescription = scheme.label },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(scheme.seed)
                        .then(
                            if (isSelected) Modifier.border(
                                width = 3.dp,
                                color = cs.onSurface,
                                shape = CircleShape
                            ) else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(Spacing.xs))
    Text(
        text = AppColorScheme.fromKey(colorScheme).label,
        style = MaterialTheme.typography.bodySmall,
        color = cs.onSurfaceVariant
    )
}


private fun isBatteryUnrestricted(context: Context): Boolean {
    val power = context.getSystemService(PowerManager::class.java) ?: return true
    return power.isIgnoringBatteryOptimizations(context.packageName)
}

private fun batteryOptimizationIntent(): Intent =
    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

private fun appNotificationSettingsIntent(packageName: String): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
