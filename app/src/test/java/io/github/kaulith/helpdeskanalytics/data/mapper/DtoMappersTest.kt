package io.github.kaulith.helpdeskanalytics.data.mapper

import io.github.kaulith.helpdeskanalytics.data.remote.dto.CommentDto
import io.github.kaulith.helpdeskanalytics.data.remote.dto.TicketDto
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class DtoMappersTest {

    private val kolkata = TimeZone.of("Asia/Kolkata")

    @Test
    fun `ticket datetimes are read in the site timezone`() {
        val ticket = ticketDto(
            creation = "2026-09-06 11:02:40.518734",
            modified = "2026-09-13 22:16:57.203114",
            responseBy = "2026-09-07 11:02:40"
        ).toDomain(kolkata)

        assertEquals(Instant.parse("2026-09-06T05:32:40.518734Z"), ticket.createdAt)
        assertEquals(Instant.parse("2026-09-13T16:46:57.203114Z"), ticket.modifiedAt)
        assertEquals(Instant.parse("2026-09-07T05:32:40Z"), ticket.responseBy)
    }

    @Test
    fun `comment creation is read in the site timezone`() {
        val comment = CommentDto(
            name = "c1f3e8a2b7",
            content = "<p>Checked the print format on the site</p>",
            commentedBy = "rahul.agrawal@frappe.io",
            creation = "2026-09-13 22:16:57"
        ).toDomain(baseUrl = "https://support.frappe.io/", siteTimeZone = kolkata)

        assertEquals(Instant.parse("2026-09-13T16:46:57Z"), comment.createdAt)
    }

    private fun ticketDto(creation: String, modified: String, responseBy: String?) = TicketDto(
        name = "77595",
        subject = "making pdf",
        status = "Open",
        priority = "Low",
        agent = null,
        creation = creation,
        modified = modified,
        firstRespondedOn = null,
        resolutionDate = null,
        lastAgentResponse = null,
        raisedBy = "firstmoondubai@hotmail.com",
        contact = null,
        customer = null,
        assign = """["rahul.agrawal@frappe.io"]""",
        responseBy = responseBy,
        resolutionBy = null,
        firstResponseTime = null,
        avgResponseTime = null,
        resolutionTime = null,
        ticketType = "UI/UX",
        sla = null,
        agreementStatus = null,
        description = null
    )
}
