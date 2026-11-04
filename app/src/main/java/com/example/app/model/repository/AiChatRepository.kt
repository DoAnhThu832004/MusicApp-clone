package com.example.app.model.repository

import com.example.app.model.ApiService
import com.example.app.model.ai.AiChatStreamRequest
import com.example.app.model.ai.ChatSessionCreationRequest
import com.example.app.model.ai.ChatMessageResponse
import com.example.app.model.ai.ChatSessionResponse
import com.example.app.model.ai.SseStreamClient
import com.example.app.model.ai.StreamEvent
import com.example.app.model.response.ApiResponse
import kotlinx.coroutines.flow.Flow
import retrofit2.Response

/**
 * Repository điều phối các thao tác AI Chat: quản lý phiên (CRUD session),
 * tải lịch sử tin nhắn và mở luồng SSE stream theo thời gian thực.
 */
open class AiChatRepository(
    private val apiService: ApiService,
    private val sseStreamClient: SseStreamClient = SseStreamClient(apiService)
) {

    /**
     * Mở luồng stream SSE nhận phản hồi thời gian thực từ AI Companion.
     */
    open fun streamAiChat(request: AiChatStreamRequest): Flow<StreamEvent> {
        return sseStreamClient.streamChat(request)
    }

    /**
     * Lấy danh sách toàn bộ các phiên chat của người dùng hiện tại.
     */
    open suspend fun getChatSessions(): Response<ApiResponse<List<ChatSessionResponse>>> {
        return apiService.getChatSessions()
    }

    /**
     * Tạo một phiên trò chuyện mới với Persona được chọn.
     */
    open suspend fun createChatSession(request: ChatSessionCreationRequest): Response<ApiResponse<ChatSessionResponse>> {
        return apiService.createChatSession(request)
    }

    /**
     * Lấy danh sách lịch sử tin nhắn trong một phiên trò chuyện cụ thể.
     */
    open suspend fun getChatMessages(sessionId: String): Response<ApiResponse<List<ChatMessageResponse>>> {
        return apiService.getChatMessages(sessionId)
    }

    /**
     * Xóa toàn bộ phiên trò chuyện và các tin nhắn liên quan.
     */
    open suspend fun deleteChatSession(sessionId: String): Response<ApiResponse<String>> {
        return apiService.deleteChatSession(sessionId)
    }
}
