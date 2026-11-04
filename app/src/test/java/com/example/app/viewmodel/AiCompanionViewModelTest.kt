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

class AiCompanionViewModelTest {

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
                    Response.success(ApiResponse(code = 1000, result = "Deleted"))
                }
                "toString" -> "MockApiService"
                "hashCode" -> 1
                "equals" -> false
                else -> throw UnsupportedOperationException("Method ${method.name} not implemented")
            }
        } as ApiService
    }

    private fun createRepositoryWithMockStream(
        apiService: ApiService,
        streamEvents: List<StreamEvent>
    ): AiChatRepository {
        return object : AiChatRepository(apiService) {
            override fun streamAiChat(request: AiChatStreamRequest) = flow {
                for (event in streamEvents) {
                    emit(event)
                }
            }
        }
    }

    @Test
    fun initialState_loadsSessionsAndSelectsActiveSession() = runBlocking {
        val initialSessions = mutableListOf(
            ChatSessionResponse(id = "s-1", title = "Chat GenZ", persona = AiPersona.GENZ_DJ)
        )
        val initialMessages = mutableListOf(
            ChatMessageResponse(
                id = "m-1",
                sessionId = "s-1",
                sender = MessageSender.USER,
                status = MessageStatus.COMPLETE,
                content = "Chào bạn"
            ),
            ChatMessageResponse(
                id = "m-2",
                sessionId = "s-1",
                sender = MessageSender.AI,
                status = MessageStatus.COMPLETE,
                content = "Yo chào bạn nha!"
            )
        )

        val apiService = createMockApiService(initialSessions, initialMessages)
        val repository = AiChatRepository(apiService)

        val viewModel = AiCompanionViewModel(
            repository = repository,
            mainDispatcher = Dispatchers.Unconfined,
            defaultDispatcher = Dispatchers.Unconfined,
            customScope = this
        )

        val state = viewModel.uiState.value
        assertNotNull(state.currentSession)
        assertEquals("s-1", state.currentSession?.id)
        assertEquals(AiPersona.GENZ_DJ, state.currentPersona)
        assertEquals(2, state.messages.size)
        assertEquals("Chào bạn", state.messages[0].content)
        assertEquals("Yo chào bạn nha!", state.messages[1].content)
    }

    @Test
    fun selectPersona_switchesPersonaAndCreatesSessionWhenNoneExists() = runBlocking {
        val initialSessions = mutableListOf<ChatSessionResponse>()
        val apiService = createMockApiService(initialSessions)
        val repository = AiChatRepository(apiService)

        val viewModel = AiCompanionViewModel(
            repository = repository,
            mainDispatcher = Dispatchers.Unconfined,
            defaultDispatcher = Dispatchers.Unconfined,
            customScope = this
        )

        viewModel.selectPersona(AiPersona.MUSIC_CRITIC)

        val state = viewModel.uiState.value
        assertEquals(AiPersona.MUSIC_CRITIC, state.currentPersona)
        assertNotNull(state.currentSession)
        assertEquals(AiPersona.MUSIC_CRITIC, state.currentSession?.persona)
    }

    @Test
    fun sendMessage_streamsAiResponse_updatesMessages() = runBlocking {
        val initialSessions = mutableListOf(
            ChatSessionResponse(id = "s-1", title = "Chat 1", persona = AiPersona.GENZ_DJ)
        )
        val apiService = createMockApiService(initialSessions)
        val streamEvents = listOf(
            StreamEvent.Chunk("Giai điệu "),
            StreamEvent.Chunk("bài hát rất êm dịu."),
            StreamEvent.Complete(messageId = "ai-msg-1", totalTokens = 45)
        )
        val repository = createRepositoryWithMockStream(apiService, streamEvents)

        val viewModel = AiCompanionViewModel(
            repository = repository,
            mainDispatcher = Dispatchers.Unconfined,
            defaultDispatcher = Dispatchers.Unconfined,
            customScope = this
        )

        viewModel.sendMessage("Nhận xét bài này giúp tôi")

        val state = viewModel.uiState.value
        assertFalse(state.isStreaming)
        assertEquals(2, state.messages.size)

        val userMsg = state.messages[0]
        assertEquals(MessageSender.USER, userMsg.sender)
        assertEquals("Nhận xét bài này giúp tôi", userMsg.content)

        val aiMsg = state.messages[1]
        assertEquals(MessageSender.AI, aiMsg.sender)
        assertEquals("Giai điệu bài hát rất êm dịu.", aiMsg.content)
        assertEquals(MessageStatus.COMPLETE, aiMsg.status)
        assertFalse(aiMsg.isStreaming)
    }

    @Test
    fun stopStreaming_cancelsStream_marksMessageInterrupted() = runBlocking {
        val initialSessions = mutableListOf(
            ChatSessionResponse(id = "s-1", title = "Chat 1", persona = AiPersona.GENZ_DJ)
        )
        val apiService = createMockApiService(initialSessions)
        val repository = object : AiChatRepository(apiService) {
            override fun streamAiChat(request: AiChatStreamRequest) = flow {
                emit(StreamEvent.Chunk("Đang phân tích dở dang..."))
                delay(10_000L)
            }
        }

        val viewModel = AiCompanionViewModel(
            repository = repository,
            mainDispatcher = Dispatchers.Unconfined,
            defaultDispatcher = Dispatchers.Unconfined,
            customScope = this
        )

        viewModel.sendMessage("Kể câu chuyện bài hát")
        viewModel.stopStreaming()

        val state = viewModel.uiState.value
        assertFalse(state.isStreaming)
        assertEquals(2, state.messages.size)

        val aiMsg = state.messages[1]
        assertEquals(MessageStatus.INTERRUPTED, aiMsg.status)
        assertFalse(aiMsg.isStreaming)
    }

    @Test
    fun handleStreamError_preStreamError_enablesRetry() = runBlocking {
        val initialSessions = mutableListOf(
            ChatSessionResponse(id = "s-1", title = "Chat 1", persona = AiPersona.GENZ_DJ)
        )
        val apiService = createMockApiService(initialSessions)
        val streamEvents = listOf(
            StreamEvent.Error(errorCode = "AI_RATE_LIMIT_EXCEEDED", message = "Quá giới hạn tần suất")
        )
        val repository = createRepositoryWithMockStream(apiService, streamEvents)

        val viewModel = AiCompanionViewModel(
            repository = repository,
            mainDispatcher = Dispatchers.Unconfined,
            defaultDispatcher = Dispatchers.Unconfined,
            customScope = this
        )

        viewModel.sendMessage("Câu hỏi bị lỗi kết nối")

        val state = viewModel.uiState.value
        assertFalse(state.isStreaming)
        assertTrue(state.canRetry)
        assertEquals("Câu hỏi bị lỗi kết nối", state.lastFailedPrompt)
        assertEquals("Quá giới hạn tần suất", state.errorMessage)
        assertEquals(1, state.messages.size)
        assertEquals(MessageSender.USER, state.messages[0].sender)
    }

    @Test
    fun deleteCurrentSession_removesSessionAndClearsState() = runBlocking {
        val initialSessions = mutableListOf(
            ChatSessionResponse(id = "s-1", title = "Chat 1", persona = AiPersona.GENZ_DJ)
        )
        val apiService = createMockApiService(initialSessions)
        val repository = AiChatRepository(apiService)

        val viewModel = AiCompanionViewModel(
            repository = repository,
            mainDispatcher = Dispatchers.Unconfined,
            defaultDispatcher = Dispatchers.Unconfined,
            customScope = this
        )

        assertEquals("s-1", viewModel.uiState.value.currentSession?.id)

        viewModel.deleteCurrentSession()

        val state = viewModel.uiState.value
        assertNull(state.currentSession)
        assertTrue(state.sessions.isEmpty())
        assertTrue(state.messages.isEmpty())
    }
}
