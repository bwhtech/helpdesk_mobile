package io.github.kaulith.helpdeskanalytics.domain.model

import kotlin.time.Instant

data class Comment(
    val name: String,
    val content: String,
    val commentedBy: String,
    val createdAt: Instant,
    val commentType: String,
    val attachments: List<Attachment> = emptyList()
)
