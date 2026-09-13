package io.github.kaulith.helpdeskanalytics.testing

import io.github.kaulith.helpdeskanalytics.domain.model.Team
import io.github.kaulith.helpdeskanalytics.domain.repository.TeamRepository
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeTeamRepository : TeamRepository {

    override fun getTeams(): Flow<Result<List<Team>>> = flowOf(Result.Success(emptyList()))

    override suspend fun refreshTeams(): Result<List<Team>> = Result.Success(emptyList())
}
