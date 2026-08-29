package io.github.kaulith.helpdeskanalytics.data.report

import io.github.kaulith.helpdeskanalytics.domain.model.report.AggregateSource
import io.github.kaulith.helpdeskanalytics.domain.model.report.ReportQuery
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SummaryFolderTest {

    private fun query(
        groupField: String?,
        sources: List<AggregateSource>,
        fanOutAssignees: Boolean = false
    ) = ReportQuery.Summary(
        fields = "",
        filters = null,
        groupBy = groupField,
        orderBy = null,
        groupField = groupField,
        sources = sources,
        fanOutAssignees = fanOutAssignees,
        ignoredFilters = emptyList()
    )

    private fun row(vararg pairs: Pair<String, Any>) = JsonObject().apply {
        pairs.forEach { (key, value) ->
            when (value) {
                is Number -> addProperty(key, value)
                else -> addProperty(key, value.toString())
            }
        }
    }

    @Test
    fun `every assignee on a ticket gets counted`() {
        val rows = listOf(
            row("_assign" to """["ann@x.io"]""", "row_count" to 3),
            row("_assign" to """["ann@x.io","bob@x.io"]""", "row_count" to 2)
        )

        val folded = SummaryFolder.fold(
            rows,
            query("_assign", listOf(AggregateSource(null, "row_count")), fanOutAssignees = true)
        ).associate { it.label to it.values.single() }

        assertEquals(5.0, folded.getValue("ann@x.io")!!, 0.0)
        assertEquals(2.0, folded.getValue("bob@x.io")!!, 0.0)
    }

    @Test
    fun `folding buckets keeps an average exact`() {
        // ann: 100s over 1 ticket, then 400s over 2 tickets => 500 / 3.
        val rows = listOf(
            row("_assign" to """["ann@x.io"]""", "sum_0" to 100, "count_0" to 1),
            row("_assign" to """["ann@x.io","bob@x.io"]""", "sum_0" to 400, "count_0" to 2)
        )

        val folded = SummaryFolder.fold(
            rows,
            query("_assign", listOf(AggregateSource("sum_0", "count_0")), fanOutAssignees = true)
        ).associate { it.label to it.values.single() }

        assertEquals(500.0 / 3.0, folded.getValue("ann@x.io")!!, 1e-9)
        assertEquals(200.0, folded.getValue("bob@x.io")!!, 1e-9)
    }

    @Test
    fun `an average over no non-null values is absent rather than zero`() {
        val rows = listOf(row("status" to "Open", "sum_0" to 0, "count_0" to 0))

        val folded = SummaryFolder.fold(
            rows,
            query("status", listOf(AggregateSource("sum_0", "count_0")))
        )

        assertNull(folded.single().values.single())
    }

    @Test
    fun `a ticket with no assignee lands under Unassigned`() {
        val rows = listOf(row("_assign" to "[]", "row_count" to 4))

        val folded = SummaryFolder.fold(
            rows,
            query("_assign", listOf(AggregateSource(null, "row_count")), fanOutAssignees = true)
        )

        assertEquals("Unassigned", folded.single().label)
        assertEquals(4.0, folded.single().values.single()!!, 0.0)
    }

    @Test
    fun `an ungrouped summary folds to a single total row`() {
        val rows = listOf(row("row_count" to 57000))

        val folded = SummaryFolder.fold(rows, query(null, listOf(AggregateSource(null, "row_count"))))

        assertEquals("All tickets", folded.single().label)
        assertEquals(57000.0, folded.single().values.single()!!, 0.0)
    }

    @Test
    fun `a plain group keeps each label once`() {
        val rows = listOf(
            row("status" to "Open", "row_count" to 10),
            row("status" to "Closed", "row_count" to 20)
        )

        val folded = SummaryFolder.fold(rows, query("status", listOf(AggregateSource(null, "row_count"))))

        assertEquals(listOf("Open", "Closed"), folded.map { it.label })
        assertEquals(listOf(10.0, 20.0), folded.map { it.values.single() })
    }
}
