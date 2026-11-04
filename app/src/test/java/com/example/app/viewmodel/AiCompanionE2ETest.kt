package com.example.app.viewmodel

import com.example.app.model.ApiService
import com.example.app.model.ai.AiChatStreamRequest
import com.example.app.model.ai.AiPersona
import com.example.app.model.ai.ChatMessageResponse
import com.example.app.model.ai.ChatSessionCreationRequest
import com.example.app.model.ai.ChatSessionResponse
import com.example.app.model.ai.MessageSender
import com.example.app.model.ai.MessageStatus
import com.example.app.model.ai.StreamEvent
import com.example.app.model.repository.AiChatRepository
import com.example.app.model.response.ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.lang.reflect.Proxy

/**
 * Task 2B.7 & AC-AI-08: Kiểm thử End-to-End (E2E) toàn diện luồng hội thoại AI Companion trên Android.
 */
class AiCompanionE2ETest {

    @Suppress("UNCHECKED_CAST")
    private fun createMockApiService(
        sessions: MutableList<ChatSessionResponse> = mutableListOf(),
        messages: MutableList<ChatMessageResponse> = mutableListOf()
    ): ApiService {
        return Proxy.newProxyInstance(
            ApiService::class.java.classLoader,
            arrayOf(ApiService::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getChatSessions" -> {
                    Response.success(ApiResponse(code = 1000, result = sessions.toList()))
                }
                "createChatSession" -> {
                    val req = args?.get(0) as ChatSessionCreationRequest
                    val newSession = ChatSessionResponse(
                        id = "session-${sessions.size + 1}",
                        title = req.title ?: "Cuộc trò chuyện mới",
                        persona = req.persona
                    )
                    sessions.add(0, newSession)
                    Response.success(ApiResponse(code = 1000, result = newSession))
                }
                "getChatMessages" -> {
                    val sessionId = args?.get(0) as String
                    val filtered = messages.filter { it.sessionId == sessionId }
                    Response.success(ApiResponse(code = 1000, result = filtered))
                }
                "deleteChatSession" -> {
                    val sessionId = args?.get(0) as String
                    sessions.removeAll { it.id == sessionId }
                    messages.removeAll { it.sessionId == sessionId }
                    Response.success(ApiResponse(code = 1000, result = "Success"))
                }
                else -> null
            }
        } as ApiService
    }

    @Test
    fun e2e_fullChatFlow_groundedWithSongContext_completesStreaming() = runBlocking {
        val initialSessions = mutableListOf(
            ChatSessionResponse(id = "sess-100", title = "Hội thoại thử nghiệm", persona = AiPersona.GENZ_DJ)
        )
        val initialMessages = mutableListOf<ChatMessageResponse>()

        var streamRequestCaptured: AiChatStreamRequest? = null

        val testRepository = object : AiChatRepository(createMockApiService(initialSessions, initialMessages)) {
            override fun streamAiChat(request: AiChatStreamRequest) = flow {
                streamRequestCaptured = request
                emit(StreamEvent.Chunk("Giai điệu bài hát này "))
                delay(60)
                emit(StreamEvent.Chunk("cực kỳ cuốn và mang năng lượng tươi mới!"))
                delay(60)
                emit(StreamEvent.Complete(messageId = "msg-e2e-1", totalTokens = 35))
            }
        }

        val viewModel = AiCompanionViewModel(
            repository = testRepository,
            mainDispatcher = Dispatchers.Unconfined,
            defaultDispatcher = Dispatchers.Unconfined,
            customScope = this
        )

        // 1. Kiểm tra khởi tạo session
        assertEquals("sess-100", viewModel.uiState.value.currentSession?.id)
        assertEquals(AiPersona.GENZ_DJ, viewModel.uiState.value.currentPersona)

        // 2. Gửi câu hỏi
        viewModel.sendMessage("Bài này hay không DJ?")

        // Chờ ticker buffer 45ms xử lý
        delay(200)

        // 3. Xác thực request gửi lên server
        assertNotNull(streamRequestCaptured)
        assertEquals("sess-100", streamRequestCaptured?.sessionId)
        assertEquals("Bài này hay không DJ?", streamRequestCaptured?.message)

        // 4. Kiểm tra trạng thái UI sau khi stream hoàn tất
        val state = viewModel.uiState.value
        assertFalse(state.isStreaming)
        assertEquals(2, state.messages.size)

        val userMsg = state.messages[0]
        assertEquals(MessageSender.USER, userMsg.sender)
        assertEquals("Bài này hay không DJ?", userMsg.content)

        val aiMsg = state.messages[1]
        assertEquals(MessageSender.AI, aiMsg.sender)
        assertEquals("Giai điệu bài hát này cực kỳ cuốn và mang năng lượng tươi mới!", aiMsg.content)
        assertEquals(MessageStatus.COMPLETE, aiMsg.status)
        assertFalse(aiMsg.isStreaming)
    }

    @Test
    fun e2e_userStopStreaming_abortsImmediately_marksInterrupted() = runBlocking {
        val initialSessions = mutableListOf(
            ChatSessionResponse(id = "sess-stop", title = "Phiên dừng", persona = AiPersona.MUSIC_CRITIC)
        )
        val initialMessages = mutableListOf<ChatMessageResponse>()

        val testRepository = object : AiChatRepository(createMockApiService(initialSessions, initialMessages)) {
            override fun streamAiChat(request: AiChatStreamRequest) = flow {
                emit(StreamEvent.Chunk("Tác phẩm này bắt đầu bằng hòa âm thứ "))
                delay(10_000L)
                emit(StreamEvent.Chunk("và sau đó mở rộng sang gam trưởng..."))
            }
        }

        val viewModel = AiCompanionViewModel(
            repository = testRepository,
            mainDispatcher = Dispatchers.Unconfined,
            defaultDispatcher = Dispatchers.Unconfined,
            customScope = this
        )

        viewModel.sendMessage("Phân tích cấu trúc bài hát")

        delay(70)
        assertTrue(viewModel.uiState.value.isStreaming)

        // Người dùng bấm nút Stop
        viewModel.stopStreaming()
        delay(70)

        val state = viewModel.uiState.value
        assertFalse(state.isStreaming)
        val aiMsg = state.messages.last()
        assertEquals(MessageSender.AI, aiMsg.sender)
        assertEquals(MessageStatus.INTERRUPTED, aiMsg.status)
        assertFalse(aiMsg.isStreaming)
    }

    @Test
    fun e2e_switchPersona_updatesState_andCreatesSession() = runBlocking {
        val initialSessions = mutableListOf<ChatSessionResponse>()
        val initialMessages = mutableListOf<ChatMessageResponse>()

        val testRepository = object : AiChatRepository(createMockApiService(initialSessions, initialMessages)) {}

        val viewModel = AiCompanionViewModel(
            repository = testRepository,
            mainDispatcher = Dispatchers.Unconfined,
            defaultDispatcher = Dispatchers.Unconfined,
            customScope = this
        )

        // Chuyển sang HEALING_FRIEND
        viewModel.selectPersona(AiPersona.HEALING_FRIEND)
        assertEquals(AiPersona.HEALING_FRIEND, viewModel.uiState.value.currentPersona)
        assertNotNull(viewModel.uiState.value.currentSession)
        assertEquals(AiPersona.HEALING_FRIEND, viewModel.uiState.value.currentSession?.persona)

        // Chuyển sang MUSIC_CRITIC
        viewModel.selectPersona(AiPersona.MUSIC_CRITIC)
        assertEquals(AiPersona.MUSIC_CRITIC, viewModel.uiState.value.currentPersona)
    }

    @Test
    fun e2e_networkFailurePreStream_safeRetryRestoresState() = runBlocking {
        val initialSessions = mutableListOf(
            ChatSessionResponse(id = "sess-err", title = "Lỗi mạng", persona = AiPersona.GENZ_DJ)
        )
        val initialMessages = mutableListOf<ChatMessageResponse>()

        var callCount = 0

        val testRepository = object : AiChatRepository(createMockApiService(initialSessions, initialMessages)) {
            override fun streamAiChat(request: AiChatStreamRequest) = flow {
                callCount++
                if (callCount == 1) {
                    emit(StreamEvent.Error("Mất kết nối Internet", "NETWORK_ERROR"))
                } else {
                    emit(StreamEvent.Chunk("Thử lại thành công rồi nè!"))
                    delay(50)
                    emit(StreamEvent.Complete(messageId = "retry-msg", totalTokens = 10))
                }
            }
        }

        val viewModel = AiCompanionViewModel(
            repository = testRepository,
            mainDispatcher = Dispatchers.Unconfined,
            defaultDispatcher = Dispatchers.Unconfined,
            customScope = this
        )

        viewModel.sendMessage("Hỏi bài hát")

        delay(150)

        // Trạng thái khi lỗi trước stream
        val state = viewModel.uiState.value
        assertFalse(state.isStreaming)
        assertTrue(state.canRetry)
        assertEquals("Hỏi bài hát", state.lastFailedPrompt)
        assertNotNull(state.errorMessage)

        // Người dùng bấm Thử lại (retryLastFailedPrompt)
        viewModel.retryLastFailedPrompt()
        delay(150)

        val retryState = viewModel.uiState.value
        assertFalse(retryState.isStreaming)
        assertNull(retryState.errorMessage)
        assertEquals(2, retryState.messages.size)
        assertEquals("Thử lại thành công rồi nè!", retryState.messages[1].content)
        assertEquals(MessageStatus.COMPLETE, retryState.messages[1].status)
    }
}
