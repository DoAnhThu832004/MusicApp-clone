package com.example.app.view.ai

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.app.model.ai.PlayerActionDto
import com.example.app.viewmodel.AiCompanionViewModel
import com.example.app.viewmodel.PlayerManager
import kotlinx.coroutines.flow.collectLatest

/**
 * Trình lắng nghe và điều phối hành động phát nhạc từ AI Agent (Task 3B.6 & Layer 7 Fast-Path).
 * Lắng nghe qua SharedFlow (replay = 0) để chống phát lặp khi Recomposition xảy ra.
 */
@Composable
fun AgenticPlayerDispatcher(
    viewModel: AiCompanionViewModel,
    playerManager: PlayerManager = PlayerManager
) {
    LaunchedEffect(viewModel) {
        viewModel.playerActionEvents.collectLatest { actionDto: PlayerActionDto ->
            Log.d("AgenticPlayer", "Nhận PlayerAction từ AI: action=${actionDto.action}, autoExecute=${actionDto.autoExecute}, song=${actionDto.song?.name}")

            when (actionDto.action.uppercase()) {
                "PAUSE" -> {
                    Log.i("AgenticPlayer", "Tạm dừng phát nhạc theo Fast-Path AI")
                    playerManager.pause()
                }
                "RESUME" -> {
                    Log.i("AgenticPlayer", "Tiếp tục phát nhạc theo Fast-Path AI")
                    playerManager.resume()
                }
                "NEXT" -> {
                    Log.i("AgenticPlayer", "Chuyển bài hát tiếp theo theo Fast-Path AI")
                    val hasNext = playerManager.next()
                    Log.d("AgenticPlayer", "Kết quả lệnh next(): $hasNext")
                }
                "PLAY" -> {
                    val songCard = actionDto.song ?: return@collectLatest
                    val song = songCard.toSong()
                    if (actionDto.autoExecute) {
                        Log.i("AgenticPlayer", "Tự động phát bài hát theo lệnh trực tiếp: ${song.name}")
                        playerManager.play(song)
                    } else {
                        Log.d("AgenticPlayer", "Bài hát được gợi ý nhưng không bật autoExecute: ${song.name}")
                    }
                }
                "QUEUE" -> {
                    val songCard = actionDto.song ?: return@collectLatest
                    val song = songCard.toSong()
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
