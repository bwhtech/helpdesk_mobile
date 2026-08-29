package io.github.kaulith.helpdeskanalytics.domain.repository

import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportData
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportQuery
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.flow.Flow

interface ReportDataRepository {
    fun runReport(query: ReportQuery): Flow<Result<ReportData>>

    /** Values a filter can pick from, read off the tickets that actually carry them. */
    suspend fun distinctValues(frappeField: String): Result<List<String>>
}
