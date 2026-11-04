package com.example.app.model.ai

import com.example.app.model.ApiService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.lang.reflect.Proxy

class SseStreamClientTest {

    @Suppress("UNCHECKED_CAST")
    private fun createMockApiService(responseProvider: (AiChatStreamRequest) -> Response<ResponseBody>): ApiService {
        return Proxy.newProxyInstance(
            ApiService::class.java.classLoader,
            arrayOf(ApiService::class.java)
        ) { _, method, args ->
            when (method.name) {
                "streamAiChat" -> {
                    val request = args?.get(0) as AiChatStreamRequest
                    responseProvider(request)
                }
                "toString" -> "MockApiService"
                "hashCode" -> 1
                "equals" -> false
                else -> throw UnsupportedOperationException("Method ${method.name} not implemented in mock")
            }
        } as ApiService
    }

    @Test
    fun streamChat_success_emitsHeartbeatChunksAndComplete() = runBlocking {
        val sseData = """
            : heartbeat
            
            data: {"delta": "Chào bạn! ", "done": false}
            
            data: {"delta": "Giai điệu này rất chill.", "done": false}
            
            data: {"delta": "", "done": true, "status": "COMPLETE", "totalTokens": 85, "messageId": "msg-001"}
        """.trimIndent()

        val responseBody = sseData.toResponseBody("text/event-stream".toMediaType())
        val mockApiService = createMockApiService { Response.success(responseBody) }

        val client = SseStreamClient(mockApiService)
        val events = client.streamChat(AiChatStreamRequest("session-1", "Chào AI")).toList()

        assertEquals(4, events.size)
        assertTrue(events[0] is StreamEvent.Heartbeat)

        val chunk1 = events[1] as StreamEvent.Chunk
        assertEquals("Chào bạn! ", chunk1.delta)

        val chunk2 = events[2] as StreamEvent.Chunk
        assertEquals("Giai điệu này rất chill.", chunk2.delta)

        val complete = events[3] as StreamEvent.Complete
        assertEquals("msg-001", complete.messageId)
        assertEquals(85, complete.totalTokens)
    }

    @Test
    fun streamChat_sessionBusy_emits409ConflictError() = runBlocking {
        val errorJson = """{"code": 3002, "message": "Another AI stream is in progress"}"""
        val responseBody = errorJson.toResponseBody("application/json".toMediaType())
        val mockApiService = createMockApiService { Response.error(409, responseBody) }

        val client = SseStreamClient(mockApiService)
        val events = client.streamChat(AiChatStreamRequest("session-1", "Chào AI")).toList()

        assertEquals(1, events.size)
        val error = events[0] as StreamEvent.Error
        assertEquals("AI_SESSION_BUSY", error.errorCode)
        assertTrue(error.message.contains("đang có một phản hồi khác"))
    }

    @Test
    fun streamChat_rateLimitExceeded_emits429TooManyRequestsError() = runBlocking {
        val errorJson = """{"code": 3001, "message": "Rate limit exceeded"}"""
        val responseBody = errorJson.toResponseBody("application/json".toMediaType())
        val mockApiService = createMockApiService { Response.error(429, responseBody) }

        val client = SseStreamClient(mockApiService)
        val events = client.streamChat(AiChatStreamRequest("session-1", "Chào AI")).toList()

        assertEquals(1, events.size)
        val error = events[0] as StreamEvent.Error
        assertEquals("AI_RATE_LIMIT_EXCEEDED", error.errorCode)
        assertTrue(error.message.contains("quá nhanh"))
    }

    @Test
    fun streamChat_interruptedStream_emitsErrorEvent() = runBlocking {
        val sseData = """
            data: {"delta": "Đang phân tích bài hát...", "done": false}
            
            data: {"delta": "", "done": true, "status": "FAILED", "errorCode": "AI_STREAM_INTERRUPTED"}
        """.trimIndent()

        val responseBody = sseData.toResponseBody("text/event-stream".toMediaType())
        val mockApiService = createMockApiService { Response.success(responseBody) }

        val client = SseStreamClient(mockApiService)
        val events = client.streamChat(AiChatStreamRequest("session-1", "Phân tích bài này")).toList()

        assertEquals(2, events.size)
        val chunk = events[0] as StreamEvent.Chunk
        assertEquals("Đang phân tích bài hát...", chunk.delta)

        val error = events[1] as StreamEvent.Error
        assertEquals("AI_STREAM_INTERRUPTED", error.errorCode)
    }
}
