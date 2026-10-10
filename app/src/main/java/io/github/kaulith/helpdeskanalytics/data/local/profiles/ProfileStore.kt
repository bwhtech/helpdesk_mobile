package io.github.kaulith.helpdeskanalytics.data.local.profiles

import android.content.Context
import io.github.kaulith.helpdeskanalytics.util.Constants

/** The helpdesk sites the app holds a profile for, and which of them is in use. */
class ProfileStore(context: Context) {

    private val prefs =
        context.getSharedPreferences(Constants.PROFILES_PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun profileIds(): List<String> =
        prefs.getString(KEY_PROFILE_IDS, null)?.split(SEPARATOR).orEmpty()

    fun activeProfileId(): String? = prefs.getString(KEY_ACTIVE_PROFILE, null)

    /** Puts a profile in use, adding it when it is not a known one yet. */
    fun activate(profileId: String) {
        prefs.edit()
            .putString(KEY_PROFILE_IDS, (profileIds() + profileId).distinct().joinToString(SEPARATOR))
            .putString(KEY_ACTIVE_PROFILE, profileId)
            .apply()
    }

    /** A profile that was in use hands over to the first one left. */
    fun remove(profileId: String) {
        val remaining = profileIds() - profileId
        val active = activeProfileId().takeIf { it != profileId } ?: remaining.firstOrNull()
        prefs.edit().apply {
            if (remaining.isEmpty()) remove(KEY_PROFILE_IDS)
            else putString(KEY_PROFILE_IDS, remaining.joinToString(SEPARATOR))
            if (active != null) putString(KEY_ACTIVE_PROFILE, active) else remove(KEY_ACTIVE_PROFILE)
        }.apply()
    }

    companion object {
        /** What the app stored before it had profiles; its files keep their old names. */
        const val FIRST_PROFILE_ID = "default"

        private const val KEY_PROFILE_IDS = "profile_ids"
        private const val KEY_ACTIVE_PROFILE = "active_profile"
        private const val SEPARATOR = ","

        fun fileName(name: String, profileId: String): String =
            if (profileId == FIRST_PROFILE_ID) name else "${name}_$profileId"
    }
}
