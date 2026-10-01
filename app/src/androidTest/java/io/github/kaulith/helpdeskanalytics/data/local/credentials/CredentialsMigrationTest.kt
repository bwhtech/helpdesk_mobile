package io.github.kaulith.helpdeskanalytics.data.local.credentials

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ApplicationProvider
import io.github.kaulith.helpdeskanalytics.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class CredentialsMigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun credentials_stored_by_1_1_move_to_the_new_store() {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            Constants.ENCRYPTED_PREFERENCES_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).edit()
            .putString("site_url", "https://support.x.io")
            .putString("api_key", "admin_key")
            .putString("api_secret", "admin_secret")
            .putString("agent_key_li@x.io", "li_key")
            .putString("agent_secret_li@x.io", "li_secret")
            .putBoolean("agent_login_session_ann@x.io", true)
            .commit()

        val credentials = CredentialsManager(context, CredentialsManager.keystoreAead(context))

        assertEquals("https://support.x.io", credentials.getSiteUrl())
        assertEquals("token admin_key:admin_secret", credentials.getAuthToken())
        assertEquals("token li_key:li_secret", credentials.getAgentToken("li@x.io"))
        assertEquals("token admin_key:admin_secret", credentials.getAgentToken("ann@x.io"))
        assertFalse(File(context.dataDir, "shared_prefs/${Constants.ENCRYPTED_PREFERENCES_NAME}.xml").exists())
    }
}
