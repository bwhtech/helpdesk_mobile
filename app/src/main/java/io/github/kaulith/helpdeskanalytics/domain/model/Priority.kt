package io.github.kaulith.helpdeskanalytics.domain.model

enum class Priority(val value: String, val weight: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    URGENT("Urgent", 4);

    companion object {
        fun fromValue(value: String): Priority {
            return entries.find { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown priority: $value")
        }
    }
}
