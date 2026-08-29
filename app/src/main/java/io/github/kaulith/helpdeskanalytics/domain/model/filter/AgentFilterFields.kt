package io.github.kaulith.helpdeskanalytics.domain.model.filter

import io.github.kaulith.helpdeskanalytics.domain.model.AgentPerformance
import io.github.kaulith.helpdeskanalytics.domain.model.Team

/**
 * Filterable fields for the ranking tab. [teams] drives the Team options and the
 * membership lookup, so "Team equals X" means the agent belongs to team X.
 */
fun agentFilterFields(teams: List<Team>): List<FilterableField<AgentPerformance>> = listOf(
    FilterableField(
        fieldname = "team",
        label = "Team",
        type = FilterFieldType.SELECT,
        options = teams.map { it.name },
        list = { agent -> teams.filter { agent.agentEmail in it.members }.map { it.name } },
    ),
    FilterableField(
        fieldname = "agent_name",
        label = "Agent",
        type = FilterFieldType.TEXT,
        text = { it.agentName },
    ),
    FilterableField(
        fieldname = "tickets_resolved",
        label = "Tickets Resolved",
        type = FilterFieldType.NUMBER,
        number = { it.ticketsResolved.toDouble() },
    ),
)
