package io.github.kaulith.helpdeskanalytics.domain.model

data class Team(
    val name: String,
    val members: List<String> = emptyList()
)
