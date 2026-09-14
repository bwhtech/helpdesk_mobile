package io.github.kaulith.helpdeskanalytics.domain.model

enum class Status(val value: String) {
    OPEN("Open"),
    REPLIED("Replied"),
    AWAITING_APPROVAL("Awaiting Approval"),
    RESOLVED("Resolved"),
    CLOSED("Closed");

    companion object {
        fun fromValue(value: String?): Status =
            entries.find { it.value.equals(value, ignoreCase = true) } ?: OPEN
    }
}
