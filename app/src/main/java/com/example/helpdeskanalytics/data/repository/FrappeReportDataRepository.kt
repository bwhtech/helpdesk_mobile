package com.example.helpdeskanalytics.data.repository

import com.example.helpdeskanalytics.data.mapper.toDomain
import com.example.helpdeskanalytics.data.remote.api.ApiServiceProvider
import com.example.helpdeskanalytics.data.remote.api.FrappeApiService
import com.example.helpdeskanalytics.data.remote.toNetworkError
import com.example.helpdeskanalytics.data.report.SummaryFolder
import com.example.helpdeskanalytics.domain.model.report.ReportData
import com.example.helpdeskanalytics.domain.model.report.ReportQuery
import com.example.helpdeskanalytics.domain.repository.ReportDataRepository
import com.example.helpdeskanalytics.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class FrappeReportDataRepository(
    private val apiServiceProvider: ApiServiceProvider
) : ReportDataRepository {

    private val distinctMutex = Mutex()
    private val distinctByField = mutableMapOf<String, List<String>>()

    override suspend fun distinctValues(frappeField: String): Result<List<String>> =
        withContext(Dispatchers.Default) {
            distinctMutex.withLock {
                distinctByField[frappeField]?.let { return@withLock Result.Success(it) }
                try {
                    val rows = apiServiceProvider.getService().getTicketSummary(
                        fields = """["$frappeField"]""",
                        groupBy = frappeField,
                        orderBy = "$frappeField asc"
                    ).message
                    val values = rows.mapNotNull { row ->
                        row.get(frappeField)
                            ?.takeIf { !it.isJsonNull }
                            ?.asString
                            ?.takeIf(String::isNotBlank)
                    }
                    distinctByField[frappeField] = values
                    Result.Success(values)
                } catch (e: Exception) {
                    Result.Error(e.toNetworkError())
                }
            }
        }

    override fun runReport(query: ReportQuery): Flow<Result<ReportData>> = flow {
        emit(Result.Loading)
        val service = apiServiceProvider.getService()
        val data = when (query) {
            is ReportQuery.Detail -> runDetail(service, query)
            is ReportQuery.Summary -> runSummary(service, query)
        }
        emit(Result.Success(data))
    }
        .catch { emit(Result.Error(it.toNetworkError())) }
        .flowOn(Dispatchers.Default)

    private suspend fun runDetail(
        service: FrappeApiService,
        query: ReportQuery.Detail
    ): ReportData.Detail = coroutineScope {
        val rows = async {
            service.getTickets(
                limit = query.limit,
                orderBy = query.orderBy,
                filters = query.filters
            ).data.map { it.toDomain() }
        }
        val total = async { service.getTicketCount(filters = query.filters ?: "[]").message }
        val tickets = rows.await()
        val serverTotal = total.await()
        ReportData.Detail(tickets, serverTotal, truncated = serverTotal > tickets.size)
    }

    private suspend fun runSummary(
        service: FrappeApiService,
        query: ReportQuery.Summary
    ): ReportData.Summary {
        val rows = service.getTicketSummary(
            fields = query.fields,
            filters = query.filters,
            groupBy = query.groupBy,
            orderBy = query.orderBy
        ).message
        return ReportData.Summary(SummaryFolder.fold(rows, query), query.ignoredFilters)
    }
}
