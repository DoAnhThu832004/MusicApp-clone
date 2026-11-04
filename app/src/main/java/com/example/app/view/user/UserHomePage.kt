package com.example.app.view.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.StackedLineChart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.modifier.modifierLocalMapOf
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.app.R
import com.example.app.model.NavItems
import com.example.app.model.response.Album
import com.example.app.model.response.Artist
import com.example.app.model.response.Playlist
import com.example.app.model.response.Song
import com.example.app.model.response.UserResponse
import com.example.app.view.admin.CustomFloatingBottomBar
import com.example.app.view.general.NoInternetScreen
import com.example.app.viewmodel.AlbumViewModel
import com.example.app.viewmodel.ArtistViewModel
import com.example.app.viewmodel.EditProfileViewModel
import com.example.app.viewmodel.FavoriteViewModel
import com.example.app.viewmodel.LoginViewModel
import com.example.app.viewmodel.PlayerViewModel
import com.example.app.viewmodel.PlaylistViewModel
import com.example.app.viewmodel.SearchViewModel
import com.example.app.viewmodel.SongViewModel

import com.example.app.viewmodel.DownloadViewModel
import com.example.app.viewmodel.RecommendationViewModel
import com.example.app.view.general.OfflineBanner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@Composable
fun UserHomePage(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    loginViewModel: LoginViewModel,
    songViewModel: SongViewModel,
    albumViewModel: AlbumViewModel,
    artistViewModel: ArtistViewModel,
    playlistViewModel: PlaylistViewModel,
    searchViewModel: SearchViewModel,
    editProfileViewModel: EditProfileViewModel,
    playerViewModel: PlayerViewModel,
    favoriteViewModel: FavoriteViewModel,
    recommendationViewModel: RecommendationViewModel? = null,
    isConnected: Boolean = true,
    name: String,
    user: UserResponse,
    onViewAllSongs: (genreId: String?) -> Unit,
    onPlayerScreen: (Song) -> Unit,
    onAlbumScreen: (Album) -> Unit,
    onArtistScreen: (Artist) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onClickToTopChart: () -> Unit,
    onToDetailClick: (Playlist) -> Unit
) {
    val navItemsList = listOf(
        NavItems(stringResource(R.string.trang_chu),Icons.Default.Home),
        NavItems(stringResource(R.string.yeu_thich),Icons.Default.Favorite),
        NavItems(stringResource(R.string.xin_chao), Icons.Filled.StackedLineChart),
        NavItems(stringResource(R.string.ho_so),Icons.Default.Person)
    )
    var selectIndex by rememberSaveable { mutableStateOf(0) }
    Scaffold(
        bottomBar = {
            CustomFloatingBottomBar(
                items = navItemsList,
                selectedIndex = selectIndex,
                onItemClick = { index -> selectIndex = index}
            )
        },
        modifier = modifier
            .padding(bottom = 24.dp)
    ) {
        ContentScreen(
            modifier = Modifier.padding(it),
            navController = navController,
            loginViewModel = loginViewModel,
            selectedIndex = selectIndex,
            name = name,
            songViewModel = songViewModel,
            albumViewModel = albumViewModel,
            artistViewModel = artistViewModel,
            playlistViewModel= playlistViewModel,
            searchViewModel = searchViewModel,
            editProfileViewModel = editProfileViewModel,
            playerViewModel = playerViewModel,
            favoriteViewModel = favoriteViewModel,
            recommendationViewModel = recommendationViewModel,
            isConnected = isConnected,
            user = user,
            onViewAllSongs = onViewAllSongs,
            onPlayerScreen = onPlayerScreen,
            onAlbumScreen = onAlbumScreen,
            onArtistScreen = onArtistScreen,
            onPlaylistClick = onPlaylistClick,
            onClickToTopChart = { selectIndex = 2 },
            onToDetailClick = onToDetailClick
        )
    }
}
@Composable
fun ContentScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    loginViewModel: LoginViewModel,
    selectedIndex: Int,
    name: String,
    songViewModel: SongViewModel,
    albumViewModel: AlbumViewModel,
    artistViewModel: ArtistViewModel,
    playlistViewModel: PlaylistViewModel,
    searchViewModel: SearchViewModel,
    editProfileViewModel: EditProfileViewModel,
    playerViewModel: PlayerViewModel,
    favoriteViewModel: FavoriteViewModel,
    recommendationViewModel: RecommendationViewModel? = null,
    isConnected: Boolean = true,
    user: UserResponse,
    onViewAllSongs: (genreId: String?) -> Unit,
    onPlayerScreen: (Song) -> Unit,
    onAlbumScreen: (Album) -> Unit,
    onArtistScreen: (Artist) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onClickToTopChart: () -> Unit,
    onToDetailClick: (Playlist) -> Unit
) {
    val songState by songViewModel.songState.collectAsState()
    val albumState by albumViewModel.albumState.collectAsState()
    val playlistState by playlistViewModel.playlistState.collectAsState()
    val artistState by artistViewModel.artistState.collectAsState()
    val selectedMoodTab by songViewModel.selectedMoodTab.collectAsState()
    val pagingGenreId by songViewModel.pagingGenreId.collectAsState()
    LaunchedEffect(Unit) {
        songViewModel.getSongs()
        songViewModel.getTopSongs()
        songViewModel.getRecentlyPlayedSongs()
        artistViewModel.getArtists()
    }
    // Khi genre filter thay đổi, album và playlist cũng thay đổi theo
    LaunchedEffect(pagingGenreId) {
        val genreId = pagingGenreId
        if (genreId == null) {
            albumViewModel.getAlbums()
            playlistViewModel.getPlaylists()
        } else {
            albumViewModel.getAlbumsByGenre(genreId)
            playlistViewModel.getPlaylistsByGenre(genreId)
        }
    }
    LaunchedEffect(playerViewModel.currentSong.value) {
        if (playerViewModel.currentSong.value != null) {
            songViewModel.getRecentlyPlayedSongs()
        }
    }
    when(selectedIndex) {
        0 -> {
            // Tab Home: khi mất mạng hiện OfflineBanner + thông báo nhỏ thay vì che toàn màn hình
            if (!isConnected) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OfflineBanner(visible = true)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Không có kết nối mạng",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Vào tab Hồ sơ → Nhạc đã tải để nghe offline",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }
            } else {
                val isScreenLoading = songState.isLoading || albumState.isLoading || playlistState.isLoading || artistState.isLoadingA
                val screenError = playlistState.error ?: songState.error ?: albumState.error
                val currentTopSongs = remember(songState.topSongs) { songState.topSongs?.take(5) ?: emptyList() }
                HomePageU(
                    isLoading = isScreenLoading,
                    errorMessage = screenError,
                    songs = songState.songs ?: emptyList(),
                    topSongs = currentTopSongs,
                    albums = albumState.albums ?: emptyList(),
                    playlists = playlistState.adminPlaylists ?: emptyList(), // Use adminPlaylists
                    songViewModel = songViewModel,
                    searchViewModel = searchViewModel,
                    recommendationViewModel = recommendationViewModel,
                    onViewAllSongs = onViewAllSongs,
                    onPlayerScreen = onPlayerScreen,
                    onAlbumScreen = onAlbumScreen,
                    onArtistScreen = onArtistScreen,
                    onClickToTopChart = onClickToTopChart,
                    onToDetailClick = onToDetailClick,
                    onAiCompanionClick = { navController.navigate(com.example.app.view.Screen.AiCompanionScreen.route) }
                )
            }
        }
        1 -> {
            // Tab Favorite: khi mất mạng hiện OfflineBanner + thông báo
            if (!isConnected) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OfflineBanner(visible = true)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tính năng Yêu thích cần kết nối mạng",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                FavoritePage(
                    favoriteViewModel = favoriteViewModel,
                    songViewModel = songViewModel,
                    playerViewModel = playerViewModel,
                    onSongClick = { onPlayerScreen(it) }
                )
            }
        }
        2 -> {
            // Tab TopChart: khi mất mạng hiện OfflineBanner + thông báo
            if (!isConnected) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OfflineBanner(visible = true)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Top Chart cần kết nối mạng",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                TopChartPage(
                    topSongs = songViewModel.songState.value.topSongs ?: emptyList(),
                    onSongClick = { onPlayerScreen(it) }
                )
            }
        }
        // Tab Profile (index=3): KHÔNG bị chặn bởi NetworkAware
        // Download và Logout vẫn hoạt động offline
        3 -> ProfilePage(
            navController = navController,
            loginViewModel = loginViewModel,
            artistViewModel = artistViewModel,
            playlistViewModel = playlistViewModel,
            editProfileViewModel = editProfileViewModel,
            name = name,
            user = user,
            isConnected = isConnected,
            onPlaylistClick = onPlaylistClick
        )
    }
}