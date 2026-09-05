package io.github.kaulith.helpdeskanalytics.domain.model

import kotlin.time.Instant

/** One email in a ticket's customer-facing conversation. */
data class Communication(
    val name: String,
    val content: String,
    val sender: String,
    /** True when an agent sent it outward; false when received from the customer. */
    val sentByAgent: Boolean,
    val createdAt: Instant,
    val subject: String?,
    val attachments: List<Attachment> = emptyList()
)
