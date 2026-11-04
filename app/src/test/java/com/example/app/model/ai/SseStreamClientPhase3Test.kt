package com.example.app.model.ai

import com.example.app.model.ApiService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.lang.reflect.Proxy

class SseStreamClientPhase3Test {

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
    fun streamChat_toolStatusAndSuggestedSongs_emitsCorrectEvents() = runBlocking {
        val ssePayload = """
            : heartbeat
            
            data: {"type": "tool_status", "toolName": "search_music_library", "message": "Đang tìm kiếm bài hát trong kho nhạc..."}
            
            data: {"delta": "Mình tìm thấy 2 bài hát rap rất hot cho bạn nè:", "done": false}
            
            data: {"delta": "", "done": true, "status": "COMPLETE", "totalTokens": 180, "messageId": "msg-phase3-1", "suggestedSongs": [{"id": "song-1", "name": "À Lôi", "artistNames": "Double2T", "imageUrl": "https://img/1.jpg", "audioUrl": "https://audio/1.mp3", "duration": 210, "playCount": 50000}, {"id": "song-2", "name": "Bật Nó Lên", "artistNames": "SOOBIN, HIEUTHUHAI", "imageUrl": "https://img/2.jpg", "audioUrl": "https://audio/2.mp3", "duration": 195, "playCount": 75000}], "playerAction": {"action": "PLAY", "autoExecute": true, "song": {"id": "song-1", "name": "À Lôi", "artistNames": "Double2T", "imageUrl": "https://img/1.jpg", "audioUrl": "https://audio/1.mp3", "duration": 210, "playCount": 50000}}, "playlistProposal": {"suggestedTitle": "Rap Việt Đỉnh Chóp", "songs": [{"id": "song-1", "name": "À Lôi", "artistNames": "Double2T", "imageUrl": "https://img/1.jpg", "audioUrl": "https://audio/1.mp3", "duration": 210, "playCount": 50000}]}}
        """.trimIndent()

        val responseBody = ssePayload.toResponseBody("text/event-stream".toMediaType())
        val mockApiService = createMockApiService { Response.success(responseBody) }

        val client = SseStreamClient(mockApiService)
        val events = client.streamChat(AiChatStreamRequest("session-test-3b", "Gợi ý nhạc rap hot")).toList()

        assertEquals(4, events.size)

        // 1. Heartbeat
        assertTrue(events[0] is StreamEvent.Heartbeat)

        // 2. ToolStatus
        assertTrue(events[1] is StreamEvent.ToolStatus)
        val toolStatus = events[1] as StreamEvent.ToolStatus
        assertEquals("search_music_library", toolStatus.toolName)
        assertEquals("Đang tìm kiếm bài hát trong kho nhạc...", toolStatus.message)

        // 3. Text Chunk
        assertTrue(events[2] is StreamEvent.Chunk)
        val chunk = events[2] as StreamEvent.Chunk
        assertEquals("Mình tìm thấy 2 bài hát rap rất hot cho bạn nè:", chunk.delta)

        // 4. Complete with Suggested Songs, PlayerAction, and PlaylistProposal
        assertTrue(events[3] is StreamEvent.Complete)
        val complete = events[3] as StreamEvent.Complete
        assertEquals("msg-phase3-1", complete.messageId)
        assertEquals(180, complete.totalTokens)

        // Verify Suggested Songs
        assertEquals(2, complete.suggestedSongs.size)
        assertEquals("À Lôi", complete.suggestedSongs[0].name)
        assertEquals("Double2T", complete.suggestedSongs[0].artistNames)
        val songObj = complete.suggestedSongs[0].toSong()
        assertEquals("song-1", songObj.id)
        assertEquals("À Lôi", songObj.name)
        assertEquals("Double2T", songObj.artistName)

        // Verify Player Action
        assertNotNull(complete.playerAction)
        assertEquals("PLAY", complete.playerAction?.action)
        assertTrue(complete.playerAction?.autoExecute == true)
        assertEquals("song-1", complete.playerAction?.song?.id)

        // Verify Playlist Proposal
        assertNotNull(complete.playlistProposal)
        assertEquals("Rap Việt Đỉnh Chóp", complete.playlistProposal?.suggestedTitle)
        assertEquals(1, complete.playlistProposal?.songs?.size)
    }

    @Test
    fun streamChat_completeWithoutTools_defaultsSafely() = runBlocking {
        val ssePayload = """
            data: {"delta": "Chào bạn, hôm nay bạn thế nào?", "done": false}
            
            data: {"delta": "", "done": true, "status": "COMPLETE", "totalTokens": 45}
        """.trimIndent()

        val responseBody = ssePayload.toResponseBody("text/event-stream".toMediaType())
        val mockApiService = createMockApiService { Response.success(responseBody) }

        val client = SseStreamClient(mockApiService)
        val events = client.streamChat(AiChatStreamRequest("session-test-3b", "Chào bạn")).toList()

        assertEquals(2, events.size)
        assertTrue(events[0] is StreamEvent.Chunk)
        assertTrue(events[1] is StreamEvent.Complete)

        val complete = events[1] as StreamEvent.Complete
        assertEquals(45, complete.totalTokens)
        assertTrue(complete.suggestedSongs.isEmpty())
        assertEquals(null, complete.playerAction)
        assertEquals(null, complete.playlistProposal)
    }
}
