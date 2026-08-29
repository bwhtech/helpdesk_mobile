package com.example.helpdeskanalytics.data.repository

import com.example.helpdeskanalytics.data.local.database.dao.ReportTemplateDao
import com.example.helpdeskanalytics.data.local.database.entities.ReportTemplateEntity
import com.example.helpdeskanalytics.domain.model.report.ChartType
import com.example.helpdeskanalytics.domain.model.report.DateRangePreset
import com.example.helpdeskanalytics.domain.model.report.ReportConfig
import com.example.helpdeskanalytics.domain.model.report.ReportMode
import com.example.helpdeskanalytics.domain.model.report.ReportTemplate
import com.example.helpdeskanalytics.domain.model.report.SortDirection
import com.example.helpdeskanalytics.domain.repository.ReportRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class ReportTemplateRepositoryImpl(
    private val dao: ReportTemplateDao
) : ReportRepository {

    private val gson = Gson()

    override fun observeTemplates(): Flow<List<ReportTemplate>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getTemplate(id: Long): ReportTemplate? =
        dao.getById(id)?.toDomain()

    override suspend fun saveTemplate(name: String, config: ReportConfig, id: Long?): Long {
        val entity = ReportTemplateEntity(
            id = id ?: 0,
            name = name,
            configJson = gson.toJson(config),
            updatedAt = Clock.System.now()
        )
        return if (id != null && id > 0) {
            dao.update(entity)
            id
        } else {
            dao.insert(entity)
        }
    }

    override suspend fun deleteTemplate(id: Long) = dao.delete(id)

    private fun ReportTemplateEntity.toDomain(): ReportTemplate {
        val config = runCatching { gson.fromJson(configJson, ReportConfig::class.java) }
            .getOrNull()
            ?.withDefaults()
        return ReportTemplate(id = id, name = name, config = config, updatedAt = updatedAt)
    }
}

/**
 * Gson builds through `Unsafe`, so Kotlin's constructor defaults never run and a
 * key absent from the stored JSON lands as null in a non-null field. Templates
 * saved before a field existed hit exactly that.
 */
@Suppress("SENSELESS_COMPARISON")
internal fun ReportConfig.withDefaults(): ReportConfig = copy(
    columns = if (columns == null) ReportConfig.DEFAULT_COLUMNS else columns,
    filters = if (filters == null) emptyList() else filters,
    sortDirection = if (sortDirection == null) SortDirection.DESC else sortDirection,
    dateRange = if (dateRange == null) DateRangePreset.LAST_30 else dateRange,
    mode = if (mode == null) ReportMode.DETAIL else mode,
    aggregates = if (aggregates == null) ReportConfig.DEFAULT_AGGREGATES else aggregates,
    chartType = if (chartType == null) ChartType.NONE else chartType
)
