package com.example.helpdeskanalytics.domain.model

import kotlinx.datetime.Instant

data class Comment(
    val name: String,
    val content: String,
    val commentedBy: String,
    val createdAt: Instant,
    val commentType: String,
    val attachments: List<Attachment> = emptyList()
)
