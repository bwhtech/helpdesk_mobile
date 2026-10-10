package io.github.kaulith.helpdeskanalytics.data.remote.api

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.github.kaulith.helpdeskanalytics.data.remote.dto.RegisterDeviceRequest
import io.github.kaulith.helpdeskanalytics.testing.credentialsManager
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class NotificationApiClientTest {

    private val site = MockWebServer()

    @After
    fun tearDown() = site.close()

    @Test
    fun `a device registers with the helpdesk site it is signed in to`() = runBlocking {
        site.enqueue(MockResponse(body = "{}"))
        site.start()
        val credentials = credentialsManager(ApplicationProvider.getApplicationContext<Application>())
        credentials.saveCredentials(site.url("/").toString(), "key", "secret")
        val httpClient = OkHttpClient()
        val client = NotificationApiClient(credentials, OAuthClient(credentials, httpClient), httpClient)

        client.service.registerDevice(RegisterDeviceRequest("fcm-token", "rahul.agrawal@frappe.io"))

        val request = site.takeRequest()
        assertEquals("token key:secret", request.headers["Authorization"])
        assertNull(request.headers["X-Remote-Token"])
    }
}
