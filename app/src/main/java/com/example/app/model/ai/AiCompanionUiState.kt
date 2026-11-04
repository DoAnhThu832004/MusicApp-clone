package com.example.app.model.ai

import com.example.app.model.response.Song
import java.util.UUID

/**
 * Model tin nhắn hiển thị trên giao diện người dùng
 */
data class UiChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val content: String,
    val status: MessageStatus = MessageStatus.COMPLETE,
    val isStreaming: Boolean = false,
    val currentSongId: String? = null,
    val tokenCount: Int? = 0,
    val createdAt: String? = null
)

/**
 * Trạng thái giao diện toàn diện cho màn hình AI Music Companion
 */
data class AiCompanionUiState(
    val currentSession: ChatSessionResponse? = null,
    val sessions: List<ChatSessionResponse> = emptyList(),
    val messages: List<UiChatMessage> = emptyList(),
    val currentPersona: AiPersona = AiPersona.GENZ_DJ,
    val isStreaming: Boolean = false,
    val isLoadingHistory: Boolean = false,
    val isCreatingSession: Boolean = false,
    val errorMessage: String? = null,
    val currentSong: Song? = null,
    val canRetry: Boolean = false,
    val lastFailedPrompt: String? = null
)
