package io.github.kaulith.helpdeskanalytics.domain.repository

import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportConfig
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportTemplate
import kotlinx.coroutines.flow.Flow

interface ReportRepository {

    fun observeTemplates(): Flow<List<ReportTemplate>>

    suspend fun getTemplate(id: Long): ReportTemplate?

    /** Inserts a new template, or updates the existing one when [id] is given. Returns the row id. */
    suspend fun saveTemplate(name: String, config: ReportConfig, id: Long?): Long

    suspend fun deleteTemplate(id: Long)
}
