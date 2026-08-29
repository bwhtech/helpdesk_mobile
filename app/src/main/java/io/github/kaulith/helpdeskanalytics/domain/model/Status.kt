package io.github.kaulith.helpdeskanalytics.domain.model

enum class Status(val value: String, val displayName: String) {
    OPEN("Open", "Open"),
    REPLIED("Replied", "Replied"),
    AWAITING_APPROVAL("Awaiting Approval", "Awaiting Approval"),
    RESOLVED("Resolved", "Resolved"),
    CLOSED("Closed", "Closed");

    companion object {
        fun fromValue(value: String): Status {
            return entries.find { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown status: $value")
        }
    }
}
