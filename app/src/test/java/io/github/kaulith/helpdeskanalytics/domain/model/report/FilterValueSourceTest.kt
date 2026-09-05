package io.github.kaulith.helpdeskanalytics.domain.model.report

import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.domain.model.Ticket
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterValueSourceTest {

    @Test
    fun `status offers the values the filter compares against`() {
        val source = ReportColumn.STATUS.valueSource() as FilterValueSource.Fixed

        assertEquals(Status.entries.map { it.value }, source.options.map { it.value })
    }

    @Test
    fun `overdue offers yes and no, matching how the engine reads a bool`() {
        val source = ReportColumn.OVERDUE.valueSource() as FilterValueSource.Fixed

        assertEquals(listOf("Yes", "No"), source.options.map { it.value })
    }

    @Test
    fun `agent picks from the agent list rather than a typed email`() {
        assertEquals(FilterValueSource.Agents, ReportColumn.AGENT.valueSource())
    }

    @Test
    fun `link-ish columns read their choices off the tickets`() {
        assertEquals(
            FilterValueSource.Distinct("customer"),
            ReportColumn.CUSTOMER.valueSource()
        )
        assertEquals(
            FilterValueSource.Distinct("agreement_status"),
            ReportColumn.SLA_STATUS.valueSource()
        )
    }

    @Test
    fun `subject and numbers stay typed`() {
        assertEquals(FilterValueSource.Typed, ReportColumn.SUBJECT.valueSource())
        assertEquals(FilterValueSource.Typed, ReportColumn.AGE_HOURS.valueSource())
        assertEquals(FilterValueSource.Typed, ReportColumn.CREATED.valueSource())
    }

    @Test
    fun `a contains condition drops a distinct column back to typing`() {
        assertTrue(ReportColumn.CUSTOMER.offersOptions(FilterOperator.EQUALS))
        assertFalse(ReportColumn.CUSTOMER.offersOptions(FilterOperator.CONTAINS))
    }

    @Test
    fun `a fixed column keeps its picker for every condition`() {
        assertTrue(ReportColumn.STATUS.offersOptions(FilterOperator.EQUALS))
        assertTrue(ReportColumn.STATUS.offersOptions(FilterOperator.CONTAINS))
        assertTrue(ReportColumn.AGENT.offersOptions(FilterOperator.CONTAINS))
    }

    /** The mapper puts the contact in `customerName`, which is not the `customer` link. */
    @Test
    fun `customer displays the same field it filters and groups on`() {
        val ticket = ticket(customerName = "ann@acme.test", customerId = "Acme Inc")

        assertEquals("customer", ReportColumn.CUSTOMER.frappeField)
        assertEquals("Acme Inc", ReportColumn.CUSTOMER.display(ticket))
    }

    private fun ticket(customerName: String?, customerId: String?) = Ticket(
        id = "1",
        subject = "s",
        status = Status.OPEN,
        priority = Priority.LOW,
        assignedTo = null,
        createdAt = Instant.fromEpochSeconds(0),
        modifiedAt = Instant.fromEpochSeconds(0),
        firstRespondedAt = null,
        resolvedAt = null,
        lastAgentResponseAt = null,
        customerName = customerName,
        customerId = customerId,
        assignees = emptyList(),
        responseBy = null,
        resolutionBy = null,
        firstResponseTimeMinutes = null,
        avgResponseTimeMinutes = null,
        resolutionTimeHours = null,
        ticketType = null,
        sla = null,
        agreementStatus = null,
        description = null
    )
}
