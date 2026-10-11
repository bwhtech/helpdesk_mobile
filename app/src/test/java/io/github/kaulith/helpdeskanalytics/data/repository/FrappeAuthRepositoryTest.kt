package io.github.kaulith.helpdeskanalytics.data.repository

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import io.github.kaulith.helpdeskanalytics.data.local.preferences.PreferencesManager
import io.github.kaulith.helpdeskanalytics.data.local.profiles.ProfileStore
import io.github.kaulith.helpdeskanalytics.data.remote.api.DeviceRegistration
import io.github.kaulith.helpdeskanalytics.data.remote.api.OAuthClient
import io.github.kaulith.helpdeskanalytics.data.remote.dto.UserDto
import io.github.kaulith.helpdeskanalytics.testing.FakeApiServiceProvider
import io.github.kaulith.helpdeskanalytics.testing.FakeFrappeApiService
import io.github.kaulith.helpdeskanalytics.testing.credentialsManager
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class FrappeAuthRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val credentials = credentialsManager(context)
    private val service = FakeFrappeApiService()
    private val site = MockWebServer()

    private fun repository(deviceRegistration: DeviceRegistration) = FrappeAuthRepository(
        credentials,
        ProfileStore(context),
        FakeApiServiceProvider(service),
        PreferencesManager(context, ProfileStore(context)),
        OAuthClient(credentials, OkHttpClient()),
        deviceRegistration
    )

    private val noDeviceRegistration = object : DeviceRegistration {
        override suspend fun unregisterCurrentDevice() = Unit

        override fun registerNow() = Unit
    }

    @After
    fun tearDown() = site.close()

    @Test
    fun `signing out unregisters the device while the session can still authenticate`() = runBlocking {
        credentials.saveCredentials("https://support.frappe.io", "key", "secret")
        var signedInWhenUnregistered = false
        val repository = repository(object : DeviceRegistration {
            override suspend fun unregisterCurrentDevice() {
                signedInWhenUnregistered = credentials.hasCredentials()
            }

            override fun registerNow() = Unit
        })

        repository.logout()

        assertTrue(signedInWhenUnregistered)
    }

    @Test
    fun `signing out keeps the profile of the site for signing back in`() = runBlocking {
        credentials.saveCredentials("https://support.frappe.io", "key", "secret")

        repository(noDeviceRegistration).logout()

        assertFalse(credentials.hasCredentials())
        assertTrue(credentials.hasProfile("https://support.frappe.io"))
    }

    @Test
    fun `signing out of one site puts another signed-in site in use`() = runBlocking {
        credentials.saveCredentials("https://support.frappe.io", "key", "secret")
        credentials.saveCredentials("https://helpdesk-mb.fsn.frappe.cloud", "key", "secret")

        repository(noDeviceRegistration).logout()

        assertEquals("https://support.frappe.io", credentials.getSiteUrl())
    }

    @Test
    fun `switching site puts the picked site in use`() = runBlocking {
        credentials.saveCredentials("https://support.frappe.io", "key", "secret")
        credentials.saveCredentials("https://helpdesk-mb.fsn.frappe.cloud", "key", "secret")
        val repository = repository(noDeviceRegistration)

        repository.switchSite(repository.otherSites().single().id)

        assertEquals("https://support.frappe.io", credentials.getSiteUrl())
    }

    @Test
    fun `a sign-in another site turns down leaves the signed-in site in use`() = runBlocking {
        credentials.saveCredentials("https://support.frappe.io", "key", "secret")

        repository(noDeviceRegistration)
            .validateCredentials("https://helpdesk-mb.fsn.frappe.cloud", "wrong_key", "wrong_secret")

        assertEquals("https://support.frappe.io", credentials.getSiteUrl())
        assertEquals("token key:secret", credentials.getAuthToken())
    }

    @Test
    fun `signing in with API keys replaces the OAuth session of the site`() = runBlocking {
        credentials.saveOAuthLogin("https://support.frappe.io", "client", "access", "refresh")
        service.user = UserDto("ann@x.io", "Ann", "ann@x.io", emptyList())

        repository(noDeviceRegistration).validateCredentials("https://support.frappe.io", "key", "secret")

        assertEquals("token key:secret", credentials.getAuthToken())
    }

    @Test
    fun `signing in over OAuth keeps the session in the profile of that site`() = runBlocking {
        site.enqueue(MockResponse(body = """{"access_token":"access","refresh_token":"refresh"}"""))
        site.start()
        service.user = UserDto("ann@x.io", "Ann", "ann@x.io", emptyList())
        val siteUrl = site.url("/").toString().trimEnd('/')
        val repository = repository(noDeviceRegistration)

        val authorizeUrl = repository.beginOAuthLogin(siteUrl, "client")
        repository.completeOAuthLogin("code", Uri.parse(authorizeUrl).getQueryParameter("state")!!)

        assertEquals(siteUrl, credentials.getSiteUrl())
        assertEquals("Bearer access", credentials.getAuthToken())
    }
}
