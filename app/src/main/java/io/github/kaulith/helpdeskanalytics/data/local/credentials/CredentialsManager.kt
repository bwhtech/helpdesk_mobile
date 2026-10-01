package io.github.kaulith.helpdeskanalytics.data.local.credentials

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import io.github.kaulith.helpdeskanalytics.util.Constants
import java.io.File
import java.security.KeyStore

class CredentialsManager(context: Context, private val aead: Aead) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.CREDENTIALS_PREFERENCES_NAME, Context.MODE_PRIVATE)

    init {
        moveEncryptedPreferences(context)
    }

    fun saveCredentials(siteUrl: String, apiKey: String, apiSecret: String) {
        prefs.edit()
            .putEncrypted(KEY_SITE_URL, siteUrl.trimEnd('/'))
            .putEncrypted(KEY_API_KEY, apiKey)
            .putEncrypted(KEY_API_SECRET, apiSecret)
            .apply()
    }

    fun getSiteUrl(): String? = getString(KEY_SITE_URL)

    fun siteBaseUrl(): String? = getSiteUrl()?.let { "$it/" }

    fun saveSiteUrl(siteUrl: String) {
        prefs.edit().putEncrypted(KEY_SITE_URL, siteUrl.trimEnd('/')).apply()
    }

    fun getApiKey(): String? = getString(KEY_API_KEY)

    fun getApiSecret(): String? = getString(KEY_API_SECRET)

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

    fun saveOAuthSession(accessToken: String, refreshToken: String?) {
        prefs.edit()
            .putEncrypted(KEY_ACCESS_TOKEN, accessToken)
            .apply {
                if (refreshToken != null) putEncrypted(KEY_REFRESH_TOKEN, refreshToken)
            }
            .apply()
    }

    fun getAccessToken(): String? = getString(KEY_ACCESS_TOKEN)

    fun getRefreshToken(): String? = getString(KEY_REFRESH_TOKEN)

    fun getOAuthClientId(): String? = getString(KEY_OAUTH_CLIENT_ID)

    fun clearOAuthSession() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
    }

    /**
     * The in-flight authorization request. Held in storage rather than memory because
     * the browser hand-off can outlive the process that started it. The client id
     * outlives the request itself; refreshing the session needs it.
     */
    fun saveOAuthRequest(clientId: String, state: String, codeVerifier: String) {
        prefs.edit()
            .putEncrypted(KEY_OAUTH_CLIENT_ID, clientId)
            .putEncrypted(KEY_OAUTH_STATE, state)
            .putEncrypted(KEY_OAUTH_VERIFIER, codeVerifier)
            .apply()
    }

    fun getOAuthState(): String? = getString(KEY_OAUTH_STATE)

    fun getOAuthVerifier(): String? = getString(KEY_OAUTH_VERIFIER)

    fun clearOAuthRequest() {
        prefs.edit().remove(KEY_OAUTH_STATE).remove(KEY_OAUTH_VERIFIER).apply()
    }

    // --- Per-agent keys (auto-provisioned via the admin key) ---

    fun saveAgentKeys(email: String, apiKey: String, apiSecret: String) {
        prefs.edit()
            .putEncrypted(agentKeyPref(email), apiKey)
            .putEncrypted(agentSecretPref(email), apiSecret)
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
        val key = getString(agentKeyPref(email)) ?: return null
        val secret = getString(agentSecretPref(email)) ?: return null
        return "token $key:$secret"
    }

    /** The agent the app is currently acting as; null means acting as the admin. */
    fun setActiveAgentEmail(email: String?) {
        prefs.edit().apply {
            if (email != null) putEncrypted(KEY_ACTIVE_AGENT, email) else remove(KEY_ACTIVE_AGENT)
        }.apply()
    }

    fun getActiveAgentEmail(): String? = getString(KEY_ACTIVE_AGENT)

    fun clearCredentials() {
        prefs.edit().clear().apply()
    }

    private fun getString(key: String): String? = prefs.getString(key, null)?.let {
        String(aead.decrypt(Base64.decode(it, Base64.NO_WRAP), key.toByteArray()))
    }

    private fun SharedPreferences.Editor.putEncrypted(key: String, value: String): SharedPreferences.Editor {
        val encrypted = aead.encrypt(value.toByteArray(), key.toByteArray())
        return putString(key, Base64.encodeToString(encrypted, Base64.NO_WRAP))
    }

    /**
     * Moves what 1.1 and earlier wrote with the deprecated EncryptedSharedPreferences into
     * this store, then deletes the old file and its master key. An old store that no
     * longer decrypts is dropped too, so the user signs in again instead of the app
     * failing on every start.
     */
    private fun moveEncryptedPreferences(context: Context) {
        val name = Constants.ENCRYPTED_PREFERENCES_NAME
        if (!File(context.dataDir, "shared_prefs/$name.xml").exists()) return
        val oldValues = runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                name,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).all
        }
            .onFailure { Log.e(TAG, "Failed to read the old credentials", it) }
            .getOrDefault(emptyMap())
        val editor = prefs.edit()
        oldValues.forEach { (key, value) ->
            when (value) {
                is String -> editor.putEncrypted(key, value)
                is Boolean -> editor.putBoolean(key, value)
            }
        }
        if (!editor.commit()) return
        context.deleteSharedPreferences(name)
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            .deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
    }

    private fun agentKeyPref(email: String) = "agent_key_$email"
    private fun agentSecretPref(email: String) = "agent_secret_$email"
    private fun agentLoginSessionPref(email: String) = "agent_login_session_$email"

    companion object {
        private const val TAG = "CredentialsManager"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEYSET_NAME = "credentials_keyset"
        private const val MASTER_KEY_ALIAS = "helpdesk_credentials_master_key"
        private const val KEY_SITE_URL = "site_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_API_SECRET = "api_secret"
        private const val KEY_ACTIVE_AGENT = "active_agent_email"
        private const val KEY_ACCESS_TOKEN = "oauth_access_token"
        private const val KEY_REFRESH_TOKEN = "oauth_refresh_token"
        private const val KEY_OAUTH_CLIENT_ID = "oauth_client_id"
        private const val KEY_OAUTH_STATE = "oauth_state"
        private const val KEY_OAUTH_VERIFIER = "oauth_code_verifier"

        fun keystoreAead(context: Context): Aead {
            AeadConfig.register()
            return AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, Constants.CREDENTIALS_KEYSET_PREFERENCES_NAME)
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri("android-keystore://$MASTER_KEY_ALIAS")
                .build()
                .keysetHandle
                .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        }
    }
}
