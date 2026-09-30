package io.github.kaulith.helpdeskanalytics.testing

import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportConfig
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportTemplate
import io.github.kaulith.helpdeskanalytics.domain.repository.ReportRepository
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeReportRepository : ReportRepository {

    val templates = MutableStateFlow<List<ReportTemplate>>(emptyList())

    override fun observeTemplates(): Flow<List<ReportTemplate>> = templates

    override suspend fun getTemplate(id: Long): ReportTemplate? = templates.value.find { it.id == id }

    override suspend fun saveTemplate(name: String, config: ReportConfig, id: Long?): Long {
        val rowId = id ?: (templates.value.maxOfOrNull { it.id } ?: 0) + 1
        val saved = ReportTemplate(rowId, name, config, Instant.fromEpochMilliseconds(0))
        templates.value = templates.value.filterNot { it.id == rowId } + saved
        return rowId
    }

    override suspend fun deleteTemplate(id: Long) {
        templates.value = templates.value.filterNot { it.id == id }
    }
}
