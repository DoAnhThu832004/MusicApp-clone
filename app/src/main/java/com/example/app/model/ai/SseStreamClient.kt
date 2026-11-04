package com.example.app.model.ai

import android.util.Log
import com.example.app.model.ApiErrorUtils
import com.example.app.model.ApiService
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Client kết nối và xử lý luồng Server-Sent Events (SSE) qua giao thức POST + ResponseBody.byteStream().
 * Tuân thủ tiêu chuẩn AC-AI-08: Quản lý vòng đời chặt chẽ, đóng kết nối ngay lập tức khi coroutine bị hủy.
 */
class SseStreamClient(
    private val apiService: ApiService,
    private val gson: Gson = Gson()
) {

    /**
     * Mở luồng phát nhận dữ liệu chat SSE theo thời gian thực dưới dạng Kotlin Flow.
     *
     * @param request Thông tin phiên, câu hỏi và ID bài hát đang phát (nếu có)
     * @return Flow phát ra các sự kiện StreamEvent (Chunk, Heartbeat, Complete, Error)
     */
    fun streamChat(request: AiChatStreamRequest): Flow<StreamEvent> = flow {
        Log.d("AiCompanion", "Bắt đầu stream SSE: sessionId=${request.sessionId}, songId=${request.currentSongId}, prompt=${request.message.take(40)}")
        val response = try {
            apiService.streamAiChat(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AiCompanion", "Lỗi kết nối mạng khi gọi API stream: ${e.localizedMessage}", e)
            emit(
                StreamEvent.Error(
                    errorCode = "NETWORK_ERROR",
                    message = e.localizedMessage ?: "Không thể kết nối đến máy chủ AI"
                )
            )
            return@flow
        }

        // Xử lý khi HTTP Status không thành công (409 Conflict, 429 Too Many Requests, 401 Unauthorized...)
        if (!response.isSuccessful) {
            val rawError = try {
                response.errorBody()?.string()
            } catch (_: Exception) {
                null
            }

            val apiError = ApiErrorUtils.parse(rawError)
            val errorCodeStr = when (response.code()) {
                409 -> "AI_SESSION_BUSY"
                429 -> "AI_RATE_LIMIT_EXCEEDED"
                401 -> "UNAUTHORIZED"
                403 -> "FORBIDDEN"
                404 -> "SESSION_NOT_FOUND"
                else -> apiError?.code?.toString() ?: "HTTP_${response.code()}"
            }

            val errorMessage = when (errorCodeStr) {
                "AI_SESSION_BUSY" -> "Phiên trò chuyện đang có một phản hồi khác đang chạy. Vui lòng đợi trong giây lát."
                "AI_RATE_LIMIT_EXCEEDED" -> "Bạn đã gửi tin nhắn quá nhanh hoặc vượt giới hạn trong ngày. Vui lòng thử lại sau."
                "UNAUTHORIZED" -> "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
                else -> apiError?.message ?: "Lỗi máy chủ (${response.code()})"
            }

            Log.e("AiCompanion", "HTTP Error ${response.code()}: $errorCodeStr - $errorMessage - raw: $rawError")
            emit(StreamEvent.Error(errorCode = errorCodeStr, message = errorMessage))
            return@flow
        }

        val responseBody = response.body()
        if (responseBody == null) {
            Log.e("AiCompanion", "HTTP 200 nhưng response body rỗng")
            emit(StreamEvent.Error(errorCode = "EMPTY_BODY", message = "Phản hồi rỗng từ máy chủ"))
            return@flow
        }

        var inputStream: InputStream? = null
        var bufferedReader: BufferedReader? = null

        try {
            inputStream = responseBody.byteStream()
            bufferedReader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))

            var line: String? = null
            while (currentCoroutineContext().isActive && bufferedReader.readLine().also { line = it } != null) {
                val currentLine = line?.trim() ?: continue
                if (currentLine.isEmpty()) continue

                // 1. Dòng chú thích / Heartbeat (Bắt đầu bằng dấu ':')
                if (currentLine.startsWith(":")) {
                    emit(StreamEvent.Heartbeat)
                    continue
                }

                // 2. Dòng dữ liệu Server-Sent Events (Bắt đầu bằng 'data:')
                if (currentLine.startsWith("data:")) {
                    val jsonPayload = currentLine.removePrefix("data:").trim()
                    if (jsonPayload.isEmpty()) continue

                    val chunk = try {
                        gson.fromJson(jsonPayload, ChatStreamChunk::class.java)
                    } catch (_: Exception) {
                        null
                    }

                    if (chunk != null) {
                        if (chunk.type == "tool_status") {
                            Log.d("AiCompanion", "Stream ToolStatus: ${chunk.toolName} - ${chunk.message}")
                            emit(
                                StreamEvent.ToolStatus(
                                    toolName = chunk.toolName ?: "",
                                    message = chunk.message ?: "Đang xử lý dữ liệu âm nhạc..."
                                )
                            )
                        } else if (chunk.done) {
                            if (chunk.status == MessageStatus.FAILED || chunk.errorCode != null) {
                                Log.e("AiCompanion", "Stream Chunk FAILED: errorCode=${chunk.errorCode}")
                                emit(
                                    StreamEvent.Error(
                                        errorCode = chunk.errorCode ?: "STREAM_FAILED",
                                        message = chunk.errorCode ?: "Quá trình sinh câu trả lời bị gián đoạn"
                                    )
                                )
                            } else {
                                Log.d("AiCompanion", "Stream COMPLETE: messageId=${chunk.messageId}, tokens=${chunk.totalTokens}, songs=${chunk.suggestedSongs?.size ?: 0}")
                                emit(
                                    StreamEvent.Complete(
                                        messageId = chunk.messageId,
                                        totalTokens = chunk.totalTokens,
                                        suggestedSongs = chunk.suggestedSongs ?: emptyList(),
                                        playerAction = chunk.playerAction,
                                        playlistProposal = chunk.playlistProposal
                                    )
                                )
                            }
                            return@flow
                        } else if (chunk.delta.isNotEmpty()) {
                            Log.d("AiCompanion", "Stream Chunk delta: ${chunk.delta.take(20)}")
                            emit(StreamEvent.Chunk(chunk.delta))
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            Log.d("AiCompanion", "Stream bị hủy bởi người dùng hoặc CoroutineScope")
            throw e
        } catch (e: Exception) {
            Log.e("AiCompanion", "Lỗi đọc luồng SSE: ${e.localizedMessage}", e)
            emit(
                StreamEvent.Error(
                    errorCode = "STREAM_READ_ERROR",
                    message = e.localizedMessage ?: "Lỗi gián đoạn khi nhận dữ liệu trực tiếp"
                )
            )
        } finally {
            // Đóng sạch tài nguyên mạng và socket bên dưới ngay lập tức (AC-AI-08)
            withContext(NonCancellable) {
                try {
                    bufferedReader?.close()
                } catch (_: Exception) {}
                try {
                    inputStream?.close()
                } catch (_: Exception) {}
                try {
                    responseBody.close()
                } catch (_: Exception) {}
            }
        }
    }.flowOn(Dispatchers.IO)
}
