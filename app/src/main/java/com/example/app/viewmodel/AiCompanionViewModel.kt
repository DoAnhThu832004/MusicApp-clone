package com.example.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.model.ai.AiChatStreamRequest
import com.example.app.model.ai.AiCompanionUiState
import com.example.app.model.ai.AiPersona
import com.example.app.model.ai.ChatSessionCreationRequest
import com.example.app.model.ai.MessageSender
import com.example.app.model.ai.MessageStatus
import com.example.app.model.ai.PlayerActionDto
import com.example.app.model.ai.PlaylistProposalDto
import com.example.app.model.ai.SongCardDto
import com.example.app.model.ai.StreamEvent
import com.example.app.model.ai.UiChatMessage
import com.example.app.model.repository.AiChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel điều khiển màn hình AI Companion với bộ đệm UI 40ms - 50ms (chống Recomposition Storm)
 * và cơ chế quản lý luồng stream an toàn (ngăn chặn rò rỉ socket và hỗ trợ nút Dừng).
 */
class AiCompanionViewModel(
    private val repository: AiChatRepository,
    private val playerManager: PlayerManager = PlayerManager,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    customScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope = customScope ?: viewModelScope

    private val _uiState = MutableStateFlow(AiCompanionUiState())
    val uiState: StateFlow<AiCompanionUiState> = _uiState.asStateFlow()

    private val _playerActionEvents = MutableSharedFlow<PlayerActionDto>(replay = 0, extraBufferCapacity = 1)
    val playerActionEvents: SharedFlow<PlayerActionDto> = _playerActionEvents.asSharedFlow()

    private var streamingJob: Job? = null
    private var songObservationJob: Job? = null

    // Ngưỡng thời gian bộ đệm UI (45ms ~ 22 FPS) chống giật màn hình
    private val bufferFlushIntervalMs: Long = 45L

    init {
        syncCurrentSong()
        loadSessions()
        observeCurrentSong()
    }

    private fun observeCurrentSong() {
        songObservationJob?.cancel()
        songObservationJob = CoroutineScope(SupervisorJob() + mainDispatcher).launch {
            playerManager.currentSongFlow.collect { song ->
                _uiState.update { it.copy(currentSong = song) }
            }
        }
    }

    /**
     * Đồng bộ hóa bài hát đang phát từ PlayerManager vào ngữ cảnh AI
     */
    fun syncCurrentSong() {
        val song = playerManager.currentSong
        _uiState.update { it.copy(currentSong = song) }
    }

    /**
     * Tải danh sách các phiên trò chuyện của người dùng
     */
    fun loadSessions() {
        scope.launch(mainDispatcher) {
            _uiState.update { it.copy(isLoadingHistory = true, errorMessage = null) }
            try {
                val response = repository.getChatSessions()
                if (response.isSuccessful && response.body()?.result != null) {
                    val sessions = response.body()!!.result
                    _uiState.update { it.copy(sessions = sessions) }

                    if (sessions.isNotEmpty()) {
                        val activeSession = sessions.first()
                        _uiState.update {
                            it.copy(
                                currentSession = activeSession,
                                currentPersona = activeSession.persona
                            )
                        }
                        loadMessages(activeSession.id)
                    } else {
                        _uiState.update {
                            it.copy(
                                currentSession = null,
                                messages = emptyList(),
                                isLoadingHistory = false
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingHistory = false,
                            errorMessage = "Không thể tải danh sách phiên trò chuyện"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingHistory = false,
                        errorMessage = e.localizedMessage ?: "Lỗi kết nối khi tải danh sách phiên"
                    )
                }
            }
        }
    }

    /**
     * Tải lịch sử tin nhắn của một phiên trò chuyện cụ thể
     */
    fun loadMessages(sessionId: String) {
        scope.launch(mainDispatcher) {
            _uiState.update { it.copy(isLoadingHistory = true, errorMessage = null) }
            try {
                val response = repository.getChatMessages(sessionId)
                if (response.isSuccessful && response.body()?.result != null) {
                    val remoteMessages = response.body()!!.result
                    val uiMessages = remoteMessages.map { remote ->
                        UiChatMessage(
                            id = remote.id,
                            sender = remote.sender,
                            content = remote.content,
                            status = remote.status,
                            isStreaming = false,
                            currentSongId = remote.currentSongId,
                            tokenCount = remote.tokenCount,
                            createdAt = remote.createdAt
                        )
                    }
                    _uiState.update {
                        it.copy(
                            messages = uiMessages,
                            isLoadingHistory = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingHistory = false,
                            errorMessage = "Không thể tải lịch sử tin nhắn"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingHistory = false,
                        errorMessage = e.localizedMessage ?: "Lỗi kết nối khi tải tin nhắn"
                    )
                }
            }
        }
    }

    /**
     * Chọn hoặc chuyển đổi Persona AI (DJ GenZ, Nhà Phê Bình, Tâm Giao)
     */
    fun selectPersona(persona: AiPersona) {
        if (_uiState.value.isStreaming) return
        if (_uiState.value.currentPersona == persona && _uiState.value.currentSession != null) return

        _uiState.update { it.copy(currentPersona = persona) }

        // Tìm phiên gần nhất khớp với Persona này hoặc tạo phiên mới
        val matchingSession = _uiState.value.sessions.firstOrNull { it.persona == persona }
        if (matchingSession != null) {
            _uiState.update { it.copy(currentSession = matchingSession) }
            loadMessages(matchingSession.id)
        } else {
            createNewSession(persona)
        }
    }

    /**
     * Tạo một phiên trò chuyện mới với Persona được chọn
     */
    fun createNewSession(persona: AiPersona = _uiState.value.currentPersona, onComplete: ((String?) -> Unit)? = null) {
        if (_uiState.value.isStreaming) return

        scope.launch(mainDispatcher) {
            _uiState.update { it.copy(isCreatingSession = true, errorMessage = null) }
            try {
                val response = repository.createChatSession(
                    ChatSessionCreationRequest(
                        title = "Cuộc trò chuyện mới",
                        persona = persona
                    )
                )

                if (response.isSuccessful && response.body()?.result != null) {
                    val newSession = response.body()!!.result
                    _uiState.update { current ->
                        current.copy(
                            currentSession = newSession,
                            sessions = listOf(newSession) + current.sessions,
                            messages = emptyList(),
                            currentPersona = newSession.persona,
                            isCreatingSession = false
                        )
                    }
                    onComplete?.invoke(newSession.id)
                } else {
                    _uiState.update {
                        it.copy(
                            isCreatingSession = false,
                            errorMessage = "Không thể khởi tạo phiên trò chuyện mới"
                        )
                    }
                    onComplete?.invoke(null)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCreatingSession = false,
                        errorMessage = e.localizedMessage ?: "Lỗi kết nối khi tạo phiên mới"
                    )
                }
                onComplete?.invoke(null)
            }
        }
    }

    /**
     * Gửi tin nhắn từ người dùng và mở luồng SSE stream nhận câu trả lời AI
     */
    fun sendMessage(prompt: String) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty() || _uiState.value.isStreaming) return

        syncCurrentSong()
        val songId = _uiState.value.currentSong?.id

        val activeSession = _uiState.value.currentSession
        if (activeSession == null) {
            // Nếu chưa có phiên, tự động tạo mới phiên rồi tiếp tục gửi
            createNewSession(_uiState.value.currentPersona) { newSessionId ->
                if (newSessionId != null) {
                    executeSendMessage(cleanPrompt, newSessionId, songId)
                }
            }
        } else {
            executeSendMessage(cleanPrompt, activeSession.id, songId)
        }
    }

    /**
     * Thử lại câu hỏi vừa bị lỗi (khi lỗi mạng xảy ra trước khi bắt đầu stream)
     */
    fun retryLastFailedPrompt() {
        val prompt = _uiState.value.lastFailedPrompt ?: return
        _uiState.update { state ->
            val updatedMessages = if (state.messages.isNotEmpty() && state.messages.last().sender == MessageSender.USER) {
                state.messages.dropLast(1)
            } else {
                state.messages
            }
            state.copy(canRetry = false, lastFailedPrompt = null, errorMessage = null, messages = updatedMessages)
        }
        sendMessage(prompt)
    }

    /**
     * Xóa thông báo lỗi hiện tại
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Dừng ngay lập tức luồng sinh văn bản của AI khi người dùng bấm nút Stop (AC-AI-08)
     */
    fun stopStreaming() {
        if (!_uiState.value.isStreaming) return
        streamingJob?.cancel()
        streamingJob = null

        _uiState.update { state ->
            val updatedMessages = state.messages.map { msg ->
                if (msg.isStreaming) {
                    msg.copy(
                        isStreaming = false,
                        status = MessageStatus.INTERRUPTED
                    )
                } else msg
            }
            state.copy(
                isStreaming = false,
                messages = updatedMessages
            )
        }
    }

    /**
     * Xóa phiên trò chuyện hiện tại và làm mới danh sách
     */
    fun deleteCurrentSession() {
        val session = _uiState.value.currentSession ?: return
        if (_uiState.value.isStreaming) stopStreaming()

        scope.launch(mainDispatcher) {
            try {
                val response = repository.deleteChatSession(session.id)
                if (response.isSuccessful) {
                    val remainingSessions = _uiState.value.sessions.filter { it.id != session.id }
                    if (remainingSessions.isNotEmpty()) {
                        val nextSession = remainingSessions.first()
                        _uiState.update {
                            it.copy(
                                currentSession = nextSession,
                                sessions = remainingSessions,
                                currentPersona = nextSession.persona
                            )
                        }
                        loadMessages(nextSession.id)
                    } else {
                        _uiState.update {
                            it.copy(
                                currentSession = null,
                                sessions = emptyList(),
                                messages = emptyList()
                            )
                        }
                    }
                } else {
                    _uiState.update { it.copy(errorMessage = "Không thể xóa phiên trò chuyện") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage ?: "Lỗi kết nối khi xóa phiên") }
            }
        }
    }

    /**
     * Lưu danh sách phát được AI đề xuất vào Thư viện cá nhân (Task 3B.7).
     * Human-in-the-Loop: Cho phép người dùng tùy biến tiêu đề và danh sách bài hát trước khi lưu.
     */
    fun saveProposedPlaylist(
        title: String,
        songIds: List<String>,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (title.isBlank() || songIds.isEmpty()) {
            onError("Tiêu đề hoặc danh sách bài hát không hợp lệ")
            return
        }

        scope.launch(mainDispatcher) {
            try {
                val createRes = repository.createPlaylist(title.trim())
                if (createRes.isSuccessful && createRes.body()?.result != null) {
                    val playlistId = createRes.body()!!.result.id
                    for (songId in songIds) {
                        try {
                            repository.addSongToPlaylist(playlistId, songId)
                        } catch (e: Exception) {
                            Log.w("AiCompanion", "Không thể thêm bài $songId vào playlist $playlistId: ${e.message}")
                        }
                    }
                    onSuccess()
                } else {
                    val errorMsg = createRes.errorBody()?.string() ?: "Không thể tạo danh sách phát"
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Lỗi kết nối khi lưu playlist")
            }
        }
    }

    /**
     * Thực thi gửi tin nhắn và quản lý bộ đệm 45ms UI buffer cho luồng SSE stream
     */
    private fun executeSendMessage(prompt: String, sessionId: String, songId: String?) {
        Log.d("AiCompanion", "executeSendMessage: prompt='$prompt', sessionId='$sessionId', songId='$songId'")
        val userMessage = UiChatMessage(
            sender = MessageSender.USER,
            content = prompt,
            status = MessageStatus.COMPLETE,
            isStreaming = false,
            currentSongId = songId
        )

        val aiPlaceholderMessage = UiChatMessage(
            sender = MessageSender.AI,
            content = "",
            status = MessageStatus.COMPLETE,
            isStreaming = true
        )

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage + aiPlaceholderMessage,
                isStreaming = true,
                errorMessage = null,
                canRetry = false,
                lastFailedPrompt = null
            )
        }

        streamingJob = scope.launch(mainDispatcher) {
            val textAccumulator = StringBuilder()
            var lastEmittedText = ""
            var isStreamActive = true
            var receivedAnyChunk = false

            // Bộ đệm UI chạy ngầm phát trạng thái mới mỗi 45ms (~22 FPS) chống giật màn hình
            val bufferTickerJob = launch(defaultDispatcher) {
                while (isStreamActive) {
                    delay(bufferFlushIntervalMs)
                    val snapshot = synchronized(textAccumulator) { textAccumulator.toString() }
                    if (snapshot != lastEmittedText) {
                        lastEmittedText = snapshot
                        withContext(mainDispatcher) {
                            updateStreamingContent(snapshot)
                        }
                    }
                }
            }

            var lastSuggestedSongs: List<SongCardDto> = emptyList()
            var lastPlayerAction: PlayerActionDto? = null
            var lastPlaylistProposal: PlaylistProposalDto? = null

            try {
                val streamRequest = AiChatStreamRequest(
                    sessionId = sessionId,
                    message = prompt,
                    currentSongId = songId
                )

                repository.streamAiChat(streamRequest).collect { event ->
                    when (event) {
                        is StreamEvent.ToolStatus -> {
                            _uiState.update { it.copy(toolStatusMessage = event.message) }
                        }

                        is StreamEvent.Chunk -> {
                            receivedAnyChunk = true
                            _uiState.update { if (it.toolStatusMessage != null) it.copy(toolStatusMessage = null) else it }
                            synchronized(textAccumulator) {
                                textAccumulator.append(event.delta)
                            }
                        }

                        is StreamEvent.Complete -> {
                            lastSuggestedSongs = event.suggestedSongs
                            lastPlayerAction = event.playerAction
                            lastPlaylistProposal = event.playlistProposal
                            if (event.playerAction != null) {
                                _playerActionEvents.tryEmit(event.playerAction)
                            }
                        }

                        is StreamEvent.Error -> {
                            // Xử lý lỗi từ stream
                            handleStreamError(event.message, receivedAnyChunk, prompt, sessionId)
                        }

                        StreamEvent.Heartbeat -> {
                            // Bỏ qua heartbeat
                        }
                    }
                }
            } catch (e: CancellationException) {
                // Người dùng bấm nút Stop hoặc thoát màn hình
                // Sẽ được dọn dẹp trong khối finally
            } catch (e: Exception) {
                handleStreamError(
                    e.localizedMessage ?: "Lỗi kết nối khi nhận phản hồi",
                    receivedAnyChunk,
                    prompt,
                    sessionId
                )
            } finally {
                isStreamActive = false
                bufferTickerJob.cancel()

                val finalText = synchronized(textAccumulator) { textAccumulator.toString() }
                withContext(mainDispatcher) {
                    finalizeStreamingMessage(
                        finalText = finalText,
                        suggestedSongs = lastSuggestedSongs,
                        playerAction = lastPlayerAction,
                        playlistProposal = lastPlaylistProposal
                    )
                }
            }
        }
    }

    /**
     * Cập nhật nội dung văn bản tăng dần trong tin nhắn AI đang stream
     */
    private fun updateStreamingContent(text: String) {
        _uiState.update { state ->
            val updated = state.messages.map { msg ->
                if (msg.isStreaming) msg.copy(content = text) else msg
            }
            state.copy(messages = updated)
        }
    }

    /**
     * Hoàn tất hoặc chốt hạ trạng thái tin nhắn khi kết thúc luồng stream
     */
    private fun finalizeStreamingMessage(
        finalText: String,
        suggestedSongs: List<SongCardDto> = emptyList(),
        playerAction: PlayerActionDto? = null,
        playlistProposal: PlaylistProposalDto? = null
    ) {
        _uiState.update { state ->
            val updated = state.messages.map { msg ->
                if (msg.isStreaming) {
                    msg.copy(
                        content = finalText,
                        isStreaming = false,
                        status = if (finalText.isNotEmpty()) MessageStatus.COMPLETE else MessageStatus.FAILED,
                        suggestedSongs = suggestedSongs,
                        playerAction = playerAction,
                        playlistProposal = playlistProposal
                    )
                } else msg
            }
            state.copy(
                isStreaming = false,
                toolStatusMessage = null,
                messages = updated
            )
        }
    }

    /**
     * Xử lý lỗi theo cơ chế Retry an toàn:
     * - Nếu đứt mạng trước khi nhận chunk đầu tiên: cho phép người dùng bấm "Thử lại".
     * - Nếu đứt mạng giữa chừng: tải lại lịch sử từ Server để đồng bộ DB.
     */
    private fun handleStreamError(
        errorMsg: String,
        receivedAnyChunk: Boolean,
        failedPrompt: String,
        sessionId: String
    ) {
        Log.e("AiCompanion", "handleStreamError: errorMsg='$errorMsg', receivedAnyChunk=$receivedAnyChunk, failedPrompt='$failedPrompt', sessionId='$sessionId'")
        if (!receivedAnyChunk) {
            // Lỗi xảy ra trước khi bắt đầu stream (429, 409, rớt mạng trước khi kết nối)
            _uiState.update { state ->
                // Xóa bong bóng AI rỗng placeholder
                val cleanedMessages = state.messages.filterNot { it.isStreaming }
                state.copy(
                    isStreaming = false,
                    toolStatusMessage = null,
                    errorMessage = errorMsg,
                    canRetry = true,
                    lastFailedPrompt = failedPrompt,
                    messages = cleanedMessages
                )
            }
        } else {
            // Đứt mạng giữa chừng khi đã sinh được một phần
            _uiState.update { state ->
                val updated = state.messages.map { msg ->
                    if (msg.isStreaming) {
                        msg.copy(isStreaming = false, status = MessageStatus.INTERRUPTED)
                    } else msg
                }
                state.copy(
                    isStreaming = false,
                    toolStatusMessage = null,
                    errorMessage = "$errorMsg (Câu trả lời bị gián đoạn)",
                    canRetry = false,
                    messages = updated
                )
            }
            // Tải lại lịch sử từ server để đảm bảo dữ liệu hiển thị trùng khớp với DB backend
            loadMessages(sessionId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        songObservationJob?.cancel()
        stopStreaming()
    }
}
