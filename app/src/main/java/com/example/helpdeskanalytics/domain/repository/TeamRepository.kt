package com.example.helpdeskanalytics.domain.repository

import com.example.helpdeskanalytics.domain.model.Team
import com.example.helpdeskanalytics.util.Result
import kotlinx.coroutines.flow.Flow

interface TeamRepository {
    fun getTeams(): Flow<Result<List<Team>>>
    suspend fun refreshTeams(): Result<List<Team>>
}
