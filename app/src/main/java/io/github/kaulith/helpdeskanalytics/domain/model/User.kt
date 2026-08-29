package io.github.kaulith.helpdeskanalytics.domain.model

data class User(
    val email: String,
    val fullName: String?,
    val roles: List<String>,
    val hasTeamLeadPermission: Boolean
) {
    fun canViewLeaderboard(): Boolean {
        return true
    }
}
