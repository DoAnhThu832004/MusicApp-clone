package com.example.app.viewmodel

import android.content.Context
import android.media.AudioManager
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.app.model.response.Song
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object PlayerManager {
    private var player: ExoPlayer? = null
    private var context: Context? = null
    private val _currentSongFlow = kotlinx.coroutines.flow.MutableStateFlow<Song?>(null)
    val currentSongFlow: kotlinx.coroutines.flow.StateFlow<Song?> = _currentSongFlow

    var currentSong: Song? = null
        private set(value) {
            field = value
            _currentSongFlow.value = value
        }


    var currentUserId: String? = null // Thêm để theo dõi người dùng hiện tại
    var isAdShowing: Boolean = false // Cờ tạm dừng khi đang phát quảng cáo

    private var songList: List<Song> = emptyList()
    private var currentIndex: Int = -1
    private var shuffledList: List<Song> = emptyList()
    private var originalIndices: List<Int> = emptyList()

    var repeatMode: Int = 0
        private set

    var isShuffleMode: Boolean = false
        private set

    private var audioManager: AudioManager? = null

    var onSongChanged: ((Song, Boolean) -> Unit)? = null
    var onDurationChanged: ((Long) -> Unit)? = null
    var onIsPlayingChanged: ((Boolean) -> Unit)? = null

    private var saveJob: Job? = null

    fun init(context: Context) {
        this.context = context.applicationContext
        if(player == null) {
            val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
            player = ExoPlayer.Builder(context)
                .setAudioAttributes(audioAttributes, true)
                .build()
            setupPlayerListener()
        }
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    private fun setupPlayerListener() {
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        onDurationChanged?.invoke(getDuration())
                    }
                    Player.STATE_ENDED -> {
                        // Ghi nhận sự kiện telemetry khi nghe hết bài
                        currentSong?.let {
                            com.example.app.analytics.AnalyticsHelper.logPlaybackSongComplete(
                                songId = it.id.toString(),
                                durationListenedSec = getDuration() / 1000
                            )
                        }
                        // Bài hát đã kết thúc, xử lý tự động chuyển bài
                        handleSongEnded()
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                currentSong?.let {
                    com.example.app.analytics.AnalyticsHelper.logPlaybackError(
                        songId = it.id.toString(),
                        errorReason = error.message ?: "Unknown playback error"
                    )
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    startPeriodicSave()
                } else {
                    stopPeriodicSave()
                }
                onIsPlayingChanged?.invoke(isPlaying)
            }
        })
    }
    private fun handleSongEnded() {
        when (repeatMode) {
            2 -> {
                // Repeat one: phát lại bài hiện tại
                currentSong?.let { play(it) }
            }
            1 -> {
                // Repeat all: chuyển sang bài tiếp theo (hoặc quay lại đầu)
                next()
            }
            else -> {
                // Repeat off: chuyển sang bài tiếp theo nếu có
                next()
            }
        }
    }
    private fun resolveUri(uriString: String): String {
        return if (!uriString.startsWith("http://") && !uriString.startsWith("https://") && !uriString.startsWith("file://") && !uriString.startsWith("content://")) {
            try {
                android.net.Uri.fromFile(java.io.File(uriString)).toString()
            } catch (e: Exception) {
                uriString
            }
        } else {
            uriString
        }
    }

    fun play(song: Song, playlist: List<Song>? = null) {
        if (player == null && context != null) {
            init(context!!)
        }
        if (playlist != null && playlist.isNotEmpty()) {
            val index = playlist.indexOfFirst { it.id == song.id }
            if (index >= 0) {
                setPlaylist(playlist, index)
            }
        }
        currentSong = song
        
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // Nếu currentUserId trống, khôi phục từ SessionManager để lấy đúng bài hát đã tải
            var userId = currentUserId
            if (userId.isNullOrBlank() && context != null) {
                userId = SessionManager(context!!).getSavedUserId()
                if (!userId.isNullOrBlank()) {
                    currentUserId = userId
                }
            }

            // Sử dụng userId để tìm kiếm bài hát đã tải của đúng người dùng
            val localSong = if (context != null && !userId.isNullOrBlank()) {
                com.example.app.model.room.AppDatabase.getDatabase(context!!).songDao()
                    .getDownloadedSongById(song.id, userId)
            } else null

            val isOfflineSong = localSong != null && java.io.File(localSong.localAudioPath).exists()
            val uriString = if (isOfflineSong) {
                localSong!!.localAudioPath
            } else {
                song.audioUrl.toString()
            }

            val finalUri = resolveUri(uriString)

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                val mediaItem = MediaItem.fromUri(finalUri)
                player?.setMediaItem(mediaItem)
                player?.prepare()
                player?.play()
                onSongChanged?.invoke(song, true) // Trigger callback để update notification

                // Telemetry: playback_song_start
                com.example.app.analytics.AnalyticsHelper.logPlaybackSongStart(
                    songId = song.id,
                    songTitle = song.name,
                    artistId = song.artistName ?: "Unknown",
                    genreId = song.genres?.firstOrNull()?.id ?: "",
                    sourceContext = "player_screen",
                    isOffline = isOfflineSong
                )
            }
        }
    }
    fun pause() {
        // Telemetry: playback_pause
        currentSong?.let {
            com.example.app.analytics.AnalyticsHelper.logPlaybackPause(
                songId = it.id.toString(),
                positionSec = getCurrentPosition() / 1000
            )
        }
        player?.pause()
    }

    fun resume() {
        if (!isAdShowing) {
            player?.play()
        }
    }

    fun togglePlayPause() {
        if (isAdShowing) return
        player?.let {
            if (it.isPlaying) {
                pause()
            } else {
                it.play()
            }
        }
    }
    fun getCurrentPosition(): Long {
        val position = player?.currentPosition ?: 0L
        return if (position == C.TIME_UNSET || position == C.TIME_END_OF_SOURCE || position < 0) {
            0L
        } else {
            position
        }
    }
    fun getDuration(): Long {
        val duration = player?.duration ?: 0L
        return if (duration == C.TIME_UNSET || duration == C.TIME_END_OF_SOURCE || duration < 0) {
            0L
        } else {
            duration
        }
    }
    fun seekTo(position: Long) {
        val fromSec = getCurrentPosition() / 1000
        val toSec = position / 1000
        currentSong?.let {
            com.example.app.analytics.AnalyticsHelper.logPlaybackSeek(
                songId = it.id.toString(),
                fromSec = fromSec,
                toSec = toSec
            )
        }
        player?.seekTo(position)
    }
    fun getMaxVolume(): Int {
        return audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 100
    }

    fun getCurrentVolume(): Int {
        return audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
    }

    fun setVolume(volume: Int) {
        audioManager?.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            volume,
            AudioManager.FLAG_SHOW_UI
        )
    }
    fun next(): Boolean {
        if (songList.isEmpty()) return false

        val nextIndex = when {
            isShuffleMode -> {
                val currentShuffledIndex = shuffledList.indexOfFirst { it.id == currentSong?.id }
                if (currentShuffledIndex >= 0 && currentShuffledIndex < shuffledList.size - 1) {
                    currentShuffledIndex + 1
                } else if (repeatMode == 1) {
                    0 // Repeat all
                } else {
                    if (currentShuffledIndex >= 0 && currentShuffledIndex < shuffledList.size - 1) {
                        currentShuffledIndex + 1
                    } else {
                        -1
                    }
                }
            }
            else -> {
                if (currentIndex < songList.size - 1) {
                    currentIndex + 1
                } else if (repeatMode == 1) {
                    0 // Repeat all
                } else {
                    if (currentIndex < songList.size - 1) {
                        currentIndex + 1
                    } else {
                        -1
                    }
                }
            }
        }

        if (nextIndex < 0) return false

        val nextSong = if (isShuffleMode) {
            if (nextIndex < shuffledList.size) shuffledList[nextIndex] else null
        } else {
            if (nextIndex < songList.size) songList[nextIndex] else null
        }

        nextSong?.let {
            // Telemetry: playback_skip (next)
            currentSong?.let { curr ->
                com.example.app.analytics.AnalyticsHelper.logPlaybackSkip(
                    songId = curr.id.toString(),
                    skipType = "next",
                    positionSec = getCurrentPosition() / 1000
                )
            }
            currentIndex = if (isShuffleMode) {
                songList.indexOfFirst { it.id == it.id }
            } else {
                nextIndex
            }
            play(it)
            return true
        }
        return false
    }
    fun previous(): Boolean {
        if (songList.isEmpty()) return false

        val prevIndex = when {
            isShuffleMode -> {
                val currentShuffledIndex = shuffledList.indexOfFirst { it.id == currentSong?.id }
                if (currentShuffledIndex > 0) {
                    currentShuffledIndex - 1
                } else if (repeatMode == 1) {
                    shuffledList.size - 1 // Repeat all
                } else {
                    -1
                }
            }
            else -> {
                if (currentIndex > 0) {
                    currentIndex - 1
                } else if (repeatMode == 1) {
                    songList.size - 1 // Repeat all
                } else {
                    -1
                }
            }
        }

        if (prevIndex < 0) return false

        val prevSong = if (isShuffleMode) {
            if (prevIndex < shuffledList.size) shuffledList[prevIndex] else null
        } else {
            if (prevIndex < songList.size) songList[prevIndex] else null
        }

        prevSong?.let {
            // Telemetry: playback_skip (previous)
            currentSong?.let { curr ->
                com.example.app.analytics.AnalyticsHelper.logPlaybackSkip(
                    songId = curr.id.toString(),
                    skipType = "previous",
                    positionSec = getCurrentPosition() / 1000
                )
            }
            currentIndex = if (isShuffleMode) {
                songList.indexOfFirst { it.id == it.id }
            } else {
                prevIndex
            }
            play(it)
            return true
        }
        return false
    }
    fun toggleRepeat(): Int {
        repeatMode = (repeatMode + 1) % 3 // 0 -> 1 -> 2 -> 0
        val modeVal = when (repeatMode) {
            1 -> "all"
            2 -> "one"
            else -> "off"
        }
        com.example.app.analytics.AnalyticsHelper.logPlaybackModeToggle("repeat", modeVal)
        return repeatMode
    }
    fun toggleShuffle(): Boolean {
        isShuffleMode = !isShuffleMode
        if (isShuffleMode) {
            createShuffledList()
        }
        val modeVal = if (isShuffleMode) "on" else "off"
        com.example.app.analytics.AnalyticsHelper.logPlaybackModeToggle("shuffle", modeVal)
        return isShuffleMode
    }
    fun isPlaying() : Boolean = player?.isPlaying ?: false

    fun getPlayer() : ExoPlayer? = player

    fun release() {
        stopPeriodicSave()
        player?.release()
        player = null
        currentSong = null
        songList = emptyList()
        currentIndex = -1
        shuffledList = emptyList()
        originalIndices = emptyList()
        onSongChanged = null
        onDurationChanged = null
        onIsPlayingChanged = null
    }

    fun enqueueSong(song: Song) {
        if (currentSong == null) {
            play(song)
        } else {
            val list = songList.toMutableList()
            if (list.none { it.id == song.id }) {
                list.add(song)
                songList = list
            }
        }
    }

    fun isPlayingSong(songId: String): Boolean {
        return isPlaying() && currentSong?.id == songId
    }

    fun setPlaylist(songs: List<Song>, startIndex: Int = 0) {
        songList = songs
        currentIndex = startIndex.coerceIn(0,songs.size-1)
        if (isShuffleMode) {
            createShuffledList()
        }
    }

    private fun createShuffledList() {
        val indices = songList.indices.toMutableList()
        indices.shuffle()
        shuffledList = indices.map { songList[it] }
        originalIndices = indices
    }

    fun savePlaybackState() {
        val ctx = context ?: return
        val userId = currentUserId ?: return
        if (userId.isBlank()) return
        val song = currentSong ?: return
        val list = songList
        val index = currentIndex
        val pos = getCurrentPosition()
        val rep = repeatMode
        val shuf = isShuffleMode

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val prefs = ctx.getSharedPreferences("player_prefs_$userId", Context.MODE_PRIVATE)
                val gson = Gson()
                val songJson = gson.toJson(song)
                val playlistJson = gson.toJson(list)
                
                prefs.edit().apply {
                    putString("last_song", songJson)
                    putString("last_playlist", playlistJson)
                    putInt("last_index", index)
                    putLong("last_position", pos)
                    putInt("last_repeat", rep)
                    putBoolean("last_shuffle", shuf)
                    apply()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun restorePlaybackState() {
        val ctx = context ?: return
        GlobalScope.launch(Dispatchers.IO) {
            var userId = currentUserId
            if (userId.isNullOrBlank()) {
                userId = SessionManager(ctx).getSavedUserId()
                if (!userId.isNullOrBlank()) {
                    currentUserId = userId
                }
            }
            if (userId.isNullOrBlank()) return@launch

            try {
                val prefs = ctx.getSharedPreferences("player_prefs_$userId", Context.MODE_PRIVATE)
                val songJson = prefs.getString("last_song", null) ?: return@launch
                val playlistJson = prefs.getString("last_playlist", null)
                
                val gson = Gson()
                val song = gson.fromJson(songJson, Song::class.java)
                val typeToken = object : TypeToken<List<Song>>() {}.type
                val playlist = gson.fromJson<List<Song>>(playlistJson, typeToken) ?: emptyList()
                val index = prefs.getInt("last_index", -1)
                val position = prefs.getLong("last_position", 0L)
                val repeat = prefs.getInt("last_repeat", 0)
                val shuffle = prefs.getBoolean("last_shuffle", false)

                withContext(Dispatchers.Main) {
                    currentSong = song
                    songList = playlist
                    currentIndex = index
                    repeatMode = repeat
                    isShuffleMode = shuffle

                    prepareRestoredPlayer(song, position)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun prepareRestoredPlayer(song: Song, position: Long) {
        val ctx = context ?: return
        GlobalScope.launch(Dispatchers.IO) {
            var userId = currentUserId
            if (userId.isNullOrBlank()) {
                userId = SessionManager(ctx).getSavedUserId()
                if (!userId.isNullOrBlank()) {
                    currentUserId = userId
                }
            }
            val localSong = if (!userId.isNullOrBlank()) {
                com.example.app.model.room.AppDatabase.getDatabase(ctx).songDao()
                    .getDownloadedSongById(song.id, userId)
            } else null

            val uriString = if (localSong != null && java.io.File(localSong.localAudioPath).exists()) {
                localSong.localAudioPath
            } else {
                song.audioUrl.toString()
            }

            val finalUri = resolveUri(uriString)

            withContext(Dispatchers.Main) {
                if (player == null) {
                    init(ctx)
                }
                val mediaItem = MediaItem.fromUri(finalUri)
                player?.setMediaItem(mediaItem)
                player?.prepare()
                player?.seekTo(position)
                player?.pause()
                onSongChanged?.invoke(song, false)
                onDurationChanged?.invoke(getDuration())
            }
        }
    }

    private fun startPeriodicSave() {
        saveJob?.cancel()
        saveJob = GlobalScope.launch(Dispatchers.Main) {
            while (isPlaying()) {
                savePlaybackState()
                delay(2000)
            }
        }
    }

    private fun stopPeriodicSave() {
        saveJob?.cancel()
        saveJob = null
        savePlaybackState()
    }
}