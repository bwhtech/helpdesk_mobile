package io.github.kaulith.helpdeskanalytics.util

object Constants {
    const val DATABASE_NAME = "helpdesk_analytics_db"
    const val ENCRYPTED_PREFERENCES_NAME = "helpdesk_secure_prefs"

    const val NETWORK_TIMEOUT = 30_000L
    const val SYNC_INTERVAL_MINUTES = 30L

    // OkHttp defaults to 5 per host, which throttles the leaderboard's per-agent counts.
    const val MAX_REQUESTS_PER_HOST = 8

    const val CACHE_TTL_TICKETS = 30 * 60 * 1000L
    const val CACHE_TTL_USER = 24 * 60 * 60 * 1000L

    const val SEARCH_DEBOUNCE_MS = 300L

    // Push registration always targets the helpdesk_push app on the FC bench,
    // independent of which Helpdesk site the app reads its data from.
    const val PUSH_BACKEND_URL = "https://helpdesk-mb.fsn.frappe.cloud/"

    // Releases live on GitHub; sideloaded builds have no store to update them.
    const val GITHUB_API_URL = "https://api.github.com/"
    const val RELEASES_OWNER = "kaulith"
    const val RELEASES_REPO = "HelpdeskAnalytics"
}
