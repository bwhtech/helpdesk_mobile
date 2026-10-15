package io.github.kaulith.helpdeskanalytics.data.local.credentials

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import io.github.kaulith.helpdeskanalytics.data.local.profiles.ProfileStore
import io.github.kaulith.helpdeskanalytics.util.Constants
import java.io.File
import java.security.KeyStore
import java.util.UUID

/**
 * The credentials of the profile in use. Every profile has a file of its own, so
 * signing out of one site or failing to sign in to another leaves the rest alone.
 */
class CredentialsManager(
    private val context: Context,
    private val aead: Aead,
    private val profileStore: ProfileStore
) {

    private val prefs: SharedPreferences?
        get() = profileStore.activeProfileId()?.let(::profilePrefs)

    private val oAuthRequestPrefs: SharedPreferences =
        context.getSharedPreferences(Constants.OAUTH_REQUEST_PREFERENCES_NAME, Context.MODE_PRIVATE)

    init {
        deleteOldEncryptedPreferences(context)
        adoptFirstProfile()
    }

    fun activeProfileId(): String? = profileStore.activeProfileId()

    fun hasProfile(siteUrl: String): Boolean = profileIdOf(siteUrl) != null

    /** The site of every profile that is signed in, by profile id. */
    fun signedInSites(): Map<String, String> =
        profileStore.profileIds().mapNotNull { profileId ->
            val prefs = profilePrefs(profileId)
            val isSignedIn = prefs.contains(KEY_ACCESS_TOKEN) ||
                (prefs.contains(KEY_API_KEY) && prefs.contains(KEY_API_SECRET))
            prefs.getDecrypted(KEY_SITE_URL)?.takeIf { isSignedIn }?.let { profileId to it }
        }.toMap()

    /** Signs in to a site with API keys; see [activateProfile] for where they land. */
    fun saveCredentials(siteUrl: String, apiKey: String, apiSecret: String) {
        activateProfile(siteUrl)
        edit {
            putEncrypted(KEY_SITE_URL, siteUrl.trimEnd('/'))
            putEncrypted(KEY_API_KEY, apiKey)
            putEncrypted(KEY_API_SECRET, apiSecret)
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
        }
    }

    fun getSiteUrl(): String? = getString(KEY_SITE_URL)

    fun siteBaseUrl(): String? = getSiteUrl()?.let { "$it/" }

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

    /**
     * Signs in to a site over OAuth; see [activateProfile] for where it lands. The
     * client id stays with the profile because refreshing the session needs it.
     */
    fun saveOAuthLogin(siteUrl: String, clientId: String, accessToken: String, refreshToken: String?) {
        activateProfile(siteUrl)
        edit {
            putEncrypted(KEY_SITE_URL, siteUrl.trimEnd('/'))
            putEncrypted(KEY_OAUTH_CLIENT_ID, clientId)
            remove(KEY_API_KEY)
            remove(KEY_API_SECRET)
        }
        saveOAuthSession(accessToken, refreshToken)
    }

    fun saveOAuthSession(
        accessToken: String,
        refreshToken: String?,
        profileId: String? = activeProfileId()
    ) = edit(profileId) {
        putEncrypted(KEY_ACCESS_TOKEN, accessToken)
        if (refreshToken != null) putEncrypted(KEY_REFRESH_TOKEN, refreshToken)
    }

    fun getAccessToken(): String? = getString(KEY_ACCESS_TOKEN)

    fun getRefreshToken(): String? = getString(KEY_REFRESH_TOKEN)

    fun getOAuthClientId(): String? = getString(KEY_OAUTH_CLIENT_ID)

    fun clearOAuthSession(profileId: String? = activeProfileId()) = edit(profileId) {
        remove(KEY_ACCESS_TOKEN)
        remove(KEY_REFRESH_TOKEN)
    }

    /** Signs the profile in use out and keeps the rest of it for signing back in. */
    fun clearSession() = edit {
        remove(KEY_API_KEY)
        remove(KEY_API_SECRET)
        remove(KEY_ACCESS_TOKEN)
        remove(KEY_REFRESH_TOKEN)
    }

    /**
     * The in-flight authorization request. Held in storage rather than memory because
     * the browser hand-off can outlive the process that started it, and outside any
     * profile because the site has none until the sign-in goes through.
     */
    fun saveOAuthRequest(request: OAuthRequest) {
        oAuthRequestPrefs.edit()
            .putEncrypted(KEY_SITE_URL, request.siteUrl)
            .putEncrypted(KEY_OAUTH_CLIENT_ID, request.clientId)
            .putEncrypted(KEY_OAUTH_STATE, request.state)
            .putEncrypted(KEY_OAUTH_VERIFIER, request.codeVerifier)
            .apply()
    }

    fun getOAuthRequest(): OAuthRequest? {
        val siteUrl = oAuthRequestPrefs.getDecrypted(KEY_SITE_URL)
        val clientId = oAuthRequestPrefs.getDecrypted(KEY_OAUTH_CLIENT_ID)
        val state = oAuthRequestPrefs.getDecrypted(KEY_OAUTH_STATE)
        val codeVerifier = oAuthRequestPrefs.getDecrypted(KEY_OAUTH_VERIFIER)
        if (siteUrl == null || clientId == null || state == null || codeVerifier == null) return null
        return OAuthRequest(siteUrl, clientId, state, codeVerifier)
    }

    fun clearOAuthRequest() {
        oAuthRequestPrefs.edit().clear().apply()
    }

    // --- Per-agent keys (auto-provisioned via the admin key) ---

    fun saveAgentKeys(email: String, apiKey: String, apiSecret: String) = edit {
        putEncrypted(agentKeyPref(email), apiKey)
        putEncrypted(agentSecretPref(email), apiSecret)
    }

    /**
     * Marks an agent as writing with the app's own signed-in session instead of a
     * minted key, which is how the account the user signed in as writes as itself.
     */
    fun setAgentUsesLoginSession(email: String) = edit {
        putBoolean(agentLoginSessionPref(email), true)
    }

    fun hasAgentKeys(email: String): Boolean = getAgentToken(email) != null

    fun getAgentToken(email: String): String? {
        if (prefs?.getBoolean(agentLoginSessionPref(email), false) == true) return getAuthToken()
        val key = getString(agentKeyPref(email)) ?: return null
        val secret = getString(agentSecretPref(email)) ?: return null
        return "token $key:$secret"
    }

    /** The agent the app is currently acting as; null means acting as the admin. */
    fun setActiveAgentEmail(email: String?) = edit {
        if (email != null) putEncrypted(KEY_ACTIVE_AGENT, email) else remove(KEY_ACTIVE_AGENT)
    }

    fun getActiveAgentEmail(): String? = getString(KEY_ACTIVE_AGENT)

    /** Removes the profile in use and what it kept; another, when there is one, takes over. */
    fun removeProfile() {
        val profileId = profileStore.activeProfileId() ?: return
        // Written out before the file goes, or a pending write would bring it back.
        profilePrefs(profileId).edit().clear().commit()
        context.deleteSharedPreferences(profileFileName(profileId))
        profileStore.remove(profileId)
    }

    /** Puts the profile of a site in use, starting one for a site that has none. */
    private fun activateProfile(siteUrl: String) {
        profileStore.activate(profileIdOf(siteUrl) ?: UUID.randomUUID().toString())
    }

    private fun profileIdOf(siteUrl: String): String? {
        val site = siteUrl.trimEnd('/')
        return profileStore.profileIds().firstOrNull {
            profilePrefs(it).getDecrypted(KEY_SITE_URL).equals(site, ignoreCase = true)
        }
    }

    private fun profileFileName(profileId: String) =
        ProfileStore.storageName(Constants.CREDENTIALS_PREFERENCES_NAME, profileId)

    private fun profilePrefs(profileId: String): SharedPreferences =
        context.getSharedPreferences(profileFileName(profileId), Context.MODE_PRIVATE)

    private fun edit(profileId: String? = activeProfileId(), block: SharedPreferences.Editor.() -> Unit) {
        profileId?.let(::profilePrefs)?.edit()?.apply(block)?.apply()
    }

    private fun getString(key: String): String? = prefs?.getDecrypted(key)

    private fun SharedPreferences.getDecrypted(key: String): String? = getString(key, null)?.let {
        String(aead.decrypt(Base64.decode(it, Base64.NO_WRAP), key.toByteArray()))
    }

    private fun SharedPreferences.Editor.putEncrypted(key: String, value: String): SharedPreferences.Editor {
        val encrypted = aead.encrypt(value.toByteArray(), key.toByteArray())
        return putString(key, Base64.encodeToString(encrypted, Base64.NO_WRAP))
    }

    /**
     * 1.1 and earlier kept credentials in EncryptedSharedPreferences, which this app no
     * longer reads. Whatever a phone still holds from then is deleted, with its key, so
     * no secret is left behind; the user signs in again.
     */
    private fun deleteOldEncryptedPreferences(context: Context) {
        if (!File(context.dataDir, "shared_prefs/$OLD_PREFERENCES_NAME.xml").exists()) return
        context.deleteSharedPreferences(OLD_PREFERENCES_NAME)
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(OLD_MASTER_KEY_ALIAS)
    }

    /** What 1.2 and earlier stored belongs to no profile; it becomes the first, in place. */
    private fun adoptFirstProfile() {
        if (profileStore.profileIds().isNotEmpty()) return
        if (profilePrefs(ProfileStore.FIRST_PROFILE_ID).all.isEmpty()) return
        profileStore.activate(ProfileStore.FIRST_PROFILE_ID)
    }

    private fun agentKeyPref(email: String) = "agent_key_$email"
    private fun agentSecretPref(email: String) = "agent_secret_$email"
    private fun agentLoginSessionPref(email: String) = "agent_login_session_$email"

    data class OAuthRequest(
        val siteUrl: String,
        val clientId: String,
        val state: String,
        val codeVerifier: String
    )

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val OLD_PREFERENCES_NAME = "helpdesk_secure_prefs"
        private const val OLD_MASTER_KEY_ALIAS = "_androidx_security_master_key_"
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
