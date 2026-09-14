package io.github.kaulith.helpdeskanalytics.testing

import io.github.kaulith.helpdeskanalytics.data.remote.api.AgentSessionManager

class FakeAgentSessionManager : AgentSessionManager {

    override fun tokenForRequest(isWrite: Boolean): String? = null

    override fun canWrite() = false

    override suspend fun needsWriteKey(email: String) = false

    override suspend fun activate(email: String, provisionWriteKey: Boolean) = Unit

    override fun deactivate() = Unit
}
