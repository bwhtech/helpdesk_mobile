package com.example.helpdeskanalytics.data.report

import com.example.helpdeskanalytics.domain.model.report.ReportQuery
import com.example.helpdeskanalytics.domain.model.report.SummaryRow
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken

private val gson = Gson()
private val assigneeListType = object : TypeToken<List<String>>() {}.type

private const val UNASSIGNED_LABEL = "Unassigned"
private const val EMPTY_LABEL = "-"
private const val TOTAL_LABEL = "All tickets"

/** Collapses grouped rows from Frappe into one row per label. */
object SummaryFolder {

    /**
     * A ticket assigned to two agents sits in one `_assign` bucket that credits
     * both, matching how the leaderboard attributes work. Averages recombine from
     * a summed total over a non-null count, so folding buckets stays exact where
     * averaging the buckets' own averages would not.
     */
    fun fold(rows: List<JsonObject>, query: ReportQuery.Summary): List<SummaryRow> {
        val aliases = query.sources
            .flatMap { listOfNotNull(it.sumAlias, it.countAlias) }
            .distinct()
        val totals = LinkedHashMap<String, MutableMap<String, Double>>()

        rows.forEach { row ->
            labelsOf(row, query).forEach { label ->
                val bucket = totals.getOrPut(label) { aliases.associateWithTo(mutableMapOf()) { 0.0 } }
                aliases.forEach { alias -> bucket[alias] = bucket.getValue(alias) + row.number(alias) }
            }
        }

        return totals.map { (label, bucket) ->
            SummaryRow(label, query.sources.map { source ->
                val sum = source.sumAlias?.let { bucket.getValue(it) }
                val count = source.countAlias?.let { bucket.getValue(it) }
                when {
                    source.sumAlias == null -> count
                    source.countAlias == null -> sum
                    count == null || count == 0.0 -> null
                    else -> sum!! / count
                }
            })
        }
    }

    private fun labelsOf(row: JsonObject, query: ReportQuery.Summary): List<String> {
        val field = query.groupField ?: return listOf(TOTAL_LABEL)
        val raw = row.get(field)?.takeIf { !it.isJsonNull }?.asString
        if (!query.fanOutAssignees) return listOf(raw?.takeIf { it.isNotBlank() } ?: EMPTY_LABEL)
        val assignees = raw?.let {
            runCatching { gson.fromJson<List<String>>(it, assigneeListType) }.getOrNull()
        }
        return assignees?.takeIf { it.isNotEmpty() } ?: listOf(UNASSIGNED_LABEL)
    }

    private fun JsonObject.number(alias: String): Double =
        get(alias)?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0
}
