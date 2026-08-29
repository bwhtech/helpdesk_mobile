package io.github.kaulith.helpdeskanalytics.ui.screens.tickets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import io.github.kaulith.helpdeskanalytics.domain.model.Attachment
import io.github.kaulith.helpdeskanalytics.domain.model.Comment
import io.github.kaulith.helpdeskanalytics.domain.model.Communication
import io.github.kaulith.helpdeskanalytics.domain.model.Priority
import io.github.kaulith.helpdeskanalytics.domain.model.Status
import io.github.kaulith.helpdeskanalytics.domain.model.Ticket
import io.github.kaulith.helpdeskanalytics.domain.model.agentResolutionSla
import io.github.kaulith.helpdeskanalytics.ui.components.EmptyBlock
import io.github.kaulith.helpdeskanalytics.ui.components.HtmlText
import io.github.kaulith.helpdeskanalytics.ui.components.InitialsAvatar
import io.github.kaulith.helpdeskanalytics.ui.components.SkeletonBox
import io.github.kaulith.helpdeskanalytics.ui.components.SkeletonCard
import io.github.kaulith.helpdeskanalytics.ui.components.htmlHasContent
import io.github.kaulith.helpdeskanalytics.ui.components.openWebLink
import io.github.kaulith.helpdeskanalytics.ui.theme.FrappeRadius
import io.github.kaulith.helpdeskanalytics.ui.theme.Spacing
import io.github.kaulith.helpdeskanalytics.util.formatResolutionTime
import io.github.kaulith.helpdeskanalytics.util.formatResponseTime
import io.github.kaulith.helpdeskanalytics.util.toRelativeTime
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(
    ticketId: String,
    onBack: () -> Unit,
    viewModel: TicketDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val cs = MaterialTheme.colorScheme

    LaunchedEffect(ticketId) { viewModel.loadTicket(ticketId) }
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    if (uiState.showSelectAgentPrompt) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSelectAgentPrompt,
            title = { Text("Select an agent first") },
            text = {
                Text(
                    "Changes are recorded as the selected agent. Pick an active agent in " +
                        "Settings → Active agent before replying or editing this ticket."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissSelectAgentPrompt) { Text("Got it") }
            },
            shape = FrappeRadius.xl2
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = ticketId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.surface,
                    titleContentColor = cs.onSurface,
                    navigationIconContentColor = cs.onSurface
                )
            )
        },
        containerColor = cs.surface,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            when {
                uiState.isLoading -> TicketDetailSkeleton()
                uiState.error != null -> EmptyBlock(
                    title = "Couldn't load ticket",
                    description = uiState.error ?: "",
                    icon = Icons.Outlined.ErrorOutline,
                    actionLabel = "Retry",
                    onAction = { viewModel.loadTicket(ticketId) }
                )
                uiState.ticket != null -> TicketDetailContent(
                    ticket = uiState.ticket!!,
                    canWrite = uiState.canWrite,
                    comments = uiState.comments,
                    isLoadingComments = uiState.isLoadingComments,
                    communications = uiState.communications,
                    isLoadingCommunications = uiState.isLoadingCommunications,
                    communicationsError = uiState.communicationsError,
                    isUpdating = uiState.isUpdating,
                    isAddingComment = uiState.isAddingComment,
                    isSendingReply = uiState.isSendingReply,
                    onStatusChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.updateStatus(ticketId, it)
                    },
                    onPriorityChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.updatePriority(ticketId, it)
                    },
                    onAddComment = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.addComment(ticketId, it)
                    },
                    onSendReply = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.sendReply(ticketId, it)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TicketDetailContent(
    ticket: Ticket,
    canWrite: Boolean,
    comments: List<Comment>,
    isLoadingComments: Boolean,
    communications: List<Communication>,
    isLoadingCommunications: Boolean,
    communicationsError: String?,
    isUpdating: Boolean,
    isAddingComment: Boolean,
    isSendingReply: Boolean,
    onStatusChange: (Status) -> Unit,
    onPriorityChange: (Priority) -> Unit,
    onAddComment: (String) -> Unit,
    onSendReply: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var activeTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.base, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.base)
        ) {
            // Header: subject + status/priority pickers
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = ticket.subject,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPicker(
                        current = ticket.status,
                        isUpdating = isUpdating,
                        onChange = onStatusChange
                    )
                    PriorityPicker(
                        current = ticket.priority,
                        isUpdating = isUpdating,
                        onChange = onPriorityChange
                    )
                    if (ticket.isOverdue()) {
                        TonalPill("Overdue", cs.errorContainer, cs.onErrorContainer)
                    }
                }
            }

            // Description (only when there's no email thread)
            if (communications.isEmpty() && !isLoadingCommunications) {
                ticket.description?.let { desc ->
                    if (htmlHasContent(desc)) {
                        SectionCard(title = "Description") {
                            HtmlText(
                                html = desc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurface
                            )
                        }
                    }
                }
            }

            SectionCard(title = "Details") {
                DetailRow("Ticket ID", ticket.id)
                ticket.ticketType?.let { DetailRow("Type", it) }
                ticket.sla?.let { DetailRow("SLA", it) }
                ticket.customerName?.let { DetailRow("Customer", it) }
                ticket.assignedTo?.let { DetailRow("Assigned to", it) }
                if (ticket.assignees.size > 1) {
                    DetailRow("All assignees", ticket.assignees.joinToString(", "))
                }
            }

            SectionCard(title = "Timeline") {
                DetailRow("Created", formatTimestamp(ticket.createdAt))
                DetailRow("Last modified", ticket.modifiedAt.toRelativeTime())
                ticket.firstRespondedAt?.let { DetailRow("First response", formatTimestamp(it)) }
                ticket.resolvedAt?.let { DetailRow("Resolved", formatTimestamp(it)) }
            }

            if (ticket.responseBy != null || ticket.resolutionBy != null ||
                ticket.firstResponseTimeMinutes != null || ticket.resolutionTimeHours != null
            ) {
                SectionCard(title = "SLA & performance") {
                    ticket.responseBy?.let { DetailRow("Response due", formatTimestamp(it)) }
                    ticket.resolutionBy?.let { DetailRow("Resolution due", formatTimestamp(it)) }
                    ticket.lastAgentResponseAt?.let {
                        DetailRow("Last agent reply", formatTimestamp(it))
                    }
                    if (ticket.resolutionBy != null) {
                        DetailRow("Resolution SLA", ticket.agentResolutionSla().label)
                    }
                    ticket.firstResponseTimeMinutes?.let {
                        DetailRow("First response time", it.formatResponseTime())
                    }
                    ticket.avgResponseTimeMinutes?.let {
                        DetailRow("Avg response time", it.formatResponseTime())
                    }
                    ticket.resolutionTimeHours?.let {
                        DetailRow("Resolution time", it.formatResolutionTime())
                    }
                }
            }

            // Conversation: email thread + internal comments
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PrimaryTabRow(
                    selectedTabIndex = activeTab,
                    containerColor = cs.surface,
                    contentColor = cs.primary
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Replies (${communications.size})") }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Comments (${comments.size})") }
                    )
                }
                when (activeTab) {
                    0 -> CommunicationsList(
                        communications = communications,
                        isLoading = isLoadingCommunications,
                        error = communicationsError
                    )
                    1 -> CommentsList(comments = comments, isLoading = isLoadingComments)
                }
            }
        }

        // Bottom composer (sticky), hidden in read-only mode (no agent write key)
        HorizontalDivider(color = cs.outlineVariant)
        if (canWrite) {
            TicketComposer(
                isSendingReply = isSendingReply,
                isAddingComment = isAddingComment,
                onSendReply = onSendReply,
                onAddComment = onAddComment
            )
        } else {
            Text(
                text = "Read-only: replies and comments need an agent with API access",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FrappeRadius.lg,
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(Spacing.base)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = cs.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(Spacing.sm))
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurface,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusPicker(current: Status, isUpdating: Boolean, onChange: (Status) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val cs = MaterialTheme.colorScheme
    val container = statusContainerColor(current)
    val onContainer = statusOnContainerColor(current)
    Box {
        AssistChip(
            onClick = { if (!isUpdating) expanded = true },
            label = { Text(current.displayName) },
            enabled = !isUpdating,
            shape = FrappeRadius.full,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = container,
                labelColor = onContainer
            ),
            border = null
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Status.entries.forEach { s ->
                DropdownMenuItem(
                    text = { Text(s.displayName) },
                    onClick = {
                        onChange(s)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriorityPicker(current: Priority, isUpdating: Boolean, onChange: (Priority) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val container = priorityContainerColor(current)
    val onContainer = priorityOnContainerColor(current)
    Box {
        AssistChip(
            onClick = { if (!isUpdating) expanded = true },
            label = { Text(current.displayName) },
            enabled = !isUpdating,
            shape = FrappeRadius.full,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = container,
                labelColor = onContainer
            ),
            border = null
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Priority.entries.reversed().forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.displayName) },
                    onClick = {
                        onChange(p)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CommentsList(comments: List<Comment>, isLoading: Boolean) {
    val cs = MaterialTheme.colorScheme
    when {
        isLoading && comments.isEmpty() -> Box(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }
        comments.isEmpty() -> Text(
            text = "No comments yet. Be the first to comment.",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(Spacing.md)
        )
        else -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            comments.forEach { CommentBubble(it) }
        }
    }
}

@Composable
private fun CommentBubble(comment: Comment) {
    val cs = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.Top
    ) {
        InitialsAvatar(name = comment.commentedBy, size = 32.dp)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = FrappeRadius.lg,
            colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = comment.commentedBy,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface
                    )
                    Text(
                        text = comment.createdAt.toRelativeTime(),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
                HtmlText(
                    html = comment.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurface
                )
                AttachmentStrip(comment.attachments)
            }
        }
    }
}

@Composable
private fun CommunicationsList(
    communications: List<Communication>,
    isLoading: Boolean,
    error: String?
) {
    val cs = MaterialTheme.colorScheme
    when {
        isLoading && communications.isEmpty() -> Box(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }
        error != null && communications.isEmpty() -> Text(
            text = "Couldn't load replies: $error",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.error,
            modifier = Modifier.padding(Spacing.md)
        )
        communications.isEmpty() -> Text(
            text = "No emails yet. Use Reply below to email the customer.",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(Spacing.md)
        )
        else -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            communications.forEach { CommunicationBubble(it) }
        }
    }
}

@Composable
private fun CommunicationBubble(communication: Communication) {
    val cs = MaterialTheme.colorScheme
    // Agent replies sit on a tinted surface to set them apart from inbound mail.
    val container = if (communication.sentByAgent) cs.primaryContainer else cs.surfaceContainerLow
    val onContainer = if (communication.sentByAgent) cs.onPrimaryContainer else cs.onSurface
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.Top
    ) {
        InitialsAvatar(name = communication.sender, size = 32.dp)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = FrappeRadius.lg,
            colors = CardDefaults.cardColors(containerColor = container, contentColor = onContainer)
        ) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Text(
                            text = communication.sender,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        TonalPill(
                            label = if (communication.sentByAgent) "Agent" else "Customer",
                            container = if (communication.sentByAgent) cs.primary else cs.surfaceContainerHighest,
                            onContainer = if (communication.sentByAgent) cs.onPrimary else cs.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = communication.createdAt.toRelativeTime(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (communication.sentByAgent) cs.onPrimaryContainer else cs.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
                HtmlText(
                    html = communication.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onContainer
                )
                AttachmentStrip(communication.attachments)
            }
        }
    }
}

@Composable
private fun AttachmentStrip(attachments: List<Attachment>) {
    if (attachments.isEmpty()) return
    val cs = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    var fullScreenImage by remember { mutableStateOf<Attachment?>(null) }

    fullScreenImage?.let { image ->
        Dialog(
            onDismissRequest = { fullScreenImage = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Close image",
                        onClick = { fullScreenImage = null }
                    ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = image.url,
                    contentDescription = image.fileName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.base),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

    Column(
        modifier = Modifier.padding(top = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        attachments.forEach { attachment ->
            val openLabel = if (attachment.isImage) "View image" else "Open attachment"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, cs.outlineVariant, RoundedCornerShape(12.dp))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = openLabel,
                        onClick = {
                            if (attachment.isImage) {
                                fullScreenImage = attachment
                            } else {
                                uriHandler.openWebLink(attachment.url)
                            }
                        }
                    )
                    .semantics(mergeDescendants = true) {
                        contentDescription = attachment.fileName
                    }
            ) {
                if (attachment.isImage) {
                    AsyncImage(
                        model = attachment.url,
                        contentDescription = attachment.fileName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 96.dp, max = 240.dp)
                            .background(cs.surfaceContainer),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.Center
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Icon(
                        imageVector = if (attachment.isImage) Icons.Outlined.Image
                        else Icons.Outlined.AttachFile,
                        contentDescription = null,
                        tint = cs.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = attachment.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (attachment.isImage) Icons.Outlined.Visibility
                        else Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = "Open attachment",
                        tint = cs.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TicketComposer(
    isSendingReply: Boolean,
    isAddingComment: Boolean,
    onSendReply: (String) -> Unit,
    onAddComment: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var isReply by remember { mutableStateOf(true) }
    var text by remember { mutableStateOf("") }
    val isSending = if (isReply) isSendingReply else isAddingComment

    val templates = remember {
        listOf(
            "Looking into this" to "Hi, thanks for reaching out. I'm looking into this and will get back to you shortly.",
            "Need info" to "Could you share a bit more detail: steps to reproduce, screenshots, or the exact error message?",
            "Resolved" to "This should be resolved now. Please confirm on your end and let me know if anything else is off.",
            "Escalating" to "I've escalated this to the right team. You'll hear back from them shortly.",
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cs.surface)
            .padding(horizontal = Spacing.base, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = isReply,
                onClick = { isReply = true },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text("Reply") }
            SegmentedButton(
                selected = !isReply,
                onClick = { isReply = false },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text("Comment") }
        }
        if (isReply) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                templates.forEach { (label, body) ->
                    AssistChip(
                        onClick = { text = body },
                        label = { Text(label) },
                        shape = FrappeRadius.full,
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = cs.surfaceContainerLow,
                        ),
                    )
                }
            }
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    if (isReply) "Reply to the customer..." else "Add an internal comment..."
                )
            },
            enabled = !isSending,
            maxLines = 4,
            shape = FrappeRadius.lg,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = cs.surfaceContainerLow,
                unfocusedContainerColor = cs.surfaceContainerLow
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isReply) "Emails the customer and marks the ticket replied."
                else "Internal note, the customer won't see this.",
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(Spacing.sm))
            FilledTonalButton(
                onClick = {
                    val body = text.trim()
                    if (body.isNotEmpty()) {
                        if (isReply) onSendReply(body) else onAddComment(body)
                        text = ""
                    }
                },
                enabled = !isSending && text.isNotBlank(),
                shape = FrappeRadius.full
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = cs.onSecondaryContainer
                    )
                    Spacer(Modifier.width(Spacing.sm))
                }
                Text(if (isReply) "Send" else "Post")
                Spacer(Modifier.width(Spacing.xs))
                Icon(
                    Icons.AutoMirrored.Outlined.Send,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun formatTimestamp(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "%d %s %d, %02d:%02d".format(
        local.dayOfMonth,
        local.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3),
        local.year,
        local.hour,
        local.minute
    )
}

@Composable
private fun TicketDetailSkeleton() {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.surface)
            .padding(Spacing.base),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // Header band: ticket ID + pills + subject.
        SkeletonBox(width = 80.dp, height = 14.dp)
        SkeletonBox(width = 260.dp, height = 26.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SkeletonBox(width = 64.dp, height = 22.dp)
            SkeletonBox(width = 64.dp, height = 22.dp)
        }
        Spacer(Modifier.height(Spacing.sm))
        // Tab strip.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            SkeletonBox(width = 88.dp, height = 18.dp)
            SkeletonBox(width = 88.dp, height = 18.dp)
        }
        Spacer(Modifier.height(Spacing.sm))
        // Two comment cards.
        repeat(2) { SkeletonCard() }
    }
}
