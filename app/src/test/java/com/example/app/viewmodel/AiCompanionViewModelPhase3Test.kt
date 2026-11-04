package com.example.app.viewmodel

import com.example.app.model.ApiService
import com.example.app.model.ai.AiChatStreamRequest
import com.example.app.model.ai.AiPersona
import com.example.app.model.ai.ChatMessageResponse
import com.example.app.model.ai.ChatSessionCreationRequest
import com.example.app.model.ai.ChatSessionResponse
import com.example.app.model.ai.PlayerActionDto
import com.example.app.model.ai.PlaylistProposalDto
import com.example.app.model.ai.SongCardDto
import com.example.app.model.ai.StreamEvent
import com.example.app.model.repository.AiChatRepository
import com.example.app.model.request.PlaylistCreateRequest
import com.example.app.model.response.ApiResponse
import com.example.app.model.response.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.lang.reflect.Proxy

class AiCompanionViewModelPhase3Test {

    @Suppress("UNCHECKED_CAST")
    private fun createMockApiService(
        sessions: MutableList<ChatSessionResponse> = mutableListOf(),
        createdPlaylists: MutableList<Playlist> = mutableListOf(),
        addedSongs: MutableList<Pair<String, String>> = mutableListOf()
    ): ApiService {
        return Proxy.newProxyInstance(
            ApiService::class.java.classLoader,
            arrayOf(ApiService::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getChatSessions" -> {
                    Response.success(ApiResponse(code = 1000, result = sessions.toList()))
                }
                "getChatMessages" -> {
                    Response.success(ApiResponse(code = 1000, result = emptyList<ChatMessageResponse>()))
                }
                "createChatSession" -> {
                    val req = args?.get(0) as ChatSessionCreationRequest
                    val newSession = ChatSessionResponse(
                        id = "session-${sessions.size + 1}",
                        title = req.title ?: "Cuộc trò chuyện mới",
                        persona = req.persona
                    )
                    sessions.add(0, newSession)
                    Response.success(ApiResponse(code = 1000, result = newSession))
                }
                "createPlaylist" -> {
                    val req = args?.get(0) as PlaylistCreateRequest
                    val playlist = Playlist(
                        id = "p-123",
                        title = req.title,
                        description = req.description,
                        imageUrlP = null
                    )
                    createdPlaylists.add(playlist)
                    Response.success(ApiResponse(code = 1000, result = playlist))
                }
                "addSongToPlaylist" -> {
                    val playlistId = args?.get(0) as String
                    val songId = args?.get(1) as String
                    addedSongs.add(playlistId to songId)
                    Response.success(ApiResponse(code = 1000, result = Playlist(id = playlistId, title = "Updated", description = "", imageUrlP = null)))
                }
                "toString" -> "MockApiService"
                "hashCode" -> 1
                "equals" -> false
                else -> throw UnsupportedOperationException("Method ${method.name} not implemented")
            }
        } as ApiService
    }

    private fun createRepositoryWithMockStream(
        apiService: ApiService,
        streamEvents: List<StreamEvent>
    ): AiChatRepository {
        return object : AiChatRepository(apiService) {
            override fun streamAiChat(request: AiChatStreamRequest) = flow {
                for (event in streamEvents) {
                    emit(event)
                }
            }
        }
    }

    @Test
    fun toolStatusEvent_updatesToolStatusMessage_andClearsOnChunk() = runBlocking {
        val initialSessions = mutableListOf(
            ChatSessionResponse(id = "s-1", title = "Chat 1", persona = AiPersona.GENZ_DJ)
        )
        val apiService = createMockApiService(initialSessions)
        val streamEvents = listOf(
            StreamEvent.ToolStatus(toolName = "search_songs", message = "🔍 Đang tìm kiếm trong kho nhạc..."),
            StreamEvent.Chunk("Đây là bài hát tìm được."),
            StreamEvent.Complete(messageId = "msg-1", totalTokens = 20)
        )
        val repository = createRepositoryWithMockStream(apiService, streamEvents)

        val viewModel = AiCompanionViewModel(
            repository = repository,
            mainDispatcher = Dispatchers.Unconfined,
            defaultDispatcher = Dispatchers.Unconfined,
            customScope = this
        )

        viewModel.sendMessage("Tìm bài hát vui nhộn")

        val state = viewModel.uiState.value
        assertNull(state.toolStatusMessage)
        assertFalse(state.isStreaming)
        assertEquals(2, state.messages.size)
        assertEquals("Đây là bài hát tìm được.", state.messages[1].content)
    }

    @Test
    fun completeEvent_withSuggestedSongsAndPlayerAction_populatesMessageAndTriggersEvent() = runBlocking {
        val initialSessions = mutableListOf(
            ChatSessionResponse(id = "s-1", title = "Chat 1", persona = AiPersona.GENZ_DJ)
        )
        val apiService = createMockApiService(initialSessions)

        val song1 = SongCardDto(
            id = "song-1",
            name = "Cắt Đôi Nỗi Sầu",
            artistNames = "Tăng Duy Tân",
            audioUrl = "https://example.com/song1.mp3",
            duration = 180L
        )
        val playerAction = PlayerActionDto(
            action = "PLAY",
            song = song1,
            autoExecute = true
        )
        val playlistProposal = PlaylistProposalDto(
            suggestedTitle = "Nhạc Chill Đêm Khuya",
            songs = listOf(song1)
        )

        val streamEvents = listOf(
            StreamEvent.Chunk("Đã tìm thấy bài hát phù hợp cho bạn!"),
            StreamEvent.Complete(
                messageId = "msg-2",
                totalTokens = 50,
                suggestedSongs = listOf(song1),
                playerAction = playerAction,
                playlistProposal = playlistProposal
            )
        )
        val repository = createRepositoryWithMockStream(apiService, streamEvents)

        val viewModel = AiCompanionViewModel(
            repository = repository,
            mainDispatcher = Dispatchers.Unconfined,
            defaultDispatcher = Dispatchers.Unconfined,
            customScope = this
        )

        viewModel.sendMessage("Phát bài Cắt Đôi Nỗi Sầu")

        val state = viewModel.uiState.value
        val aiMessage = state.messages[1]

        assertEquals(1, aiMessage.suggestedSongs.size)
        assertEquals("song-1", aiMessage.suggestedSongs[0].id)
        assertNotNull(aiMessage.playerAction)
        assertEquals("PLAY", aiMessage.playerAction?.action)
        assertTrue(aiMessage.playerAction?.autoExecute == true)
        assertEquals("song-1", aiMessage.playerAction?.song?.id)
        assertNotNull(aiMessage.playlistProposal)
        assertEquals("Nhạc Chill Đêm Khuya", aiMessage.playlistProposal?.suggestedTitle)
    }

    @Test
    fun saveProposedPlaylist_createsPlaylistAndAddsSongs() = runBlocking {
        val createdPlaylists = mutableListOf<Playlist>()
        val addedSongs = mutableListOf<Pair<String, String>>()
        val apiService = createMockApiService(
            createdPlaylists = createdPlaylists,
            addedSongs = addedSongs
        )
        val repository = AiChatRepository(apiService)

        val viewModel = AiCompanionViewModel(
            repository = repository,
            mainDispatcher = Dispatchers.Unconfined,
            defaultDispatcher = Dispatchers.Unconfined,
            customScope = this
        )

        var isSuccess = false
        viewModel.saveProposedPlaylist(
            title = "Top Rap Việt 2026",
            songIds = listOf("s1", "s2", "s3"),
            onSuccess = { isSuccess = true }
        )

        assertTrue(isSuccess)
        assertEquals(1, createdPlaylists.size)
        assertEquals("Top Rap Việt 2026", createdPlaylists[0].title)
        assertEquals(3, addedSongs.size)
        assertEquals("p-123" to "s1", addedSongs[0])
        assertEquals("p-123" to "s2", addedSongs[1])
        assertEquals("p-123" to "s3", addedSongs[2])
    }
}
