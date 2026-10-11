package io.github.kaulith.helpdeskanalytics.domain.model

/** A helpdesk site the app is signed in to. */
data class SiteProfile(
    val id: String,
    val siteUrl: String
)
