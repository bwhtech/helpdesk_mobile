package io.github.kaulith.helpdeskanalytics.testing

import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportData
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportQuery
import io.github.kaulith.helpdeskanalytics.domain.repository.ReportDataRepository
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeReportDataRepository : ReportDataRepository {

    val queries = mutableListOf<ReportQuery>()

    override fun runReport(query: ReportQuery): Flow<Result<ReportData>> {
        queries += query
        return flowOf(Result.Success(ReportData.Detail(emptyList(), serverTotal = 0, truncated = false)))
    }

    override suspend fun distinctValues(frappeField: String): Result<List<String>> = Result.Success(emptyList())
}
