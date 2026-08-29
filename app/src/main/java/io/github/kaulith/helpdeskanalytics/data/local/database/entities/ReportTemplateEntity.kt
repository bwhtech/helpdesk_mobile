package io.github.kaulith.helpdeskanalytics.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/** A saved report definition. [configJson] is a Gson-serialized ReportConfig. */
@Entity(tableName = "report_templates")
data class ReportTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val configJson: String,
    val updatedAt: Instant
)
