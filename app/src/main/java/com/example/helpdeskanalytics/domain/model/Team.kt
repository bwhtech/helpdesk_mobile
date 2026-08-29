package com.example.helpdeskanalytics.domain.model

data class Team(
    val name: String,
    val members: List<String> = emptyList()
)
