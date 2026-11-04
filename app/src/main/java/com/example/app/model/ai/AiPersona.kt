package com.example.app.model.ai

import com.google.gson.annotations.SerializedName

/**
 * 3 Nhân cách trò chuyện của AI Music Companion
 */
enum class AiPersona(
    val displayName: String,
    val roleDescription: String,
    val avatarEmoji: String
) {
    @SerializedName("GENZ_DJ")
    GENZ_DJ(
        displayName = "DJ GenZ",
        roleDescription = "Năng động, bắt trend, gọi bạn xưng tui, am hiểu nhạc trẻ & xu hướng",
        avatarEmoji = "🎧"
    ),

    @SerializedName("MUSIC_CRITIC")
    MUSIC_CRITIC(
        displayName = "Nhà Phê Bình",
        roleDescription = "Học thuật, phân tích chuyên sâu về giai điệu, hòa âm, phối khí & lịch sử",
        avatarEmoji = "🎻"
    ),

    @SerializedName("HEALING_FRIEND")
    HEALING_FRIEND(
        displayName = "Tâm Giao",
        roleDescription = "Lắng nghe, thấu hiểu, xoa dịu tâm hồn qua âm nhạc & an toàn tâm lý",
        avatarEmoji = "🌿"
    )
}
