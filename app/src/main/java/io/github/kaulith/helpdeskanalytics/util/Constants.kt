package io.github.kaulith.helpdeskanalytics.util

object Constants {
    const val DATABASE_NAME = "helpdesk_analytics_db"
    const val ENCRYPTED_PREFERENCES_NAME = "helpdesk_secure_prefs"
    const val CREDENTIALS_PREFERENCES_NAME = "helpdesk_credentials"
    const val CREDENTIALS_KEYSET_PREFERENCES_NAME = "helpdesk_credentials_keyset"

    const val NETWORK_TIMEOUT = 30_000L
    const val SYNC_INTERVAL_MINUTES = 30L

    // OkHttp defaults to 5 per host, which throttles the leaderboard's per-agent counts.
    const val MAX_REQUESTS_PER_HOST = 8

    const val CACHE_TTL_TICKETS = 30 * 60 * 1000L
    const val CACHE_TTL_USER = 24 * 60 * 60 * 1000L

    const val SEARCH_DEBOUNCE_MS = 300L

    // The bench that holds the Firebase key. It also polls POLLED_SITE_HOST, which
    // cannot install helpdesk_push itself, so only that site's devices register here.
    const val PUSH_BACKEND_URL = "https://helpdesk-mb.fsn.frappe.cloud/"
    const val POLLED_SITE_HOST = "support.frappe.io"

    // Releases live on GitHub; sideloaded builds have no store to update them.
    const val GITHUB_API_URL = "https://api.github.com/"
    const val RELEASES_OWNER = "kaulith"
    const val RELEASES_REPO = "helpdesk-mobile"
}
