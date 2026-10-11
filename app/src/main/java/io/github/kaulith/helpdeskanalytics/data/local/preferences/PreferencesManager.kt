package io.github.kaulith.helpdeskanalytics.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.kaulith.helpdeskanalytics.data.local.profiles.ProfileStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "helpdesk_prefs")

class PreferencesManager(
    private val context: Context,
    private val profileStore: ProfileStore
) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val COLOR_SCHEME = stringPreferencesKey("color_scheme")
        val DISMISSED_UPDATE_VERSION = stringPreferencesKey("dismissed_update_version")
    }

    /** What is kept per site. Each profile has its own, so none reads another's. */
    private class SiteKeys(profileId: String) {
        val lastSync = longPreferencesKey(ProfileStore.storageName("last_sync", profileId))
        val activeAgentEmail = stringPreferencesKey(ProfileStore.storageName("active_agent_email", profileId))
        val activeAgentName = stringPreferencesKey(ProfileStore.storageName("active_agent_name", profileId))
        val loggedInUserEmail = stringPreferencesKey(ProfileStore.storageName("logged_in_user_email", profileId))
        val agentCounts = stringPreferencesKey(ProfileStore.storageName("agent_counts", profileId))
        val agentCountsSyncedAt = longPreferencesKey(ProfileStore.storageName("agent_counts_synced_at", profileId))
        val userSyncedAt = longPreferencesKey(ProfileStore.storageName("user_synced_at", profileId))
        val ticketPollState = stringPreferencesKey(ProfileStore.storageName("ticket_poll_state", profileId))

        val all = listOf(
            lastSync, activeAgentEmail, activeAgentName, loggedInUserEmail,
            agentCounts, agentCountsSyncedAt, userSyncedAt, ticketPollState
        )
    }

    private val siteKeys: SiteKeys
        get() = SiteKeys(profileStore.activeProfileId() ?: SIGNED_OUT_PROFILE_ID)

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE] ?: "system"
    }

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DYNAMIC_COLOR] ?: true
    }

    val colorScheme: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.COLOR_SCHEME] ?: "ocean"
    }

    suspend fun setColorScheme(scheme: String) {
        context.dataStore.edit { it[Keys.COLOR_SCHEME] = scheme }
    }

    val lastSync: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[siteKeys.lastSync] ?: 0L
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setLastSync(timestamp: Long) {
        context.dataStore.edit { it[siteKeys.lastSync] = timestamp }
    }

    val userSyncedAt: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[siteKeys.userSyncedAt] ?: 0L
    }

    suspend fun setUserSyncedAt(timestamp: Long) {
        context.dataStore.edit { it[siteKeys.userSyncedAt] = timestamp }
    }

    val agentCounts: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[siteKeys.agentCounts]
    }

    val agentCountsSyncedAt: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[siteKeys.agentCountsSyncedAt] ?: 0L
    }

    suspend fun setAgentCounts(json: String, timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[siteKeys.agentCounts] = json
            prefs[siteKeys.agentCountsSyncedAt] = timestamp
        }
    }

    val activeAgentEmail: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[siteKeys.activeAgentEmail]
    }

    val activeAgentName: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[siteKeys.activeAgentName]
    }

    val loggedInUserEmail: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[siteKeys.loggedInUserEmail]
    }

    suspend fun setLoggedInUserEmail(email: String?) {
        context.dataStore.edit { prefs ->
            if (email != null) prefs[siteKeys.loggedInUserEmail] = email
            else prefs.remove(siteKeys.loggedInUserEmail)
        }
    }

    suspend fun setActiveAgent(email: String?, name: String?) {
        context.dataStore.edit { prefs ->
            if (email != null && name != null) {
                prefs[siteKeys.activeAgentEmail] = email
                prefs[siteKeys.activeAgentName] = name
            } else {
                prefs.remove(siteKeys.activeAgentEmail)
                prefs.remove(siteKeys.activeAgentName)
            }
        }
    }

    val dismissedUpdateVersion: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.DISMISSED_UPDATE_VERSION]
    }

    suspend fun setDismissedUpdateVersion(version: String) {
        context.dataStore.edit { it[Keys.DISMISSED_UPDATE_VERSION] = version }
    }

    val ticketPollState: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[siteKeys.ticketPollState]
    }

    suspend fun setTicketPollState(json: String) {
        context.dataStore.edit { it[siteKeys.ticketPollState] = json }
    }

    /** Forgets what is kept for the profile in use; the look of the app is not its to reset. */
    suspend fun clearProfile() {
        context.dataStore.edit { prefs -> siteKeys.all.forEach { prefs.remove(it) } }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    private companion object {
        const val SIGNED_OUT_PROFILE_ID = "signed_out"
    }
}
