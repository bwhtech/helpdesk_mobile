package com.example.helpdeskanalytics.data.local.credentials

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.helpdeskanalytics.util.Constants

class CredentialsManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        Constants.ENCRYPTED_PREFERENCES_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCredentials(siteUrl: String, apiKey: String, apiSecret: String) {
        prefs.edit()
            .putString(KEY_SITE_URL, siteUrl.trimEnd('/'))
            .putString(KEY_API_KEY, apiKey)
            .putString(KEY_API_SECRET, apiSecret)
            .apply()
    }

    fun getSiteUrl(): String? = prefs.getString(KEY_SITE_URL, null)

    fun getApiKey(): String? = prefs.getString(KEY_API_KEY, null)

    fun getApiSecret(): String? = prefs.getString(KEY_API_SECRET, null)

    /** Admin (login) token: the key the user signed in with. */
    fun getAuthToken(): String? {
        val key = getApiKey() ?: return null
        val secret = getApiSecret() ?: return null
        return "token $key:$secret"
    }

    fun hasCredentials(): Boolean {
        return getSiteUrl() != null && getApiKey() != null && getApiSecret() != null
    }

    // --- Per-agent keys (auto-provisioned via the admin key) ---

    fun saveAgentKeys(email: String, apiKey: String, apiSecret: String) {
        prefs.edit()
            .putString(agentKeyPref(email), apiKey)
            .putString(agentSecretPref(email), apiSecret)
            .apply()
    }

    fun hasAgentKeys(email: String): Boolean =
        prefs.getString(agentKeyPref(email), null) != null &&
            prefs.getString(agentSecretPref(email), null) != null

    fun getAgentToken(email: String): String? {
        val key = prefs.getString(agentKeyPref(email), null) ?: return null
        val secret = prefs.getString(agentSecretPref(email), null) ?: return null
        return "token $key:$secret"
    }

    /** The agent the app is currently acting as; null means acting as the admin. */
    fun setActiveAgentEmail(email: String?) {
        prefs.edit().apply {
            if (email != null) putString(KEY_ACTIVE_AGENT, email) else remove(KEY_ACTIVE_AGENT)
        }.apply()
    }

    fun getActiveAgentEmail(): String? = prefs.getString(KEY_ACTIVE_AGENT, null)

    fun clearCredentials() {
        prefs.edit().clear().apply()
    }

    private fun agentKeyPref(email: String) = "agent_key_$email"
    private fun agentSecretPref(email: String) = "agent_secret_$email"

    companion object {
        private const val KEY_SITE_URL = "site_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_API_SECRET = "api_secret"
        private const val KEY_ACTIVE_AGENT = "active_agent_email"
    }
}
