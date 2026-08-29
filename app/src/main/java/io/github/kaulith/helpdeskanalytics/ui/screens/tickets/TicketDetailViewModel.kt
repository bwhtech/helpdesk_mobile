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
                    loadComments(ticketId)
                    loadCommunications(ticketId)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.exception.message ?: "Failed to load ticket")
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun loadComments(ticketId: String) {
        viewModelScope.launch {
            repository.getComments(ticketId).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update {
                        it.copy(isLoadingComments = it.comments.isEmpty())
                    }
                    is Result.Success -> _uiState.update {
                        it.copy(comments = result.data, isLoadingComments = false)
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoadingComments = false)
                    }
                }
            }
        }
    }

    private fun loadCommunications(ticketId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoadingCommunications = it.communications.isEmpty(), communicationsError = null)
            }
            when (val result = repository.getCommunications(ticketId)) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        communications = result.data,
                        isLoadingCommunications = false,
                        communicationsError = null
                    )
                }
                is Result.Error -> _uiState.update {
                    it.copy(
                        isLoadingCommunications = false,
                        communicationsError = result.exception.message ?: "Couldn't load replies"
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    fun sendReply(ticketId: String, message: String) {
        if (!requireActiveAgent()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingReply = true) }
            when (repository.sendReply(ticketId, message)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSendingReply = false, snackbarMessage = "Reply sent") }
                    loadCommunications(ticketId)
                    // reply_via_agent moves the ticket to "Replied", so refresh quietly
                    val fresh = repository.getTicketById(ticketId, force = true)
                    if (fresh is Result.Success) {
                        _uiState.update { it.copy(ticket = fresh.data) }
                    }
                }
                is Result.Error -> _uiState.update {
                    it.copy(isSendingReply = false, snackbarMessage = "Failed to send reply")
                }
                is Result.Loading -> {}
            }
        }
    }

    fun updateStatus(ticketId: String, status: Status) {
        if (!requireActiveAgent()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            when (val result = repository.updateTicketStatus(ticketId, status)) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        ticket = result.data,
                        isUpdating = false,
                        snackbarMessage = "Status updated to ${status.displayName}"
                    )
                }
                is Result.Error -> _uiState.update {
                    it.copy(
                        isUpdating = false,
                        snackbarMessage = "Failed to update status"
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    fun updatePriority(ticketId: String, priority: Priority) {
        if (!requireActiveAgent()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            when (val result = repository.updateTicketPriority(ticketId, priority)) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        ticket = result.data,
                        isUpdating = false,
                        snackbarMessage = "Priority updated to ${priority.displayName}"
                    )
                }
                is Result.Error -> _uiState.update {
                    it.copy(
                        isUpdating = false,
                        snackbarMessage = "Failed to update priority"
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    fun addComment(ticketId: String, content: String) {
        if (!requireActiveAgent()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingComment = true) }
            when (repository.addComment(ticketId, content)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isAddingComment = false,
                            snackbarMessage = "Comment added"
                        )
                    }
                    loadComments(ticketId)
                }
                is Result.Error -> _uiState.update {
                    it.copy(
                        isAddingComment = false,
                        snackbarMessage = "Failed to add comment"
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
