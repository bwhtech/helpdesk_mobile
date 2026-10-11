package io.github.kaulith.helpdeskanalytics.data.local.preferences

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.github.kaulith.helpdeskanalytics.data.local.profiles.ProfileStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PreferencesManagerTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val profileStore = ProfileStore(context)
    private val preferencesManager = PreferencesManager(context, profileStore)

    @After
    fun tearDown() = runBlocking { preferencesManager.clearAll() }

    @Test
    fun `the signed-in user of one site is not read as that of another`() = runBlocking {
        profileStore.activate("support.frappe.io")
        preferencesManager.setLoggedInUserEmail("rahul.agrawal@frappe.io")

        profileStore.activate("helpdesk-mb.fsn.frappe.cloud")

        assertNull(preferencesManager.loggedInUserEmail.first())
    }
}
