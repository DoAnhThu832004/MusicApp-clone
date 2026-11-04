package com.example.app.view.ai

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.app.model.ai.PlayerActionDto
import com.example.app.viewmodel.AiCompanionViewModel
import com.example.app.viewmodel.PlayerManager
import kotlinx.coroutines.flow.collectLatest

/**
 * Trình lắng nghe và điều phối hành động phát nhạc từ AI Agent (Task 3B.6).
 * Lắng nghe qua SharedFlow (replay = 0) để chống phát lặp khi Recomposition xảy ra.
 */
@Composable
fun AgenticPlayerDispatcher(
    viewModel: AiCompanionViewModel,
    playerManager: PlayerManager = PlayerManager
) {
    LaunchedEffect(viewModel) {
        viewModel.playerActionEvents.collectLatest { actionDto: PlayerActionDto ->
            val songCard = actionDto.song ?: return@collectLatest
            val song = songCard.toSong()

            Log.d("AgenticPlayer", "Nhận PlayerAction từ AI: action=${actionDto.action}, autoExecute=${actionDto.autoExecute}, song=${song.name}")

            when (actionDto.action.uppercase()) {
                "PLAY" -> {
                    if (actionDto.autoExecute) {
                        Log.i("AgenticPlayer", "Tự động phát bài hát theo lệnh trực tiếp: ${song.name}")
                        playerManager.play(song)
                    } else {
                        Log.d("AgenticPlayer", "Bài hát được gợi ý nhưng không bật autoExecute: ${song.name}")
                    }
                }
                "QUEUE" -> {
                    Log.i("AgenticPlayer", "Thêm bài hát vào hàng đợi: ${song.name}")
                    playerManager.enqueueSong(song)
                }
                else -> {
                    Log.w("AgenticPlayer", "Không rõ hành động phát nhạc: ${actionDto.action}")
                }
            }
        }
    }
}
