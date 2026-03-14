package com.nitrous.docanalyzer.viewmodel

import com.nitrous.docanalyzer.model.*
import com.nitrous.docanalyzer.network.dto.PdfDto
import com.nitrous.docanalyzer.network.dto.SessionMetaDto

data class ChatUiState(
    val sessions: List<ChatSession> = emptyList(),
    val searchResults: List<ChatSession>? = null,
    val activeSessionId: String? = null,
    val messages: Map<String, List<ChatMessage>> = emptyMap(),
    val currentUploads: List<UploadFile> = emptyList(),
    val isTyping: Boolean = false,
    val error: String? = null,
    
    // Feature States
    val currentUser: User? = null,
    val sessionDocuments: List<PdfDto> = emptyList(),
    val sessionMeta: SessionMetaDto? = null,
    val isMenuVisible: Boolean = false,
    val isSearchLoading: Boolean = false,
    val isProfileLoading: Boolean = false,
    val isMetaLoading: Boolean = false,
    val isDocsLoading: Boolean = false,
    
    // UI Dialog/Sheet Visibility
    val showRenameDialog: Boolean = false,
    val showSessionInfo: Boolean = false,
    val showDocViewer: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val showDocLimitError: Boolean = false,
    val unsupportedFileError: String? = null,

    // Rate Limit and Sending States
    val isRateLimited: Boolean = false,
    val retryAfterSeconds: Int = 0,
    val isSending: Boolean = false,
    val streamingMessageId: String? = null
)
