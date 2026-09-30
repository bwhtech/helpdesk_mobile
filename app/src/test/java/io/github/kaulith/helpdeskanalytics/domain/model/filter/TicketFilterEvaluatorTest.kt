package io.github.kaulith.helpdeskanalytics.domain.model.filter

import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.domain.model.Ticket
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TicketFilterEvaluatorTest {

    private val created = Instant.parse("2026-09-09T12:00:00Z")

    private val status = field("status")
    private val assign = field("_assign")
    private val customer = field("customer")
    private val subject = field("subject")
    private val creation = field("creation")
    private val resolutionHours = FilterableField<Ticket>(
        fieldname = "resolution_time",
        label = "Resolution Time",
        type = FilterFieldType.NUMBER,
        number = { it.resolutionTimeHours?.toDouble() },
    )

    private val openTicket = ticket(
        Status.OPEN, subject = "making pdf", assignees = listOf("ann@x.io", "bob@x.io")
    )
    private val closedTicket = ticket(
        Status.CLOSED, customerName = "ann@acme.test", resolutionTimeHours = 6f
    )

    @Test
    fun `select equals ignores case`() {
        assertTrue(condition(status, FilterOperator.EQUALS, "open").matches(openTicket))
        assertFalse(condition(status, FilterOperator.EQUALS, "open").matches(closedTicket))
        assertTrue(condition(status, FilterOperator.NOT_EQUALS, "Open").matches(closedTicket))
    }

    @Test
    fun `in and not in split a comma list and trim it`() {
        val openOrReplied = "Open, Replied,"
        assertTrue(condition(status, FilterOperator.IN, openOrReplied).matches(openTicket))
        assertFalse(condition(status, FilterOperator.IN, openOrReplied).matches(closedTicket))
        assertTrue(condition(status, FilterOperator.NOT_IN, openOrReplied).matches(closedTicket))
    }

    @Test
    fun `like matches part of the text ignoring case`() {
        assertTrue(condition(subject, FilterOperator.LIKE, "PDF").matches(openTicket))
        assertFalse(condition(subject, FilterOperator.NOT_LIKE, "pdf").matches(openTicket))
    }

    @Test
    fun `assigned to matches any of the assignees`() {
        assertTrue(condition(assign, FilterOperator.LIKE, "bob").matches(openTicket))
        assertFalse(condition(assign, FilterOperator.LIKE, "bob").matches(closedTicket))
        assertFalse(condition(assign, FilterOperator.NOT_LIKE, "ann").matches(openTicket))
    }

    @Test
    fun `is set and is not set follow a missing value`() {
        assertTrue(condition(customer, FilterOperator.IS, IS_NOT_SET).matches(openTicket))
        assertTrue(condition(customer, FilterOperator.IS, IS_SET).matches(closedTicket))
        assertTrue(condition(assign, FilterOperator.IS, IS_NOT_SET).matches(closedTicket))
        assertTrue(condition(resolutionHours, FilterOperator.IS, IS_NOT_SET).matches(openTicket))
        assertTrue(condition(creation, FilterOperator.IS, IS_SET).matches(openTicket))
    }

    @Test
    fun `number comparisons skip tickets without a value`() {
        assertTrue(condition(resolutionHours, FilterOperator.GT, "5").matches(closedTicket))
        assertFalse(condition(resolutionHours, FilterOperator.LT, "5").matches(closedTicket))
        assertTrue(condition(resolutionHours, FilterOperator.IN, "4, 6").matches(closedTicket))
        assertFalse(condition(resolutionHours, FilterOperator.GT, "5").matches(openTicket))
    }

    @Test
    fun `an unfinished value hides nothing`() {
        assertTrue(condition(resolutionHours, FilterOperator.GTE, "").matches(closedTicket))
        assertTrue(condition(creation, FilterOperator.LT, "").matches(openTicket))
        assertTrue(condition(creation, FilterOperator.BETWEEN, millis(created)).matches(openTicket))
        assertTrue(condition(creation, FilterOperator.TIMESPAN, "").matches(openTicket))
    }

    @Test
    fun `date operators compare calendar days`() {
        val sameDay = millis(created)
        val twoDaysLater = millis(created + 2.days)
        assertTrue(condition(creation, FilterOperator.EQUALS, sameDay).matches(openTicket))
        assertTrue(condition(creation, FilterOperator.LT, twoDaysLater).matches(openTicket))
        assertFalse(condition(creation, FilterOperator.GT, sameDay).matches(openTicket))
        assertTrue(condition(creation, FilterOperator.GTE, sameDay).matches(openTicket))
        assertTrue(
            condition(creation, FilterOperator.BETWEEN, "$sameDay,$twoDaysLater").matches(openTicket)
        )
    }

    @Test
    fun `today timespan picks tickets created now`() {
        val createdNow = openTicket.copy(createdAt = Clock.System.now())
        assertTrue(condition(creation, FilterOperator.TIMESPAN, "TODAY").matches(createdNow))
        assertFalse(condition(creation, FilterOperator.TIMESPAN, "YESTERDAY").matches(createdNow))
        assertTrue(condition(creation, FilterOperator.TIMESPAN, "LAST_7_DAYS").matches(createdNow))
    }

    @Test
    fun `every condition in a list must match`() {
        val conditions = listOf(
            condition(status, FilterOperator.EQUALS, "Open"),
            condition(assign, FilterOperator.LIKE, "ann"),
        )
        assertTrue(conditions.matches(openTicket))
        assertFalse(conditions.matches(closedTicket))
        assertTrue(emptyList<FilterCondition<Ticket>>().matches(closedTicket))
    }

    @Test
    fun `a fresh row starts on a usable value`() {
        assertEquals(IS_SET, defaultValueFor(customer, FilterOperator.IS))
        assertEquals(Status.OPEN.value, defaultValueFor(status, FilterOperator.EQUALS))
        assertEquals("", defaultValueFor(subject, FilterOperator.LIKE))
    }

    private fun field(fieldname: String) = TicketFilterFields.ALL.first { it.fieldname == fieldname }

    private fun condition(field: FilterableField<Ticket>, operator: FilterOperator, value: String) =
        FilterCondition(field, operator, value)

    private fun millis(instant: Instant) = instant.toEpochMilliseconds().toString()

    private fun ticket(
        status: Status,
        subject: String = "s",
        customerName: String? = null,
        assignees: List<String> = emptyList(),
        resolutionTimeHours: Float? = null
    ) = Ticket(
        id = "1",
        subject = subject,
        status = status,
        priority = Priority.LOW,
        assignedTo = null,
        createdAt = created,
        modifiedAt = created,
        firstRespondedAt = null,
        resolvedAt = null,
        lastAgentResponseAt = null,
        customerName = customerName,
        customerId = null,
        assignees = assignees,
        responseBy = null,
        resolutionBy = null,
        firstResponseTimeMinutes = null,
        avgResponseTimeMinutes = null,
        resolutionTimeHours = resolutionTimeHours,
        ticketType = null,
        sla = null,
        agreementStatus = null,
        description = null
    )
}
