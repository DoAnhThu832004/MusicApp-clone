package com.example.app.model.ai

import com.google.gson.annotations.SerializedName

/**
 * Người gửi tin nhắn trong phiên trò chuyện
 */
enum class MessageSender {
    @SerializedName("USER")
    USER,

    @SerializedName("AI")
    AI
}

/**
 * Trạng thái xử lý của tin nhắn
 */
enum class MessageStatus {
    @SerializedName("COMPLETE")
    COMPLETE,

    @SerializedName("INTERRUPTED")
    INTERRUPTED,

    @SerializedName("FAILED")
    FAILED
}

/**
 * Yêu cầu tạo phiên trò chuyện mới
 */
data class ChatSessionCreationRequest(
    val title: String? = null,
    val persona: AiPersona = AiPersona.GENZ_DJ
)

/**
 * Dữ liệu trả về thông tin phiên trò chuyện
 */
data class ChatSessionResponse(
    val id: String,
    val title: String,
    val persona: AiPersona,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * Dữ liệu một tin nhắn trong lịch sử trò chuyện
 */
data class ChatMessageResponse(
    val id: String,
    val sessionId: String,
    val sender: MessageSender,
    val status: MessageStatus,
    val content: String,
    val currentSongId: String? = null,
    val tokenCount: Int? = 0,
    val createdAt: String? = null
)

/**
 * Yêu cầu gửi tin nhắn để mở luồng Server-Sent Events (SSE)
 */
data class AiChatStreamRequest(
    val sessionId: String,
    val message: String,
    val currentSongId: String? = null
)

/**
 * Cấu trúc một gói dữ liệu SSE trả về từ backend:
 * data: {"delta": "...", "done": false}
 * data: {"delta": "", "done": true, "status": "COMPLETE", "totalTokens": 185, "messageId": "..."}
 */
data class ChatStreamChunk(
    val delta: String = "",
    val done: Boolean = false,
    val status: MessageStatus? = null,
    val messageId: String? = null,
    val totalTokens: Int? = null,
    val errorCode: String? = null
)

/**
 * Sự kiện miền (Domain Event) được phát ra từ luồng stream cho ViewModel lắng nghe
 */
sealed interface StreamEvent {
    /**
     * Nhận được đoạn văn bản mới (chunk)
     */
    data class Chunk(val delta: String) : StreamEvent

    /**
     * Luồng stream hoàn tất thành công
     */
    data class Complete(val messageId: String?, val totalTokens: Int?) : StreamEvent

    /**
     * Xảy ra lỗi trong quá trình kết nối hoặc nhận stream
     */
    data class Error(val errorCode: String, val message: String) : StreamEvent

    /**
     * Gói tin heartbeat từ máy chủ để duy trì kết nối
     */
    data object Heartbeat : StreamEvent
}
