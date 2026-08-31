package io.github.kaulith.helpdeskanalytics.ui.agent

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.kaulith.helpdeskanalytics.ui.components.AgentSelectorSheet
import io.github.kaulith.helpdeskanalytics.ui.theme.FrappeRadius

/**
 * Agent picker and the confirmations that go with it, hosted once so the top bar
 * chip and Settings both open the same flow.
 */
@Composable
fun AgentSwitcher(viewModel: AgentSwitcherViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isVisible) {
        AgentSelectorSheet(
            agents = uiState.agents,
            activeAgent = uiState.activeAgent,
            onAgentSelected = { agent -> viewModel.setActiveAgent(agent) },
            onDismiss = viewModel::dismiss
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
}
