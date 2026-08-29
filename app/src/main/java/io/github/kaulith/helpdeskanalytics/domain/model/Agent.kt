package io.github.kaulith.helpdeskanalytics.domain.model

data class Agent(
    val email: String,
    val name: String,
    val avatarUrl: String? = null
)
