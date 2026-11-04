package com.example.app.model.repository

import com.example.app.model.ApiService
import com.example.app.model.ai.AiChatStreamRequest
import com.example.app.model.ai.AiPersona
import com.example.app.model.ai.ChatMessageResponse
import com.example.app.model.ai.ChatSessionCreationRequest
import com.example.app.model.ai.ChatSessionResponse
import com.example.app.model.ai.MessageSender
import com.example.app.model.ai.MessageStatus
import com.example.app.model.ai.StreamEvent
import com.example.app.model.response.ApiResponse
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.lang.reflect.Proxy

class AiChatRepositoryTest {

    @Suppress("UNCHECKED_CAST")
    private fun createMockApiService(): ApiService {
        return Proxy.newProxyInstance(
            ApiService::class.java.classLoader,
            arrayOf(ApiService::class.java)
        ) { _, method, args ->
            when (method.name) {
                "createChatSession" -> {
                    val request = args?.get(0) as ChatSessionCreationRequest
                    val session = ChatSessionResponse(
                        id = "session-test-1",
                        title = request.title ?: "Cuộc trò chuyện mới",
                        persona = request.persona
                    )
                    Response.success(ApiResponse(code = 1000, result = session))
                }
                "getChatSessions" -> {
                    val sessions = listOf(
                        ChatSessionResponse(id = "s-1", title = "Chat 1", persona = AiPersona.GENZ_DJ),
                        ChatSessionResponse(id = "s-2", title = "Chat 2", persona = AiPersona.HEALING_FRIEND)
                    )
                    Response.success(ApiResponse(code = 1000, result = sessions))
                }
                "getChatMessages" -> {
                    val sessionId = args?.get(0) as String
                    val messages = listOf(
                        ChatMessageResponse(
                            id = "m-1",
                            sessionId = sessionId,
                            sender = MessageSender.USER,
                            status = MessageStatus.COMPLETE,
                            content = "Gợi ý nhạc chill"
                        ),
                        ChatMessageResponse(
                            id = "m-2",
                            sessionId = sessionId,
                            sender = MessageSender.AI,
                            status = MessageStatus.COMPLETE,
                            content = "Có ngay một playlist cực êm cho bạn!"
                        )
                    )
                    Response.success(ApiResponse(code = 1000, result = messages))
                }
                "deleteChatSession" -> {
                    Response.success(ApiResponse(code = 1000, result = "Deleted"))
                }
                "streamAiChat" -> {
                    val body = "data: {\"delta\": \"Phản hồi mẫu\", \"done\": false}\n\ndata: {\"delta\": \"\", \"done\": true, \"status\": \"COMPLETE\"}\n\n"
                        .toResponseBody("text/event-stream".toMediaType())
                    Response.success(body)
                }
                "toString" -> "MockApiService"
                "hashCode" -> 1
                "equals" -> false
                else -> throw UnsupportedOperationException("Method ${method.name} not implemented in mock")
            }
        } as ApiService
    }

    @Test
    fun createChatSession_success() = runBlocking {
        val repository = AiChatRepository(createMockApiService())
        val response = repository.createChatSession(ChatSessionCreationRequest(persona = AiPersona.MUSIC_CRITIC))

        assertTrue(response.isSuccessful)
        val result = response.body()?.result
        assertNotNull(result)
        assertEquals("session-test-1", result?.id)
        assertEquals(AiPersona.MUSIC_CRITIC, result?.persona)
    }

    @Test
    fun getChatSessions_returnsList() = runBlocking {
        val repository = AiChatRepository(createMockApiService())
        val response = repository.getChatSessions()

        assertTrue(response.isSuccessful)
        val list = response.body()?.result
        assertNotNull(list)
        assertEquals(2, list?.size)
    }

    @Test
    fun getChatMessages_returnsMessages() = runBlocking {
        val repository = AiChatRepository(createMockApiService())
        val response = repository.getChatMessages("session-test-1")

        assertTrue(response.isSuccessful)
        val messages = response.body()?.result
        assertNotNull(messages)
        assertEquals(2, messages?.size)
        assertEquals(MessageSender.USER, messages?.get(0)?.sender)
        assertEquals(MessageSender.AI, messages?.get(1)?.sender)
    }

    @Test
    fun deleteChatSession_success() = runBlocking {
        val repository = AiChatRepository(createMockApiService())
        val response = repository.deleteChatSession("session-test-1")

        assertTrue(response.isSuccessful)
        assertEquals("Deleted", response.body()?.result)
    }

    @Test
    fun streamAiChat_delegatesToSseStreamClient() = runBlocking {
        val repository = AiChatRepository(createMockApiService())
        val events = repository.streamAiChat(AiChatStreamRequest("s-1", "test")).toList()

        assertEquals(2, events.size)
        assertTrue(events[0] is StreamEvent.Chunk)
        assertTrue(events[1] is StreamEvent.Complete)
    }
}
