package io.github.kaulith.helpdeskanalytics.testing

import io.github.kaulith.helpdeskanalytics.data.remote.api.AgentSessionManager
import io.github.kaulith.helpdeskanalytics.util.Result

class FakeAgentSessionManager : AgentSessionManager {

    override fun tokenForRequest(isWrite: Boolean): String? = null

    override fun hasActiveAgent() = false

    override fun canWrite() = false

    override suspend fun needsWriteKey(email: String) = false

    override suspend fun activate(email: String, provisionWriteKey: Boolean): Result<Unit> = Result.Success(Unit)

    override fun deactivate() = Unit
}
