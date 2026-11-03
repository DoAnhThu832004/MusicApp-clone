package com.example.app.viewmodel

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.app.model.response.Song
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.viewModelScope
import com.example.app.model.ApiErrorUtils
import com.example.app.model.ApiService
import com.example.app.model.FileUtils
import com.example.app.model.repository.SongRepository
import com.example.app.model.request.SongCreationRequest
import com.example.app.model.request.SongUpdateRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest

class SongViewModel(
    private val repository: SongRepository,
): ViewModel() {
    private val _songUiState = MutableStateFlow(SongState())
    val songState: StateFlow<SongState> = _songUiState.asStateFlow()

    private var searchJob: Job? = null
    private var currentSearchQuery: String = ""
    private var currentPage: Int = 1

    var downloadProgress = mutableStateMapOf<String, Float>()
    var isDownloading = mutableStateMapOf<String, Boolean>()

    private val _searchQuery = MutableStateFlow("")
    /** genreId đang được lọc cho màn hình ListAllSong (null = tất cả) */
    private val _pagingGenreId = MutableStateFlow<String?>(null)
    /** Public expose \u0111\u1ec3 UI observe genreId hi\u1ec7n t\u1ea1i khi \u1ea5n Xem t\u1ea5t c\u1ea3 */
    val pagingGenreId: StateFlow<String?> = _pagingGenreId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val songsPaging: Flow<PagingData<Song>> = _searchQuery
        .debounce(500L)
        .combine(_pagingGenreId) { query, genreId -> query to genreId }
        .flatMapLatest { (query, genreId) ->
            repository.getSongsPaging(query, genreId)
        }
        .cachedIn(viewModelScope)

    fun searchAdminSongsPaging(query: String) {
        _searchQuery.value = query
    }

    /** Gọi từ màn hình ListAllSong để lọc paging theo genre đang chọn */
    fun setPagingGenreFilter(genreId: String?) {
        _pagingGenreId.value = genreId
    }

    // ===============================
    // Mood / Genre Filter
    // ===============================
    /** Cache toàn bộ genres lấy từ API (tránh gọi lại nhiều lần) */
    private var _allGenres: List<com.example.app.model.response.Genre> = emptyList()

    /** Trạng thái tab tâm trạng đang được chọn (mặc định "Tất cả") */
    private val _selectedMoodTab = MutableStateFlow("Tất cả")
    val selectedMoodTab: StateFlow<String> = _selectedMoodTab.asStateFlow()

    /** Bảng ánh xạ: nhãn UI -> keyG của genre tương ứng trên server */
    private val moodToKeyG = mapOf(
        "Thư giãn" to "chill",
        "Tập trung" to "tập trung",
        "Workout"  to "động lực"
    )

    init {
        // Tải sẵn danh sách genres ngay khi ViewModel khởi tạo
        viewModelScope.launch {
            try {
                val response = repository.getGenres()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.code == 1000 && body.result != null) {
                        _allGenres = body.result
                        _songUiState.value = _songUiState.value.copy(genres = body.result)
                    }
                }
            } catch (_: Exception) { }
        }
    }

    /**
     * Gọi khi người dùng ấn một nút tâm trạng.
     * @param moodLabel Tên hiển thị trên UI, ví dụ "Thư giãn", "Tất cả"…
     */
    fun filterSongsByMood(moodLabel: String) {
        _selectedMoodTab.value = moodLabel

        if (moodLabel == "Tất cả") {
            _pagingGenreId.value = null   // Reset paging: hiển thị tất cả bài hát
            getSongs()
            return
        }

        val keyG = moodToKeyG[moodLabel] ?: return
        val genre = _allGenres.firstOrNull { it.keyG.equals(keyG, ignoreCase = true) } ?: run {
            // Genres chưa load xong, thử load lại rồi filter
            viewModelScope.launch {
                try {
                    val response = repository.getGenres()
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.code == 1000 && body.result != null) {
                            _allGenres = body.result
                            _songUiState.value = _songUiState.value.copy(genres = body.result)
                            val found = _allGenres.firstOrNull { it.keyG.equals(keyG, ignoreCase = true) }
                            found?.let {
                                _pagingGenreId.value = it.id  // Cập nhật paging
                                getSongsByGenreId(it.id)
                            }
                        }
                    }
                } catch (_: Exception) { }
            }
            return
        }
        _pagingGenreId.value = genre.id   // Cập nhật paging ngay lập tức
        getSongsByGenreId(genre.id)
    }

    /** Gọi API lấy danh sách bài hát theo genreId rồi cập nhật songState.songs */
    private fun getSongsByGenreId(genreId: String) {
        viewModelScope.launch {
            _songUiState.value = _songUiState.value.copy(isLoading = true, error = null)
            try {
                val response = repository.getSongs(genreId = genreId)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.code == 1000 && body.result != null) {
                        _songUiState.value = _songUiState.value.copy(
                            isLoading = false,
                            songs = body.result.result,
                            error = null
                        )
                    } else {
                        _songUiState.value = _songUiState.value.copy(
                            isLoading = false,
                            error = "Không tải được bài hát theo thể loại"
                        )
                    }
                } else {
                    val apiErr = ApiErrorUtils.parse(response.errorBody()?.string())
                    _songUiState.value = _songUiState.value.copy(
                        isLoading = false,
                        error = apiErr?.message ?: "Lỗi từ máy chủ"
                    )
                }
            } catch (e: Exception) {
                _songUiState.value = _songUiState.value.copy(
                    isLoading = false,
                    error = "Lỗi kết nối: ${e.message}"
                )
            }
        }
    }
    // ===============================
    fun getTopSongs() {
        viewModelScope.launch {
            _songUiState.value = _songUiState.value.copy(
                isLoading = true,
                error = null
            )
            try {
                val response = repository.getTopSongs()
                if(response.isSuccessful) {
                    val body = response.body()
                    if (body?.code == 1000 && body.result != null) {
                        _songUiState.value = _songUiState.value.copy(
                            isLoading = false,
                            topSongs = body.result,
                            error = null
                        )
                    } else {
                        _songUiState.value = _songUiState.value.copy(
                            isLoading = false,
                            error = "Failed to load songs"
                        )
                    }
                }
            } catch (e: Exception) {
                _songUiState.value = _songUiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }
    fun getSongs(page: Int = 1, size: Int = 10) {
        viewModelScope.launch {
            _songUiState.value = _songUiState.value.copy(isLoading = true, error = null)
            try {
                val response = repository.getSongs(page = page, size = size)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.code == 1000 && body.result != null) {
                        _songUiState.value = _songUiState.value.copy(
                            isLoading = false,
                            songs = body.result.result,
                            error = null
                        )
                    } else {
                        _songUiState.value = _songUiState.value.copy(
                            isLoading = false,
                            error = "Failed to load songs"
                        )
                    }
                } else {
                    val apiErr = ApiErrorUtils.parse(response.errorBody()?.string())
                    _songUiState.value = _songUiState.value.copy(
                        isLoading = false,
                        error = apiErr?.message ?: "Failed to load songs"
                    )
                }
            } catch (e: Exception) {
                _songUiState.value = _songUiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }
    fun getRecentlyPlayedSongs() {
        viewModelScope.launch {
            _songUiState.value = _songUiState.value.copy(isLoading = true, error = null)
            try {
                val response = repository.getRecentlyPlayedSongs()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.code == 1000 && body.result != null) {
                        _songUiState.value = _songUiState.value.copy(
                            isLoading = false,
                            recentlyPlayedSongs = body.result,
                            error = null
                        )
                    } else {
                        _songUiState.value = _songUiState.value.copy(
                            isLoading = false,
                            error = "Failed to load recently played songs"
                        )
                    }
                } else {
                    val apiErr = ApiErrorUtils.parse(response.errorBody()?.string())
                    _songUiState.value = _songUiState.value.copy(
                        isLoading = false,
                        error = apiErr?.message ?: "Failed to load recently played songs"
                    )
                }
            } catch (e: Exception) {
                _songUiState.value = _songUiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }
    fun getGenres() {
        viewModelScope.launch {
            try {
                val response = repository.getGenres()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.code == 1000 && body.result != null) {
                        _songUiState.value = _songUiState.value.copy(
                            genres = body.result
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    fun createSong(name: String, description: String, duration: Int, releasedDate: String, genreIds: List<String> = emptyList()) {
        viewModelScope.launch {
            _songUiState.value = _songUiState.value.copy(
                isLoading = true,
                error = null,
                isCreating = true
            )
            try {
                val request = SongCreationRequest(
                    name = name,
                    description = description,
                    duration = duration,
                    releasedDate = releasedDate,
                    genreIds = genreIds
                )
                val response = repository.createSong(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.code == 1000 && body.result != null) {
                        val currentSongs = _songUiState.value.songs?.toMutableList() ?: mutableListOf()
                        currentSongs.add(body.result)
                        _songUiState.value = _songUiState.value.copy(
                            isLoading = false,
                            isCreating = false,
                            songs = currentSongs,
                            isSuccessful = true,
                            error = "Song created successfully"
                        )
                    } else {
                        _songUiState.value = _songUiState.value.copy(
                            isLoading = false,
                            isCreating = false,
                            error = "Failed to create song"
                        )
                    }
                } else {
                    val apiErr = ApiErrorUtils.parse(response.errorBody()?.string())
                    _songUiState.value = _songUiState.value.copy(
                        isLoading = false,
                        isCreating = false,
                        error = apiErr?.message ?: "Failed to create song"
                    )
                }
            } catch (e: Exception) {
                _songUiState.value = _songUiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}",
                    isCreating = false
                )
            }
        }
    }
    fun deleteSong(id: String) {
        viewModelScope.launch {
            _songUiState.value = _songUiState.value.copy(
                isLoading = true,
                error = null
            )
            try {
                val response = repository.deleteSong(id)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.code == 1000) {
                        val currentSongs = _songUiState.value.songs ?: emptyList()
                        val updatedSongs = currentSongs.filter { it.id != id }
                        _songUiState.value = _songUiState.value.copy(
                            isLoading = false,
                            songs = updatedSongs,
                            error = null
                        )
                    } else {
                        _songUiState.value = _songUiState.value.copy(
                            isLoading = false,
                            error = "Failed to delete song"
                        )
                    }
                }
            } catch (e: Exception) {
                _songUiState.value = _songUiState.value.copy(
                    isLoading = false,
                    error = "Lỗi kết nối: ${e.message}"
                )
            }
        }
    }
    fun uploadFiles(songId: String, imageFile: File, audioFile: File) {
        viewModelScope.launch {
            _songUiState.value = _songUiState.value.copy(
                isLoading = true,
                error = null
            )
            try {
                val response = repository.uploadSongFiles(songId, imageFile, audioFile)
                if(response.isSuccessful) {
                    _songUiState.value = _songUiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                } else {
                    _songUiState.value = _songUiState.value.copy(
                        isLoading = false,
                        error = "Failed to upload files"
                    )
                }
            } catch (e : Exception) {
                _songUiState.value = _songUiState.value.copy(
                    isLoading = false,
                    error = "Lỗi kết nối: ${e.message}"
                )
            }
        }
    }
    fun updateSong(id: String, name: String, description: String, status: String, duration: Int, releasedDate: String, type: String, genreIds: List<String> = emptyList()) {
        viewModelScope.launch {
            _songUiState.value = _songUiState.value.copy(
                isLoading = true,
                error = null
            )
            try {
                val request = SongUpdateRequest(
                    name = name,
                    description = description,
                    status = status,
                    duration = duration,
                    releasedDate = releasedDate,
                    type = type,
                    genreIds = genreIds
                )
                val response = repository.updateSong(id, request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.code == 1000 && body.result != null) {
                        val updateSongFromApi = body.result
                        val currentList = _songUiState.value.songs ?: emptyList()
                        val updatedList = currentList.map {
                            if (it.id == id) {
                                updateSongFromApi
                            } else {
                                it
                            }
                        }
                        _songUiState.value = _songUiState.value.copy(
                            isLoading = false,
                            songs = updatedList,
                            error = null
                        )
                    } else {
                        _songUiState.value = _songUiState.value.copy(
                            isLoading = false,
                            error = "Failed to update song"
                        )
                    }
                }
            } catch (e : Exception) {
                _songUiState.value = _songUiState.value.copy(isLoading = false)
            }
        }
    }

    /**
     * Gọi API AI tự động sinh mô tả & gợi ý thể loại (Feature 1)
     */
    fun enrichSongMetadata(
        songId: String,
        overwrite: Boolean = true,
        onSuccess: ((Song) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val response = repository.enrichSongMetadata(songId, overwrite)
                if (response.isSuccessful && response.body()?.code == 1000 && response.body()?.result != null) {
                    val enrichedSong = response.body()!!.result
                    val currentList = _songUiState.value.songs ?: emptyList()
                    val updatedList = currentList.map {
                        if (it.id == songId) enrichedSong else it
                    }
                    _songUiState.value = _songUiState.value.copy(
                        songs = updatedList,
                        error = null
                    )
                    onSuccess?.invoke(enrichedSong)
                } else {
                    val err = response.errorBody()?.string() ?: "Lỗi phản hồi từ AI"
                    onError?.invoke(err)
                }
            } catch (e: Exception) {
                onError?.invoke(e.message ?: "Lỗi kết nối khi gọi AI")
            }
        }
    }

    fun toggleFavorite(song: Song, playerViewModel: PlayerViewModel) {
        // 1. Xác định trạng thái mới (đảo ngược trạng thái hiện tại)
        val newFavoriteState = !song.favorite

        // 2. Optimistic Update: Cập nhật UI ngay lập tức để user thấy phản hồi nhanh
        updateLocalSongFavoriteStatus(song.id, newFavoriteState)
        if (playerViewModel.currentSong.value?.id == song.id) {
            playerViewModel.currentSong.value = song.copy(favorite = newFavoriteState)
        }

        viewModelScope.launch {
            try {
                // 3. Gọi API tương ứng dựa trên trạng thái mới
                val response = if (newFavoriteState) {
                    repository.addSongToFavorite(song.id)
                } else {
                    repository.deleteSongFromFavorite(song.id)
                }

                // 4. Kiểm tra kết quả từ Server
                if (!response.isSuccessful) {
                    // Nếu thất bại (lỗi server, lỗi mạng), hoàn tác lại UI (Rollback)
                    updateLocalSongFavoriteStatus(song.id, !newFavoriteState)

                    val errorBody = response.errorBody()?.string()
                    val apiErr = ApiErrorUtils.parse(errorBody)
                    _songUiState.value = _songUiState.value.copy(
                        error = "Lỗi thao tác: ${apiErr?.message ?: response.code()}"
                    )
                }
                // Nếu thành công thì không cần làm gì thêm vì UI đã update ở bước 2

            } catch (e: Exception) {
                // Nếu có Exception (mất mạng...), hoàn tác lại UI
                updateLocalSongFavoriteStatus(song.id, !newFavoriteState)
                _songUiState.value = _songUiState.value.copy(
                    error = "Lỗi kết nối: ${e.message}"
                )
            }
        }
    }
    private fun updateLocalSongFavoriteStatus(songId: String, isFavorite: Boolean) {
        val currentList = _songUiState.value.songs ?: return

        // Tạo list mới với item đã được cập nhật (State trong Compose là bất biến)
        val updatedList = currentList.map { song ->
            if (song.id == songId) {
                song.copy(favorite = isFavorite)
            } else {
                song
            }
        }

        _songUiState.value = _songUiState.value.copy(
            songs = updatedList
        )
    }
    fun refreshSongs() {
        getSongs()
    }
    fun searchAdminSongs(query: String, isLoadMore: Boolean = false) {
        // Xử lý logic khi bắt đầu tìm kiếm từ khóa mới
        if (!isLoadMore) {
            searchJob?.cancel()
            currentSearchQuery = query
            currentPage = 1
        } else {
            // Ngăn chặn gọi API nếu đang tải hoặc đã hết trang
            if (_songUiState.value.isLastPage || _songUiState.value.isLoadingMore) return
            currentPage++
        }

        // Xóa khôi phục danh sách mặc định vì ta muốn phân trang cho cả danh sách toàn bộ

        searchJob = viewModelScope.launch {
            // Chỉ áp dụng độ trễ (Debounce) cho lần gõ phím mới, không áp dụng khi cuộn trang
            if (!isLoadMore) delay(500)

            _songUiState.value = _songUiState.value.copy(
                isLoading = !isLoadMore,        // Quay loading lớn ở giữa màn hình
                isLoadingMore = isLoadMore,     // Quay loading nhỏ ở cuối màn hình
                error = null
            )

            try {
                // Phân trang với toàn bộ danh sách nếu query rỗng
                val response = if (query.isBlank()) {
                    repository.getSongs(page = currentPage, size = 20)
                } else {
                    repository.searchSongsForAdmin(query, page = currentPage, size = 20)
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.code == 1000 && body.result != null) {
                        val pageData = body.result
                        val newSongs = pageData.result

                        // Nếu là tải thêm (Load More), nối mảng mới vào mảng cũ
                        // Nếu là tìm kiếm mới, ghi đè mảng mới
                        val currentList = if (isLoadMore) _songUiState.value.songs.orEmpty() else emptyList()
                        val updatedList = currentList + newSongs

                        _songUiState.value = _songUiState.value.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            songs = updatedList,
                            // Thuật toán kiểm tra kết thúc: Nếu trang hiện tại >= tổng số trang từ API trả về
                            isLastPage = currentPage >= pageData.totalPages,
                            error = null
                        )
                    } else {
                        _songUiState.value = _songUiState.value.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = "Không tải được dữ liệu"
                        )
                    }
                } else {
                    val apiErr = ApiErrorUtils.parse(response.errorBody()?.string())
                    _songUiState.value = _songUiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = apiErr?.message ?: "Lỗi từ hệ thống máy chủ"
                    )
                }
            } catch (e: Exception) {
                _songUiState.value = _songUiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = "Lỗi đường truyền kết nối mạng"
                )
                // Lùi lại trang nếu tải lỗi để người dùng có thể thử cuộn lại
                if (isLoadMore) currentPage--
            }
        }
    }
    fun downloadSong(context: Context, song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            isDownloading[song.id] = true
            downloadProgress[song.id] = 0f

            try {
                val response = repository.downloadSong(song.id)
                if (response.isSuccessful && response.body() != null) {
                    val file = FileUtils.saveSongToStorage(
                        context,
                        response.body()!!,
                        song.name
                    ) { progress ->
                        downloadProgress[song.id] = progress
                    }

                    withContext(Dispatchers.Main) {
                        if (file != null) {
                            Toast.makeText(context, "Tải xong: ${song.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Lỗi khi lưu file", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isDownloading[song.id] = false
            }
        }
    }
    data class SongState(
        val songs: List<Song>? = null,
        val topSongs : List<Song>? = null,
        val recentlyPlayedSongs: List<Song>? = null,
        val genres: List<com.example.app.model.response.Genre>? = null,
        val isLoading: Boolean = false,
        val isLoadingMore: Boolean = false,
        val isLastPage: Boolean = false,
        val isCreating: Boolean = false,
        val isSuccessful: Boolean = false,
        val error: String? = null
    )
}