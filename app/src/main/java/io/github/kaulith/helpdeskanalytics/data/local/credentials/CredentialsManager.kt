package io.github.kaulith.helpdeskanalytics.data.local.credentials

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.kaulith.helpdeskanalytics.util.Constants
import kotlin.time.Duration.Companion.seconds

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

    fun saveSiteUrl(siteUrl: String) {
        prefs.edit().putString(KEY_SITE_URL, siteUrl.trimEnd('/')).apply()
    }

    fun getApiKey(): String? = prefs.getString(KEY_API_KEY, null)

    fun getApiSecret(): String? = prefs.getString(KEY_API_SECRET, null)

    /** The token of the signed-in session, whichever way the user signed in. */
    fun getAuthToken(): String? {
        getAccessToken()?.let { return "Bearer $it" }
        val key = getApiKey() ?: return null
        val secret = getApiSecret() ?: return null
        return "token $key:$secret"
    }

    fun hasCredentials(): Boolean {
        if (getSiteUrl() == null) return false
        return getAccessToken() != null || (getApiKey() != null && getApiSecret() != null)
    }

    // --- OAuth session ---

    fun saveOAuthSession(
        accessToken: String,
        refreshToken: String?,
        expiresInSeconds: Long?
    ) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .apply {
                if (refreshToken != null) putString(KEY_REFRESH_TOKEN, refreshToken)
                if (expiresInSeconds != null) {
                    putLong(
                        KEY_TOKEN_EXPIRES_AT,
                        System.currentTimeMillis() + expiresInSeconds.seconds.inWholeMilliseconds
                    )
                }
            }
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun getOAuthClientId(): String? = prefs.getString(KEY_OAUTH_CLIENT_ID, null)

    fun accessTokenExpiresAt(): Long = prefs.getLong(KEY_TOKEN_EXPIRES_AT, 0L)

    fun clearOAuthSession() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TOKEN_EXPIRES_AT)
            .apply()
    }

    /**
     * The in-flight authorization request. Held in storage rather than memory because
     * the browser hand-off can outlive the process that started it. The client id
     * outlives the request itself; refreshing the session needs it.
     */
    fun saveOAuthRequest(clientId: String, state: String, codeVerifier: String) {
        prefs.edit()
            .putString(KEY_OAUTH_CLIENT_ID, clientId)
            .putString(KEY_OAUTH_STATE, state)
            .putString(KEY_OAUTH_VERIFIER, codeVerifier)
            .apply()
    }

    fun getOAuthState(): String? = prefs.getString(KEY_OAUTH_STATE, null)

    fun getOAuthVerifier(): String? = prefs.getString(KEY_OAUTH_VERIFIER, null)

    fun clearOAuthRequest() {
        prefs.edit().remove(KEY_OAUTH_STATE).remove(KEY_OAUTH_VERIFIER).apply()
    }

    // --- Per-agent keys (auto-provisioned via the admin key) ---

    fun saveAgentKeys(email: String, apiKey: String, apiSecret: String) {
        prefs.edit()
            .putString(agentKeyPref(email), apiKey)
            .putString(agentSecretPref(email), apiSecret)
            .apply()
    }

    /**
     * Marks an agent as writing with the app's own signed-in session instead of a
     * minted key, which is how the account the user signed in as writes as itself.
     */
    fun setAgentUsesLoginSession(email: String) {
        prefs.edit().putBoolean(agentLoginSessionPref(email), true).apply()
    }

    fun hasAgentKeys(email: String): Boolean = getAgentToken(email) != null

    fun getAgentToken(email: String): String? {
        if (prefs.getBoolean(agentLoginSessionPref(email), false)) return getAuthToken()
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
    private fun agentLoginSessionPref(email: String) = "agent_login_session_$email"

    companion object {
        private const val KEY_SITE_URL = "site_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_API_SECRET = "api_secret"
        private const val KEY_ACTIVE_AGENT = "active_agent_email"
        private const val KEY_ACCESS_TOKEN = "oauth_access_token"
        private const val KEY_REFRESH_TOKEN = "oauth_refresh_token"
        private const val KEY_TOKEN_EXPIRES_AT = "oauth_token_expires_at"
        private const val KEY_OAUTH_CLIENT_ID = "oauth_client_id"
        private const val KEY_OAUTH_STATE = "oauth_state"
        private const val KEY_OAUTH_VERIFIER = "oauth_code_verifier"
    }
}
