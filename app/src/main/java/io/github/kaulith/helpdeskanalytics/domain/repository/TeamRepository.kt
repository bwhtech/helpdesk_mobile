package io.github.kaulith.helpdeskanalytics.domain.repository

import io.github.kaulith.helpdeskanalytics.domain.model.Team
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.flow.Flow

interface TeamRepository {
    fun getTeams(): Flow<Result<List<Team>>>
    suspend fun refreshTeams(): Result<List<Team>>
}
