package io.github.kaulith.helpdeskanalytics.data.local.credentials

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.kaulith.helpdeskanalytics.data.local.profiles.ProfileStore
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class OldCredentialsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun credentials_left_by_1_1_are_deleted() {
        context.getSharedPreferences("helpdesk_secure_prefs", Context.MODE_PRIVATE).edit()
            .putString("api_key", "admin_key")
            .commit()

        CredentialsManager(context, CredentialsManager.keystoreAead(context), ProfileStore(context))

        assertFalse(File(context.dataDir, "shared_prefs/helpdesk_secure_prefs.xml").exists())
    }
}
