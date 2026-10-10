package io.github.kaulith.helpdeskanalytics.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.github.kaulith.helpdeskanalytics.data.local.preferences.PreferencesManager
import io.github.kaulith.helpdeskanalytics.data.remote.api.DeviceRegistration
import io.github.kaulith.helpdeskanalytics.data.remote.api.OAuthClient
import io.github.kaulith.helpdeskanalytics.testing.FakeApiServiceProvider
import io.github.kaulith.helpdeskanalytics.testing.FakeFrappeApiService
import io.github.kaulith.helpdeskanalytics.testing.credentialsManager
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class FrappeAuthRepositoryTest {

    @Test
    fun `signing out unregisters the device while the session can still authenticate`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val credentials = credentialsManager(context)
        credentials.saveCredentials("https://support.frappe.io", "key", "secret")
        var signedInWhenUnregistered = false
        val repository = FrappeAuthRepository(
            credentials,
            FakeApiServiceProvider(FakeFrappeApiService()),
            PreferencesManager(context),
            OAuthClient(credentials, OkHttpClient()),
            object : DeviceRegistration {
                override suspend fun unregisterCurrentDevice() {
                    signedInWhenUnregistered = credentials.hasCredentials()
                }
            }
        )

        repository.logout()

        assertTrue(signedInWhenUnregistered)
    }
}
