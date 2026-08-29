package com.example.helpdeskanalytics.domain.model

/** A file attached to a ticket comment or communication. */
data class Attachment(
    val name: String,
    /** Absolute URL, resolved against the site base URL. */
    val url: String,
    val fileName: String,
    val isImage: Boolean
)
