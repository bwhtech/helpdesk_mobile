package io.github.kaulith.helpdeskanalytics.data.remote.api

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.github.kaulith.helpdeskanalytics.testing.credentialsManager
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class OAuthClientTest {

    private val site = MockWebServer()

    @After
    fun tearDown() = site.close()

    @Test
    fun `a session refreshed while another site comes into use stays with its own site`() {
        val credentials = credentialsManager(ApplicationProvider.getApplicationContext<Application>())
        site.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                credentials.saveCredentials("https://helpdesk-mb.fsn.frappe.cloud", "key", "secret")
                return MockResponse(body = """{"access_token":"refreshed","refresh_token":"refresh"}""")
            }
        }
        site.start()
        credentials.saveOAuthLogin(site.url("/").toString(), "client", "expired", "refresh")

        OAuthClient(credentials, OkHttpClient()).refreshSession()

        assertEquals("token key:secret", credentials.getAuthToken())
    }
}
