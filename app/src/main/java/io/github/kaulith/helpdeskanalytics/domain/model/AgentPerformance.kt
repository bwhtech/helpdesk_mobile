package io.github.kaulith.helpdeskanalytics.domain.model

data class AgentPerformance(
    val agentEmail: String,
    val agentName: String,
    val rank: Int,
    val ticketsResolved: Int,
    val averageResponseTime: Float,
    val averageResolutionTime: Float,
    val isCurrentUser: Boolean = false
)
