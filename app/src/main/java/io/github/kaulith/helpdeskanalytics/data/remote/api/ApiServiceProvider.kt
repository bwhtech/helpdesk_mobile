package io.github.kaulith.helpdeskanalytics.data.remote.api

import kotlinx.datetime.TimeZone

interface ApiServiceProvider {

    fun getService(): FrappeApiService

    fun invalidate()

    /** Frappe returns datetimes as wall clock time in the site's system timezone. */
    suspend fun siteTimeZone(): TimeZone

    /** Site URL with a trailing slash, for resolving relative file URLs. */
    fun siteBaseUrl(): String?
}
