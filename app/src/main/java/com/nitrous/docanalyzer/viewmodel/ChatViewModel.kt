package com.nitrous.docanalyzer.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nitrous.docanalyzer.mapper.toDomain
import com.nitrous.docanalyzer.mapper.toModel
import com.nitrous.docanalyzer.model.*
import com.nitrous.docanalyzer.network.NetworkResult
import com.nitrous.docanalyzer.network.RetrofitClient
import com.nitrous.docanalyzer.network.dto.ChatRequest
import com.nitrous.docanalyzer.network.dto.MessageDto
import com.nitrous.docanalyzer.repository.ChatStreamEvent
import com.nitrous.docanalyzer.repository.DocRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.UUID

class ChatViewModel(
    private val repository: DocRepository = DocRepository(RetrofitClient.apiService, RetrofitClient.okHttpClient)
) : ViewModel() {
    private val TAG = "AUDIT_ChatVM"
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _sessionMessages = mutableMapOf<String, SnapshotStateList<ChatMessage>>()
    
    private var countdownJob: Job? = null
    private var streamingJob: Job? = null
    private var safetyTimeoutJob: Job? = null

    init {
        Log.d(TAG, "ChatViewModel Initializing")
        loadSessions()
        loadProfile()
    }

    fun updateUi(reducer: (ChatUiState) -> ChatUiState) {
        _uiState.update(reducer)
    }

    fun loadSessions() {
        viewModelScope.launch {
            when (val result = repository.getSessions()) {
                is NetworkResult.Success -> {
                    val sessions = withContext(Dispatchers.Default) {
                        result.data.map { it.toModel() }.sortedByDescending { it.createdAt }
                    }
                    
                    _uiState.update { state ->
                        state.copy(sessions = sessions).also {
                            if (sessions.isNotEmpty() && state.activeSessionId == null) {
                                selectSession(sessions.first().id)
                            } else if (sessions.isEmpty()) {
                                createNewSession()
                            }
                        }
                    }
                }
                is NetworkResult.ApiError -> handleNetworkError(result)
                is NetworkResult.NetworkError -> _uiState.update { it.copy(error = result.message) }
                else -> {}
            }
        }
    }

    private fun handleNetworkError(result: NetworkResult.ApiError) {
        when (result.code) {
            "RATE_LIMITED", "429" -> startRateLimitCountdown(result.message)
            else -> _uiState.update { it.copy(error = result.message) }
        }
    }

    private fun startRateLimitCountdown(message: String) {
        val seconds = message.filter { it.isDigit() }.toIntOrNull() ?: 60
        countdownJob?.cancel()
        _uiState.update { it.copy(isRateLimited = true, retryAfterSeconds = seconds) }
        
        countdownJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _uiState.update { it.copy(retryAfterSeconds = remaining) }
            }
            _uiState.update { it.copy(isRateLimited = false, retryAfterSeconds = 0) }
        }
    }

    fun createNewSession() {
        viewModelScope.launch {
            when (val result = repository.createSession("New Chat")) {
                is NetworkResult.Success -> {
                    val newSession = result.data.toModel()
                    _uiState.update { it.copy(
                        sessions = (listOf(newSession) + it.sessions).distinctBy { s -> s.id },
                        activeSessionId = newSession.id,
                        currentUploads = emptyList()
                    ) }
                }
                is NetworkResult.ApiError -> handleNetworkError(result)
                else -> {}
            }
        }
    }

    fun selectSession(sessionId: String) {
        // STEP 8: Reset State for New Chat
        Log.d(TAG, "STEP 8: Selecting session $sessionId. Resetting state.")
        stopChatStream()
        _uiState.update { it.copy(
            activeSessionId = sessionId, 
            currentUploads = emptyList(), 
            error = null, 
            isMenuVisible = false,
            isTyping = false,
            isSending = false
        ) }
        loadHistory(sessionId)
        loadSessionPdfs(sessionId)
    }

    private fun loadHistory(sessionId: String) {
        val sessionIntId = sessionId.toIntOrNull() ?: return
        viewModelScope.launch {
            when (val result = repository.getHistory(sessionIntId)) {
                is NetworkResult.Success -> {
                    val history = withContext(Dispatchers.Default) {
                        result.data.map { it.toModel() }
                    }
                    val sessionList = getMessagesForSession(sessionId).apply {
                        clear()
                        addAll(history)
                    }
                    
                    _uiState.update { state ->
                        state.copy(messages = state.messages + (sessionId to sessionList))
                    }
                }
                else -> {}
            }
        }
    }

    private fun getMessagesForSession(sessionId: String): SnapshotStateList<ChatMessage> {
        return _sessionMessages.getOrPut(sessionId) { mutableStateListOf() }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProfileLoading = true) }
            when (val result = repository.getMe()) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(currentUser = result.data.toDomain(), isProfileLoading = false) }
                }
                else -> {
                    _uiState.update { it.copy(isProfileLoading = false) }
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun logout(onSuccess: () -> Unit) {
        RetrofitClient.logout()
        onSuccess()

        GlobalScope.launch(Dispatchers.IO) {
            runCatching { repository.logout() }
        }
    }

    fun toggleMenu() {
        _uiState.update { it.copy(isMenuVisible = !it.isMenuVisible) }
    }

    fun sendMessage(text: String) {
        if (_uiState.value.isRateLimited || _uiState.value.isSending || text.isBlank()) return
        val sessionId = _uiState.value.activeSessionId ?: return
        val sessionIntId = sessionId.toIntOrNull() ?: return

        stopChatStream()
        
        streamingJob = viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, isTyping = true) }
            
            startSafetyTimeout()

            val sessionList = getMessagesForSession(sessionId)
            val history = sessionList.map { 
                MessageDto(role = it.role.name.lowercase(), text = it.content) 
            }
            
            val userMessage = ChatMessage(sessionId = sessionId, content = text, role = MessageRole.USER)
            withContext(Dispatchers.Main.immediate) {
                sessionList.add(userMessage)
            }

            val aiMessageId = UUID.randomUUID().toString()
            var aiContent = ""
            
            try {
                repository.streamChat(sessionIntId, ChatRequest(message = text, history = history)).collect { event ->
                    startSafetyTimeout()
                    
                    when (event) {
                        is ChatStreamEvent.Ready -> {
                            Log.d(TAG, "Stream Ready event received")
                        }
                        is ChatStreamEvent.Progress -> {
                            Log.d(TAG, "Stream Progress: ${event.stage}")
                        }
                        is ChatStreamEvent.Token -> {
                            // STEP 5: Confirm Token Handling
                            aiContent += event.text
                            Log.d(TAG, "STEP 5: Token appended: [${event.text}]")
                            updateLiveAiMessage(sessionId, aiMessageId, aiContent)
                        }
                        is ChatStreamEvent.Done -> {
                            // STEP 6: Verify Done Event
                            Log.d(TAG, "STEP 6: 'done' event received. Finalizing message.")
                            val finalAnswer = event.response.answer
                            if (!finalAnswer.isNullOrEmpty()) {
                                aiContent = finalAnswer
                                updateLiveAiMessage(sessionId, aiMessageId, aiContent)
                            }
                            
                            // CASE 4: Update session title if returned by backend
                            val newTitle = event.response.sessionTitle
                            if (newTitle != null) {
                                Log.d(TAG, "Updating session title to: $newTitle")
                                updateSessionTitle(sessionId, newTitle)
                            }

                            resetStreamingState()
                            loadSessions()
                        }
                        is ChatStreamEvent.Error -> {
                            // STEP 7: Handle Stream Failure
                            Log.e(TAG, "STEP 7: Stream failure event - Code: ${event.code}, Msg: ${event.message}")
                            resetStreamingState()
                            when (event.code) {
                                "PDF_NOT_READY" -> addSystemMessage(sessionId, "Document is still processing. Please wait.")
                                "RATE_LIMITED" -> startRateLimitCountdown(event.message)
                                else -> _uiState.update { it.copy(error = event.message) }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // STEP 7: Handle Stream Failure (Exceptions)
                Log.e(TAG, "STEP 7: Exception in stream collection: ${e.message}")
                resetStreamingState()
                _uiState.update { it.copy(error = e.message) }
            } finally {
                // STEP 7: Ensure state reset
                Log.d(TAG, "STEP 7: Stream collector finished (finally). Ensuring state reset.")
                resetStreamingState()
            }
        }
    }

    private fun updateSessionTitle(sessionId: String, newTitle: String) {
        _uiState.update { state ->
            state.copy(sessions = state.sessions.map { 
                if (it.id == sessionId) it.copy(title = newTitle) else it 
            })
        }
    }

    private fun startSafetyTimeout() {
        safetyTimeoutJob?.cancel()
        safetyTimeoutJob = viewModelScope.launch {
            delay(30000) // 30 seconds
            if (_uiState.value.isTyping) {
                Log.w(TAG, "AUDIT: Chat stream safety timeout reached. Typing indicator was stuck.")
                resetStreamingState()
                _uiState.update { it.copy(error = "Response timed out. Please try again.") }
                stopChatStream()
            }
        }
    }

    private fun resetStreamingState() {
        safetyTimeoutJob?.cancel()
        _uiState.update { it.copy(isTyping = false, isSending = false) }
    }

    fun stopChatStream() {
        streamingJob?.cancel()
        resetStreamingState()
    }

    private suspend fun updateLiveAiMessage(sessionId: String, messageId: String, content: String) {
        val sessionList = getMessagesForSession(sessionId)
        withContext(Dispatchers.Main.immediate) {
            val index = sessionList.indexOfFirst { it.id == messageId }
            if (index >= 0) {
                sessionList[index] = sessionList[index].copy(content = content)
            } else {
                sessionList.add(ChatMessage(id = messageId, sessionId = sessionId, content = content, role = MessageRole.ASSISTANT))
            }
        }
    }

    private fun addSystemMessage(sessionId: String, content: String) {
        getMessagesForSession(sessionId).add(ChatMessage(sessionId = sessionId, content = content, role = MessageRole.SYSTEM))
    }

    fun handleSelectedDocuments(uris: List<Uri>, contentResolver: android.content.ContentResolver, cacheDir: File) {
        val currentDocCount = _uiState.value.sessionDocuments.size
        if (currentDocCount + uris.size > 5) {
            updateUi { it.copy(showDocLimitError = true) }
            return
        }

        uris.forEach { uri ->
            val fileName = getFileName(uri, contentResolver)
            if (!isSupportedFileType(fileName)) {
                updateUi { it.copy(unsupportedFileError = "This file type is not supported. Please upload PDF, DOCX, CSV, MD, or TXT.") }
                return@forEach
            }

            val tempFile = File(cacheDir, "upload_${System.currentTimeMillis()}_$fileName")
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                uploadFile(fileName, tempFile.length(), tempFile)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy file: ${e.message}")
            }
        }
    }

    private fun getFileName(uri: Uri, contentResolver: android.content.ContentResolver): String {
        var name = "document.pdf"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun isSupportedFileType(fileName: String): Boolean {
        val supportedExtensions = listOf("pdf", "docx", "csv", "md", "txt")
        val extension = fileName.substringAfterLast(".", "").lowercase()
        return supportedExtensions.contains(extension)
    }

    fun uploadFile(name: String, size: Long, file: java.io.File) {
        viewModelScope.launch {
            var sessionId = _uiState.value.activeSessionId
            if (sessionId == null) {
                createNewSession()
                while (_uiState.value.activeSessionId == null) delay(50)
                sessionId = _uiState.value.activeSessionId
            }
            val sessionIntId = sessionId?.toIntOrNull() ?: return@launch

            val tempFileId = UUID.randomUUID().toString()
            val tempFile = UploadFile(id = tempFileId, name = name, size = size, status = UploadStatus.UPLOADING)
            
            _uiState.update { it.copy(currentUploads = it.currentUploads + tempFile, error = null) }

            when (val result = repository.uploadPdf(sessionIntId, file)) {
                is NetworkResult.Success -> {
                    result.data.jobId?.let { jobId ->
                        pollUploadJob(jobId, tempFileId)
                    }
                }
                is NetworkResult.ApiError -> {
                    updateUploadStatus(tempFileId, UploadStatus.FAILED)
                    handleNetworkError(result)
                }
                else -> {
                    updateUploadStatus(tempFileId, UploadStatus.FAILED)
                }
            }
        }
    }

    private fun pollUploadJob(jobId: String, fileId: String) {
        viewModelScope.launch {
            repository.pollJob(jobId).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val job = result.data
                        updateUploadProgress(fileId, (job.progress ?: 0) / 100f)
                        
                        val status = when (job.status) {
                            "completed" -> UploadStatus.INDEXED
                            "failed" -> UploadStatus.FAILED
                            "processing" -> UploadStatus.PROCESSING
                            else -> UploadStatus.QUEUED
                        }
                        updateUploadStatus(fileId, status)
                        
                        if (job.status == "completed") {
                            delay(800)
                            removeUpload(fileId)
                            loadSessions()
                            _uiState.value.activeSessionId?.let { loadSessionPdfs(it) }
                        } else if (job.status == "failed") {
                            _uiState.update { it.copy(error = "Indexing failed") }
                        }
                    }
                    is NetworkResult.ApiError -> {
                        updateUploadStatus(fileId, UploadStatus.FAILED)
                        handleNetworkError(result)
                    }
                    else -> {
                        updateUploadStatus(fileId, UploadStatus.FAILED)
                    }
                }
            }
        }
    }

    private fun updateUploadProgress(fileId: String, progress: Float) {
        _uiState.update { state ->
            state.copy(currentUploads = state.currentUploads.map { if (it.id == fileId) it.copy(progress = progress) else it })
        }
    }

    private fun updateUploadStatus(fileId: String, status: UploadStatus) {
        _uiState.update { state ->
            state.copy(currentUploads = state.currentUploads.map { if (it.id == fileId) it.copy(status = status) else it })
        }
    }

    fun removeUpload(fileId: String) {
        _uiState.update { state ->
            state.copy(currentUploads = state.currentUploads.filter { it.id != fileId })
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        val sessionIntId = sessionId.toIntOrNull() ?: return
        viewModelScope.launch {
            when (val result = repository.updateSession(sessionIntId, newTitle)) {
                is NetworkResult.Success -> loadSessions()
                is NetworkResult.ApiError -> _uiState.update { it.copy(error = result.message) }
                else -> {}
            }
        }
    }

    fun deleteSession(sessionId: String) {
        val sessionIntId = sessionId.toIntOrNull() ?: return
        viewModelScope.launch {
            when (val result = repository.deleteSession(sessionIntId)) {
                is NetworkResult.Success -> {
                    _uiState.update { state ->
                        val newSessions = state.sessions.filter { it.id != sessionId }
                        val newActiveId = if (state.activeSessionId == sessionId) newSessions.firstOrNull()?.id else state.activeSessionId
                        state.copy(sessions = newSessions, activeSessionId = newActiveId, messages = state.messages - sessionId)
                    }
                    _sessionMessages.remove(sessionId)
                    if (_uiState.value.sessions.isEmpty()) createNewSession()
                }
                is NetworkResult.ApiError -> _uiState.update { it.copy(error = result.message) }
                else -> {}
            }
        }
    }

    fun loadSessionPdfs(sessionId: String) {
        val sessionIntId = sessionId.toIntOrNull() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDocsLoading = true) }
            when (val result = repository.getSessionPdfs(sessionIntId)) {
                is NetworkResult.Success -> _uiState.update { it.copy(sessionDocuments = result.data, isDocsLoading = false) }
                else -> _uiState.update { it.copy(isDocsLoading = false) }
            }
        }
    }

    fun deletePdf(pdfId: Int) {
        viewModelScope.launch {
            when (val result = repository.deletePdf(pdfId)) {
                is NetworkResult.Success -> loadSessionPdfs(_uiState.value.activeSessionId ?: "")
                is NetworkResult.ApiError -> _uiState.update { it.copy(error = result.message) }
                else -> {}
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = null) }
            return
        }
        val results = _uiState.value.sessions.filter { it.title.contains(query, ignoreCase = true) }
        _uiState.update { it.copy(searchResults = results) }
    }

    override fun onCleared() {
        super.onCleared()
        stopChatStream()
        countdownJob?.cancel()
        safetyTimeoutJob?.cancel()
    }
}
