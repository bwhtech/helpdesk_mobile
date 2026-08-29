package io.github.kaulith.helpdeskanalytics.domain.model.report

/** Ticket rows a detail report pulls before it stops and reports a truncation. */
const val DETAIL_ROW_CAP = 2000

/**
 * Where each aggregate's numbers come from in the response. `AVG` is never asked
 * of the server: a `SUM` and a non-null `COUNT` divide into an exact mean, and
 * they stay exact when several `_assign` buckets fold into one agent.
 */
data class AggregateSource(val sumAlias: String?, val countAlias: String?)

sealed interface ReportQuery {
    data class Detail(
        val filters: String?,
        val orderBy: String,
        val limit: Int
    ) : ReportQuery

    data class Summary(
        val fields: String,
        val filters: String?,
        val groupBy: String?,
        val orderBy: String?,
        val groupField: String?,
        val sources: List<AggregateSource>,
        val fanOutAssignees: Boolean,
        val ignoredFilters: List<ReportColumn>
    ) : ReportQuery
}
