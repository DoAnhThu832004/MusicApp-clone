package com.example.app.view.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ModifierInfo
import androidx.compose.ui.unit.dp
import com.example.app.model.response.Album
import com.example.app.model.response.Artist
import com.example.app.model.response.Playlist
import com.example.app.model.response.Song
import com.example.app.view.Song.SongScreen
import com.example.app.view.general.SearchBar
import com.example.app.viewmodel.AlbumViewModel
import com.example.app.viewmodel.ArtistViewModel
import com.example.app.viewmodel.PlaylistViewModel
import com.example.app.viewmodel.SearchViewModel
import com.example.app.viewmodel.SongViewModel

import com.example.app.viewmodel.RecommendationViewModel

@Composable
fun HomePageU(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    errorMessage: String?,
    songs: List<Song>,
    topSongs: List<Song>,
    albums: List<Album>,
    playlists: List<Playlist>,
    songViewModel: SongViewModel,
    searchViewModel: SearchViewModel,
    recommendationViewModel: RecommendationViewModel? = null,
    onViewAllSongs: (genreId: String?) -> Unit,
    onPlayerScreen: (Song) -> Unit,
    onAlbumScreen: (Album) -> Unit,
    onArtistScreen: (Artist) -> Unit,
    onClickToTopChart: () -> Unit,
    onToDetailClick: (Playlist) -> Unit,
    onAiCompanionClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SearchBar(searchViewModel = searchViewModel, onArtistClick = onArtistScreen ,onSongClick = onPlayerScreen)
        if (onAiCompanionClick != null) {
            AiCompanionHomeCard(onClick = onAiCompanionClick)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                //.background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier.align(Alignment.Center))
                }
                errorMessage != null -> {
                    Text(text = "error: ${errorMessage}")
                }
                else -> {
                    SongScreen(
                        songs = songs,
                        topSong = topSongs,
                        albums = albums,
                        playlists = playlists,
                        songViewModel = songViewModel,
                        recommendationViewModel = recommendationViewModel,
                        onViewAllClick = onViewAllSongs,
                        onSongClick = { song ->
                            onPlayerScreen(song)
                        },
                        onAlbumClick = { album ->
                            onAlbumScreen(album)
                        },
                        onArtistClick = { artist ->
                            onArtistScreen(artist)
                        },
                        onPlaylistClick = { playlist ->
                            onToDetailClick(playlist)
                        },
                        onClickToTopChart = onClickToTopChart,
                        onToDetailClick = {
                            onToDetailClick(it)
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun AiCompanionHomeCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "✨", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AI Music Companion",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🎧 🎻 🌿",
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "Trò chuyện & khám phá âm nhạc trực tiếp",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Text(
                    text = "Chat ngay",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
