package com.example.app.view.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.model.ai.SongCardDto

/**
 * Băng chuyền ngang hiển thị các bài hát do AI gợi ý (Task 3B.4).
 */
@Composable
fun AiSongCarousel(
    songs: List<SongCardDto>,
    currentPlayingSongId: String?,
    isPlaying: Boolean,
    onPlaySong: (SongCardDto) -> Unit,
    onAddToQueue: (SongCardDto) -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Bài hát đề xuất (${songs.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(songs, key = { it.id }) { song ->
                val isCurrentPlaying = (song.id == currentPlayingSongId) && isPlaying
                AiSongCardWidget(
                    song = song,
                    isPlaying = isCurrentPlaying,
                    onPlayClick = { onPlaySong(song) },
                    onQueueClick = { onAddToQueue(song) }
                )
            }
        }
    }
}
