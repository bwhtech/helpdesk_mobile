package io.github.kaulith.helpdeskanalytics.data.repository

import io.github.kaulith.helpdeskanalytics.data.local.database.dao.TeamDao
import io.github.kaulith.helpdeskanalytics.data.mapper.toDomain
import io.github.kaulith.helpdeskanalytics.data.mapper.toEntity
import io.github.kaulith.helpdeskanalytics.data.remote.api.ApiServiceProvider
import io.github.kaulith.helpdeskanalytics.domain.model.Team
import io.github.kaulith.helpdeskanalytics.domain.repository.TeamRepository
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class FrappeTeamRepository(
    private val apiServiceProvider: ApiServiceProvider,
    private val teamDao: TeamDao
) : TeamRepository {

    override fun getTeams(): Flow<Result<List<Team>>> = flow {
        emit(Result.Loading)

        val cached = teamDao.getAllTeams().first()
        if (cached.isNotEmpty()) {
            emit(Result.Success(cached.map { it.toDomain() }))
        }

        try {
            val teams = fetchTeamsWithMembers()
            teamDao.deleteAll()
            teamDao.insertAll(teams.map { it.toEntity() })
            emit(Result.Success(teams))
        } catch (e: Exception) {
            if (cached.isEmpty()) {
                emit(Result.Error(e))
            }
        }
    }

    override suspend fun refreshTeams(): Result<List<Team>> {
        return try {
            val teams = fetchTeamsWithMembers()
            teamDao.deleteAll()
            teamDao.insertAll(teams.map { it.toEntity() })
            Result.Success(teams)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun fetchTeamsWithMembers(): List<Team> {
        val service = apiServiceProvider.getService()
        val teamList = service.getTeams().data
        return coroutineScope {
            teamList.map { teamDto ->
                async { service.getTeam(teamDto.name).data.toDomain() }
            }.awaitAll()
        }
    }
}
