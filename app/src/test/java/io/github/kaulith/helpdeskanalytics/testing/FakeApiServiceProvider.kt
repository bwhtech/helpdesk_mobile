package io.github.kaulith.helpdeskanalytics.testing

import io.github.kaulith.helpdeskanalytics.data.remote.api.ApiServiceProvider
import io.github.kaulith.helpdeskanalytics.data.remote.api.FrappeApiService
import kotlinx.datetime.TimeZone

class FakeApiServiceProvider(private val service: FrappeApiService) : ApiServiceProvider {

    override fun getService() = service

    override fun invalidate() = Unit

    override suspend fun siteTimeZone() = TimeZone.of("Asia/Kolkata")

    override fun siteBaseUrl() = "https://support.frappe.io/"
}
