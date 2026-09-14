package io.github.kaulith.helpdeskanalytics.ui.screens.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kaulith.helpdeskanalytics.domain.model.Agent
import io.github.kaulith.helpdeskanalytics.domain.model.Comment
import io.github.kaulith.helpdeskanalytics.domain.model.Communication
import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.domain.model.Ticket
import io.github.kaulith.helpdeskanalytics.domain.repository.AgentRepository
import io.github.kaulith.helpdeskanalytics.domain.repository.TicketRepository
import io.github.kaulith.helpdeskanalytics.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TicketDetailUiState(
    val ticket: Ticket? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val comments: List<Comment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val communications: List<Communication> = emptyList(),
    val isLoadingCommunications: Boolean = false,
    val communicationsError: String? = null,
    val isUpdating: Boolean = false,
    val isAddingComment: Boolean = false,
    val isSendingReply: Boolean = false,
    val activeAgent: Agent? = null,
    val canWrite: Boolean = false,
    val showSelectAgentPrompt: Boolean = false,
    val snackbarMessage: String? = null
)

class TicketDetailViewModel(
    private val repository: TicketRepository,
    private val agentRepository: AgentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketDetailUiState())
    val uiState: StateFlow<TicketDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            agentRepository.getActiveAgent().collect { agent ->
                _uiState.update { it.copy(activeAgent = agent, canWrite = repository.canWrite()) }
            }
        }
    }

    // Writes must be attributed to a chosen agent; block and prompt if none is active.
    private fun requireActiveAgent(): Boolean {
        if (_uiState.value.activeAgent == null) {
            _uiState.update { it.copy(showSelectAgentPrompt = true) }
            return false
        }
        return true
    }

    fun dismissSelectAgentPrompt() {
        _uiState.update { it.copy(showSelectAgentPrompt = false) }
    }

    fun loadTicket(ticketId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getTicketById(ticketId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(ticket = result.data, isLoading = false) }
                    loadConversation(ticketId)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.exception.message ?: "Failed to load ticket")
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun loadConversation(ticketId: String) {
        viewModelScope.launch {
            val cachedComments = repository.getCachedComments(ticketId)
            _uiState.update {
                it.copy(
                    comments = cachedComments,
                    isLoadingComments = cachedComments.isEmpty(),
                    isLoadingCommunications = it.communications.isEmpty(),
                    communicationsError = null
                )
            }
            when (val result = repository.getConversation(ticketId)) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        comments = result.data.comments,
                        isLoadingComments = false,
                        communications = result.data.communications,
                        isLoadingCommunications = false
                    )
                }
                is Result.Error -> _uiState.update {
                    it.copy(
                        isLoadingComments = false,
                        isLoadingCommunications = false,
                        communicationsError = result.exception.message ?: "Couldn't load replies"
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    fun sendReply(ticketId: String, message: String) = runWrite(
        setBusy = { copy(isSendingReply = it) },
        successMessage = "Reply sent",
        failureMessage = "Failed to send reply",
        write = { repository.sendReply(ticketId, message) }
    ) {
        loadConversation(ticketId)
        // reply_via_agent moves the ticket to "Replied", so refresh quietly
        val fresh = repository.getTicketById(ticketId, force = true)
        if (fresh is Result.Success) {
            _uiState.update { it.copy(ticket = fresh.data) }
        }
    }

    fun updateStatus(ticketId: String, status: Status) = runWrite(
        setBusy = { copy(isUpdating = it) },
        successMessage = "Status updated to ${status.value}",
        failureMessage = "Failed to update status",
        write = { repository.updateTicketStatus(ticketId, status) }
    ) { ticket ->
        _uiState.update { it.copy(ticket = ticket) }
    }

    fun updatePriority(ticketId: String, priority: Priority) = runWrite(
        setBusy = { copy(isUpdating = it) },
        successMessage = "Priority updated to ${priority.value}",
        failureMessage = "Failed to update priority",
        write = { repository.updateTicketPriority(ticketId, priority) }
    ) { ticket ->
        _uiState.update { it.copy(ticket = ticket) }
    }

    fun addComment(ticketId: String, content: String) = runWrite(
        setBusy = { copy(isAddingComment = it) },
        successMessage = "Comment added",
        failureMessage = "Failed to add comment",
        write = { repository.addComment(ticketId, content) }
    ) {
        loadConversation(ticketId)
    }

    private fun <T> runWrite(
        setBusy: TicketDetailUiState.(Boolean) -> TicketDetailUiState,
        successMessage: String,
        failureMessage: String,
        write: suspend () -> Result<T>,
        onSuccess: suspend (T) -> Unit
    ) {
        if (!requireActiveAgent()) return
        viewModelScope.launch {
            _uiState.update { it.setBusy(true) }
            when (val result = write()) {
                is Result.Success -> {
                    _uiState.update { it.setBusy(false).copy(snackbarMessage = successMessage) }
                    onSuccess(result.data)
                }
                is Result.Error -> _uiState.update { it.setBusy(false).copy(snackbarMessage = failureMessage) }
                is Result.Loading -> {}
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
