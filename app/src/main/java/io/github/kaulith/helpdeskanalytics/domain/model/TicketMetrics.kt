package io.github.kaulith.helpdeskanalytics.domain.model

data class TicketMetrics(
    val today: PeriodMetrics,
    val thisWeek: PeriodMetrics,
    val thisMonth: PeriodMetrics,
    val averageResponseTime: Float,
    val averageResolutionTime: Float,
    val openTicketsCount: Int,
    val urgentOpenCount: Int,
    val overdueCount: Int,
    val dueToday: Int,
    val responseTimePercentiles: ResponseTimePercentiles
)

data class PeriodMetrics(
    val ticketsCreated: Int,
    val ticketsResolved: Int,
    val trendPercentage: Float,
    val openTickets: Int
)

data class ResponseTimePercentiles(
    val p50: Float,
    val p90: Float,
    val p95: Float
)
