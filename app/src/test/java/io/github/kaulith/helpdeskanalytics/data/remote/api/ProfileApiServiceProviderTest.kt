package io.github.kaulith.helpdeskanalytics.data.remote.api

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.github.kaulith.helpdeskanalytics.data.local.profiles.ProfileStore
import io.github.kaulith.helpdeskanalytics.testing.FakeApiServiceProvider
import io.github.kaulith.helpdeskanalytics.testing.FakeFrappeApiService
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ProfileApiServiceProviderTest {

    @Test
    fun `a fetch for a site no longer in use is refused`() {
        val profileStore = ProfileStore(ApplicationProvider.getApplicationContext<Application>())
        profileStore.activate("support.frappe.io")
        val provider = ProfileApiServiceProvider(FakeApiServiceProvider(FakeFrappeApiService()), profileStore)

        profileStore.activate("helpdesk-mb.fsn.frappe.cloud")

        assertThrows(IllegalStateException::class.java) { provider.getService() }
    }
}
