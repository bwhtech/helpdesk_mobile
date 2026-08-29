package com.example.helpdeskanalytics.domain.model

enum class Priority(val value: String, val displayName: String, val weight: Int) {
    LOW("Low", "Low", 1),
    MEDIUM("Medium", "Medium", 2),
    HIGH("High", "High", 3),
    URGENT("Urgent", "Urgent", 4);

    companion object {
        fun fromValue(value: String): Priority {
            return entries.find { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown priority: $value")
        }
    }
}
