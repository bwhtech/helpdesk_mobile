package io.github.kaulith.helpdeskanalytics.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "helpdesk_prefs")

class PreferencesManager(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val COLOR_SCHEME = stringPreferencesKey("color_scheme")
        val LAST_SYNC = longPreferencesKey("last_sync")
        val ACTIVE_AGENT_EMAIL = stringPreferencesKey("active_agent_email")
        val ACTIVE_AGENT_NAME = stringPreferencesKey("active_agent_name")
        val LOGGED_IN_USER_EMAIL = stringPreferencesKey("logged_in_user_email")
        val AGENT_COUNTS = stringPreferencesKey("agent_counts")
        val AGENT_COUNTS_SYNCED_AT = longPreferencesKey("agent_counts_synced_at")
        val DISMISSED_UPDATE_VERSION = stringPreferencesKey("dismissed_update_version")
    }

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
        prefs[Keys.LAST_SYNC] ?: 0L
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setLastSync(timestamp: Long) {
        context.dataStore.edit { it[Keys.LAST_SYNC] = timestamp }
    }

    val agentCounts: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.AGENT_COUNTS]
    }

    val agentCountsSyncedAt: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.AGENT_COUNTS_SYNCED_AT] ?: 0L
    }

    suspend fun setAgentCounts(json: String, timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AGENT_COUNTS] = json
            prefs[Keys.AGENT_COUNTS_SYNCED_AT] = timestamp
        }
    }

    val activeAgentEmail: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_AGENT_EMAIL]
    }

    val activeAgentName: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_AGENT_NAME]
    }

    val loggedInUserEmail: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.LOGGED_IN_USER_EMAIL]
    }

    suspend fun setLoggedInUserEmail(email: String?) {
        context.dataStore.edit { prefs ->
            if (email != null) prefs[Keys.LOGGED_IN_USER_EMAIL] = email
            else prefs.remove(Keys.LOGGED_IN_USER_EMAIL)
        }
    }

    suspend fun setActiveAgent(email: String?, name: String?) {
        context.dataStore.edit { prefs ->
            if (email != null && name != null) {
                prefs[Keys.ACTIVE_AGENT_EMAIL] = email
                prefs[Keys.ACTIVE_AGENT_NAME] = name
            } else {
                prefs.remove(Keys.ACTIVE_AGENT_EMAIL)
                prefs.remove(Keys.ACTIVE_AGENT_NAME)
            }
        }
    }

    val dismissedUpdateVersion: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.DISMISSED_UPDATE_VERSION]
    }

    suspend fun setDismissedUpdateVersion(version: String) {
        context.dataStore.edit { it[Keys.DISMISSED_UPDATE_VERSION] = version }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
