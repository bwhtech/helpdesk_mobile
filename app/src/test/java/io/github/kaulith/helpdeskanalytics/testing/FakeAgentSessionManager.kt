package io.github.kaulith.helpdeskanalytics.testing

import io.github.kaulith.helpdeskanalytics.data.remote.api.AgentSessionManager
import io.github.kaulith.helpdeskanalytics.util.Result

class FakeAgentSessionManager : AgentSessionManager {

    var isWriteKeyMissing = false
    var activation: Result<Unit> = Result.Success(Unit)

    override fun tokenForRequest(isWrite: Boolean): String? = null

    override fun canWrite() = false

    override suspend fun needsWriteKey(email: String) = isWriteKeyMissing

    override suspend fun activate(email: String, provisionWriteKey: Boolean) = activation

    override fun deactivate() = Unit
}
