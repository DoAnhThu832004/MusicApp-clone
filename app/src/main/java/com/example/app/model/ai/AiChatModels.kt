package com.example.app.model.ai

import com.example.app.model.response.Song
import com.google.gson.annotations.SerializedName
import java.io.Serializable

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
 * DTO đại diện cho thẻ bài hát đề xuất từ AI (sử dụng cho Carousel, BottomSheet và Player Actions).
 */
data class SongCardDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("artistNames") val artistNames: String? = null,
    @SerializedName("genreNames") val genreNames: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("audioUrl") val audioUrl: String? = null,
    @SerializedName("duration") val duration: Long? = 0L,
    @SerializedName("playCount") val playCount: Long? = 0L
) : Serializable {
    fun toSong(): Song = Song(
        id = this.id,
        name = this.name,
        description = this.description ?: "",
        status = "PUBLIC",
        duration = this.duration?.toInt() ?: 0,
        releasedDate = "",
        type = "AUDIO",
        artistName = this.artistNames,
        imageUrl = this.imageUrl,
        audioUrl = this.audioUrl,
        favorite = false,
        playCount = this.playCount?.toInt() ?: 0,
        genres = null
    )
}

/**
 * DTO đại diện cho lệnh điều khiển trình phát từ AI (PLAY hoặc QUEUE).
 */
data class PlayerActionDto(
    @SerializedName("action") val action: String, // "PLAY" hoặc "QUEUE"
    @SerializedName("song") val song: SongCardDto? = null,
    @SerializedName("autoExecute") val autoExecute: Boolean = false
) : Serializable

/**
 * DTO đại diện cho đề xuất danh sách phát (Playlist) từ AI gửi xuống Android UI (Human-in-the-Loop).
 */
data class PlaylistProposalDto(
    @SerializedName("suggestedTitle") val suggestedTitle: String,
    @SerializedName("songs") val songs: List<SongCardDto> = emptyList()
) : Serializable

/**
 * Cấu trúc một gói dữ liệu SSE trả về từ backend:
 * data: {"delta": "...", "done": false}
 * data: {"type": "tool_status", "toolName": "...", "message": "..."}
 * data: {"delta": "", "done": true, "status": "COMPLETE", "totalTokens": 185, "suggestedSongs": [...], ...}
 */
data class ChatStreamChunk(
    val delta: String = "",
    val done: Boolean = false,
    val status: MessageStatus? = null,
    val messageId: String? = null,
    val totalTokens: Int? = null,
    val errorCode: String? = null,
    val type: String? = null,
    val toolName: String? = null,
    val message: String? = null,
    val suggestedSongs: List<SongCardDto>? = null,
    val playerAction: PlayerActionDto? = null,
    val playlistProposal: PlaylistProposalDto? = null
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
     * Bắn trạng thái công cụ AI đang chạy (Task 3B.1)
     */
    data class ToolStatus(val toolName: String, val message: String) : StreamEvent

    /**
     * Luồng stream hoàn tất thành công (kèm thẻ bài hát, lệnh phát, playlist)
     */
    data class Complete(
        val messageId: String?,
        val totalTokens: Int?,
        val suggestedSongs: List<SongCardDto> = emptyList(),
        val playerAction: PlayerActionDto? = null,
        val playlistProposal: PlaylistProposalDto? = null
    ) : StreamEvent

    /**
     * Xảy ra lỗi trong quá trình kết nối hoặc nhận stream
     */
    data class Error(val errorCode: String, val message: String) : StreamEvent

    /**
     * Gói tin heartbeat từ máy chủ để duy trì kết nối
     */
    data object Heartbeat : StreamEvent
}
