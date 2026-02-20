package com.nitrous.docanalyzer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nitrous.docanalyzer.mapper.toModel
import com.nitrous.docanalyzer.model.*
import com.nitrous.docanalyzer.network.NetworkResult
import com.nitrous.docanalyzer.network.RetrofitClient
import com.nitrous.docanalyzer.network.dto.ChatMessageDto
import com.nitrous.docanalyzer.repository.ChatStreamEvent
import com.nitrous.docanalyzer.repository.DocRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class ChatUiState(
    val sessions: List<ChatSession> = emptyList(),
    val activeSessionId: String? = null,
    val messages: Map<String, List<ChatMessage>> = emptyMap(),
    val currentUploads: List<UploadFile> = emptyList(),
    val isTyping: Boolean = false,
    val error: String? = null,
    val testSettings: TestSettings = TestSettings()
)

data class TestSettings(
    val networkError: Boolean = false,
    val aiFailure: Boolean = false,
    val uploadFailure: Boolean = false,
    val slowResponse: Boolean = false
)

class ChatViewModel(
    private val repository: DocRepository = DocRepository(RetrofitClient.apiService, RetrofitClient.okHttpClient)
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadSessions()
    }

    fun loadSessions() {
        viewModelScope.launch {
            when (val result = repository.getSessions()) {
                is NetworkResult.Success -> {
                    val sessions = withContext(Dispatchers.Default) {
                        result.data
                            .map { it.toModel() }
                            .sortedByDescending { it.createdAt }
                    }
                    
                    _uiState.update { it.copy(sessions = sessions) }
                    if (sessions.isNotEmpty() && _uiState.value.activeSessionId == null) {
                        selectSession(sessions.first().id)
                    } else if (sessions.isEmpty()) {
                        createNewSession()
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                is NetworkResult.RateLimit -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                is NetworkResult.Exception -> {
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    fun createNewSession() {
        viewModelScope.launch {
            val title = "New Chat"
            when (val result = repository.createSession(title)) {
                is NetworkResult.Success -> {
                    val newSession = result.data.toModel()
                    _uiState.update { it.copy(
                        sessions = (listOf(newSession) + it.sessions).distinctBy { s -> s.id },
                        activeSessionId = newSession.id,
                        messages = it.messages + (newSession.id to emptyList()),
                        currentUploads = emptyList()
                    ) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                is NetworkResult.RateLimit -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                is NetworkResult.Exception -> {
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    fun selectSession(sessionId: String) {
        _uiState.update { it.copy(activeSessionId = sessionId, currentUploads = emptyList(), error = null) }
        loadHistory(sessionId)
    }

    private fun loadHistory(sessionId: String) {
        viewModelScope.launch {
            val id = sessionId.toIntOrNull() ?: return@launch
            when (val result = repository.getHistory(id)) {
                is NetworkResult.Success -> {
                    val history = withContext(Dispatchers.Default) {
                        result.data.map { it.toModel() }
                    }
                    _uiState.update { state ->
                        state.copy(messages = state.messages + (sessionId to history))
                    }
                }
                else -> {}
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            val id = sessionId.toIntOrNull() ?: return@launch
            when (repository.deleteSession(id)) {
                is NetworkResult.Success -> {
                    _uiState.update { state ->
                        val newSessions = state.sessions.filter { it.id != sessionId }
                        val newActiveId = if (state.activeSessionId == sessionId) {
                            newSessions.firstOrNull()?.id
                        } else {
                            state.activeSessionId
                        }
                        state.copy(
                            sessions = newSessions,
                            activeSessionId = newActiveId,
                            messages = state.messages - sessionId
                        )
                    }
                    if (_uiState.value.sessions.isEmpty()) {
                        createNewSession()
                    }
                }
                else -> {}
            }
        }
    }

    fun sendMessage(text: String) {
        val sessionId = _uiState.value.activeSessionId ?: return
        val sessionIntId = sessionId.toIntOrNull() ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            // Check PDF readiness
            val pdfsResult = repository.getSessionPdfs(sessionIntId)
            if (pdfsResult is NetworkResult.Success) {
                val pdfs = pdfsResult.data
                val isReady = pdfs.isNotEmpty() && pdfs.all { it.status == "indexed" }
                
                if (!isReady) {
                    addSystemMessage(sessionId, "Please upload and index a document first.")
                    return@launch
                }
            }

            // Optimistic update
            val userMessage = ChatMessage(
                sessionId = sessionId,
                content = text,
                role = MessageRole.USER
            )
            _uiState.update { it.copy(
                messages = it.messages + (sessionId to (it.messages[sessionId] ?: emptyList()) + userMessage),
                isTyping = true,
                error = null
            ) }

            val history = withContext(Dispatchers.Default) {
                _uiState.value.messages[sessionId]?.map { 
                    ChatMessageDto(role = if (it.role == MessageRole.USER) "user" else "assistant", text = it.content)
                }
            }

            // Use streaming by default for smooth experience
            val aiMessageId = UUID.randomUUID().toString()
            var aiContent = ""
            var hasReceivedToken = false
            
            try {
                repository.streamChat(sessionIntId, text, history).collect { event ->
                    when (event) {
                        is ChatStreamEvent.Token -> {
                            hasReceivedToken = true
                            aiContent += event.text
                            updateLiveAiMessage(sessionId, aiMessageId, aiContent)
                        }
                        is ChatStreamEvent.Done -> {
                            _uiState.update { it.copy(isTyping = false) }
                            loadHistory(sessionId)
                        }
                        is ChatStreamEvent.Error -> {
                            _uiState.update { it.copy(isTyping = false, error = event.message) }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isTyping = false, error = e.message) }
            } finally {
                if (!hasReceivedToken) {
                    _uiState.update { it.copy(isTyping = false) }
                }
            }
        }
    }

    private fun updateLiveAiMessage(sessionId: String, messageId: String, content: String) {
        _uiState.update { state ->
            val sessionMessages = state.messages[sessionId] ?: emptyList()
            val existingIndex = sessionMessages.indexOfFirst { it.id == messageId }
            
            val updatedMessages = if (existingIndex >= 0) {
                sessionMessages.toMutableList().apply {
                    set(existingIndex, sessionMessages[existingIndex].copy(content = content))
                }
            } else {
                sessionMessages + ChatMessage(
                    id = messageId,
                    sessionId = sessionId,
                    content = content,
                    role = MessageRole.ASSISTANT
                )
            }
            
            state.copy(messages = state.messages + (sessionId to updatedMessages))
        }
    }

    private fun addSystemMessage(sessionId: String, content: String) {
        val systemMessage = ChatMessage(
            sessionId = sessionId,
            content = content,
            role = MessageRole.SYSTEM
        )
        _uiState.update { it.copy(
            messages = it.messages + (sessionId to (it.messages[sessionId] ?: emptyList()) + systemMessage)
        ) }
    }

    fun uploadFile(name: String, size: Long, file: java.io.File) {
        val sessionId = _uiState.value.activeSessionId ?: return
        val sessionIntId = sessionId.toIntOrNull() ?: return

        viewModelScope.launch {
            val tempFileId = UUID.randomUUID().toString()
            val tempFile = UploadFile(id = tempFileId, name = name, size = size, status = UploadStatus.UPLOADING)
            _uiState.update { it.copy(currentUploads = it.currentUploads + tempFile, error = null) }

            when (val result = repository.uploadPdf(sessionIntId, file, name)) {
                is NetworkResult.Success -> {
                    val uploadResponse = result.data
                    pollUploadJob(uploadResponse.jobId, tempFileId)
                }
                is NetworkResult.Error -> {
                    updateUploadStatus(tempFileId, UploadStatus.FAILED)
                    _uiState.update { it.copy(error = result.message) }
                }
                is NetworkResult.RateLimit -> {
                    updateUploadStatus(tempFileId, UploadStatus.FAILED)
                    _uiState.update { it.copy(error = result.message) }
                }
                is NetworkResult.Exception -> {
                    updateUploadStatus(tempFileId, UploadStatus.FAILED)
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    private fun pollUploadJob(jobId: String, fileId: String) {
        viewModelScope.launch {
            var attempts = 0
            while (attempts < 40) {
                delay(1500)
                attempts++
                when (val result = repository.getJob(jobId)) {
                    is NetworkResult.Success -> {
                        val job = result.data
                        updateUploadProgress(fileId, job.progress / 100f)
                        
                        val status = when (job.status) {
                            "completed" -> UploadStatus.INDEXED
                            "failed" -> UploadStatus.FAILED
                            "processing" -> {
                                when (job.stage) {
                                    "uploading" -> UploadStatus.UPLOADING
                                    "parsing", "chunking", "embedding" -> UploadStatus.PROCESSING
                                    else -> UploadStatus.PROCESSING
                                }
                            }
                            else -> UploadStatus.QUEUED
                        }
                        
                        updateUploadStatus(fileId, status)
                        
                        if (job.status == "completed" || job.status == "failed") {
                            if (job.status == "completed") {
                                delay(1000)
                                removeUpload(fileId)
                                loadHistory(_uiState.value.activeSessionId ?: "")
                            } else {
                                _uiState.update { it.copy(error = job.error ?: "Indexing failed") }
                            }
                            return@launch
                        }
                    }
                    is NetworkResult.Error -> {
                        // Don't stop on first error, maybe it's temporary. But log it or handle it.
                    }
                    is NetworkResult.RateLimit -> {
                        // Wait a bit longer
                        delay(2000)
                    }
                    is NetworkResult.Exception -> {
                        updateUploadStatus(fileId, UploadStatus.FAILED)
                        _uiState.update { it.copy(error = result.message) }
                        return@launch
                    }
                }
            }
            updateUploadStatus(fileId, UploadStatus.FAILED)
            _uiState.update { it.copy(error = "Job timed out") }
        }
    }

    private fun updateUploadProgress(fileId: String, progress: Float) {
        _uiState.update { state ->
            state.copy(currentUploads = state.currentUploads.map { 
                if (it.id == fileId) it.copy(progress = progress) else it 
            })
        }
    }

    private fun updateUploadStatus(fileId: String, status: UploadStatus) {
        _uiState.update { state ->
            state.copy(currentUploads = state.currentUploads.map { 
                if (it.id == fileId) it.copy(status = status) else it 
            })
        }
    }

    fun removeUpload(fileId: String) {
        _uiState.update { it.copy(currentUploads = it.currentUploads.filter { it.id != fileId }) }
    }

    fun toggleTestSetting(setting: String) {
        _uiState.update { state ->
            val settings = state.testSettings
            val newSettings = when (setting) {
                "network" -> settings.copy(networkError = !settings.networkError)
                "ai" -> settings.copy(aiFailure = !settings.aiFailure)
                "upload" -> settings.copy(uploadFailure = !settings.uploadFailure)
                "slow" -> settings.copy(slowResponse = !settings.slowResponse)
                else -> settings
            }
            state.copy(testSettings = newSettings)
        }
    }

    fun clearChat(sessionId: String) {
        viewModelScope.launch {
            val id = sessionId.toIntOrNull() ?: return@launch
            when (repository.clearHistory(id)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(
                        messages = it.messages + (sessionId to emptyList())
                    ) }
                }
                else -> {}
            }
        }
    }
}
