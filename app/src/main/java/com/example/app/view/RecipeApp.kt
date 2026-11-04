package com.example.app.view

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.app.model.ApiClient
import com.example.app.model.repository.SongRepository
import com.example.app.view.Album.AlbumDetailScreen
import com.example.app.view.Artist.ArtistScreen
import com.example.app.view.InProfile.ChangePasswordScreen
import com.example.app.view.InProfile.DownloadScreen
import com.example.app.view.InProfile.FollowerArtistScreen
import com.example.app.view.Login.LoginScreen
import com.example.app.view.Login.RegisterScreen
import com.example.app.view.Player.MiniPlayer
import com.example.app.view.Player.PlayerScreen
import com.example.app.view.Playlist.DetailPlaylistScreen
import com.example.app.view.Playlist.MyPlaylistDetailScreen
import com.example.app.view.Song.ListAllSong
import com.example.app.view.admin.NavigationDraw
import com.example.app.view.admin.report.AllReportScreen
import com.example.app.view.general.NoInternetScreen
import com.example.app.view.user.EditProfilePage
import com.example.app.view.user.InformationProfilePage
import com.example.app.view.user.SettingPage
import com.example.app.view.user.TopChartPage
import com.example.app.view.user.UserHomePage
import com.example.app.viewmodel.AlbumViewModel
import com.example.app.viewmodel.AlbumViewModelFactory
import com.example.app.viewmodel.ArtistViewModel
import com.example.app.viewmodel.ArtistViewModelFactory
import com.example.app.viewmodel.CommentViewModel
import com.example.app.viewmodel.CommentViewModelFactory
import com.example.app.viewmodel.EditProfileViewModel
import com.example.app.viewmodel.EditProfileViewModelFactory
import com.example.app.viewmodel.FavoriteViewModel
import com.example.app.viewmodel.FavoriteViewModelFactory
import com.example.app.viewmodel.LoginViewModel
import com.example.app.viewmodel.LoginViewModelFactory
import com.example.app.viewmodel.PlayerViewModel
import com.example.app.viewmodel.PlaylistViewModel
import com.example.app.viewmodel.PlaylistViewModelFactory
import com.example.app.viewmodel.ChangePasswordViewModel
import com.example.app.viewmodel.ChangePasswordViewModelFactory
import com.example.app.viewmodel.PlayerManager
import com.example.app.viewmodel.RegisterViewModel
import com.example.app.viewmodel.RegisterViewModelFactory
import com.example.app.viewmodel.ReportViewModel
import com.example.app.viewmodel.ReportViewModelFactory
import com.example.app.viewmodel.SearchViewModel
import com.example.app.viewmodel.SearchViewModelFactory
import com.example.app.viewmodel.SessionManager
import com.example.app.viewmodel.SongViewModel
import com.example.app.viewmodel.SongViewModelFactory
import com.example.app.viewmodel.UserViewModel
import com.example.app.viewmodel.UserViewModelFactory
import com.example.app.viewmodel.rememberNetworkState

/**
 * Wrapper kiểm tra kết nối mạng.
 * Nếu [isConnected] = false → hiển thị NoInternetScreen.
 * Nếu [isConnected] = true  → hiển thị [content].
 */
@Composable
fun NetworkAwareWrapper(
    isConnected: Boolean,
    content: @Composable () -> Unit
) {
    if (isConnected) {
        content()
    } else {
        NoInternetScreen()
    }
}

@Composable
fun RecipeApp(
    navController: NavHostController,
    modifier: Modifier,
    darkTheme: Boolean,
    onThemeUpdated: () -> Unit
) {
    val context = LocalContext.current
    val isConnected by rememberNetworkState()
    val apiService = ApiClient.build(context)
    val songRepository = remember { SongRepository(apiService) }
    val albumRepository = remember { com.example.app.model.repository.AlbumRepository(apiService) }
    val artistRepository = remember { com.example.app.model.repository.ArtistRepository(apiService) }
    val playlistRepository = remember { com.example.app.model.repository.PlaylistRepository(apiService) }
    val commentRepository = remember { com.example.app.model.repository.CommentRepository(apiService) }
    val reportRepository = remember { com.example.app.model.repository.ReportRepository(apiService) }
    val userRepository = remember { com.example.app.model.repository.UserRepository(apiService) }
    val searchRepository = remember { com.example.app.model.repository.SearchRepository(apiService) }
    val sessionManager = remember { SessionManager(context) }
    val database = remember { com.example.app.model.room.AppDatabase.getDatabase(context) }
    val downloadRepository = remember { com.example.app.model.repository.DownloadRepository(apiService, database.songDao(), context) }

    val playerViewModel : PlayerViewModel = viewModel(
        factory = com.example.app.viewmodel.PlayerViewModelFactory(songRepository)
    )

    // FavoriteViewModel ở cấp RecipeApp để share state giữa tất cả màn hình
    val favoriteViewModel: FavoriteViewModel = viewModel(
        factory = FavoriteViewModelFactory(songRepository)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Telemetry: Ghi nhận screen_view tự động khi chuyển màn hình
    LaunchedEffect(currentRoute) {
        if (!currentRoute.isNullOrEmpty()) {
            com.example.app.analytics.AnalyticsHelper.logEvent(com.google.firebase.analytics.FirebaseAnalytics.Event.SCREEN_VIEW) {
                putString(com.google.firebase.analytics.FirebaseAnalytics.Param.SCREEN_NAME, currentRoute)
                putString(com.google.firebase.analytics.FirebaseAnalytics.Param.SCREEN_CLASS, currentRoute)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.SplashScreen.route,
            enterTransition = { slideInHorizontally { it } },
            exitTransition = { slideOutHorizontally { -it } },
            popEnterTransition = { slideInHorizontally { -it } },
            popExitTransition = { slideOutHorizontally { it } }
        ) {
            composable(route = Screen.LoginScreen.route) {
                val loginViewModel : LoginViewModel = viewModel(
                    factory = LoginViewModelFactory(userRepository, sessionManager)
                )
                val editProfileViewModel : EditProfileViewModel = viewModel(
                    factory = EditProfileViewModelFactory(userRepository, loginViewModel, sessionManager)
                )
                // LoginScreen KHÔNG bọc NetworkAwareWrapper:
                // – Khi có mạng: user đăng nhập bình thường
                // – Khi mất mạng: hiện OfflineBanner + disable nút đăng nhập
                //   + hiện nút "Nghe nhạc offline" để vào OfflinePlayerScreen
                LoginScreen(
                    loginViewModel = loginViewModel,
                    editProfileViewModel = editProfileViewModel,
                    navController = navController,
                    navigateToRegister = { navController.navigate(Screen.RegisterScreen.route) },
                    navigateToUserHomePage = { token, name ->
                        navController.currentBackStackEntry?.savedStateHandle?.set("token", token)
                        navController.currentBackStackEntry?.savedStateHandle?.set("name", name)
                    },
                    isConnected = isConnected,
                    onGoToOfflinePlayer = {
                        navController.navigate(Screen.OfflinePlayerScreen.route)
                    }
                )
            }
            composable(route = Screen.RegisterScreen.route) {
                val registerViewModel : RegisterViewModel = viewModel(
                    factory = RegisterViewModelFactory(userRepository, songRepository)
                )
                NetworkAwareWrapper(isConnected = isConnected) {
                    RegisterScreen(
                        registerViewModel = registerViewModel,
                        navigateToLogin = { navController.navigate(Screen.LoginScreen.route) }
                    )
                }
            }
            composable(
                route = Screen.SplashScreen.route,
                exitTransition = { ExitTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                // [FIX #3] Business logic (token/role) đã chuyển sang SplashViewModel
                // Composable chỉ observe StateFlow và navigate – không làm I/O trực tiếp
                val loginViewModel : LoginViewModel = viewModel(
                    factory = LoginViewModelFactory(userRepository, sessionManager)
                )
                val splashViewModel : com.example.app.viewmodel.SplashViewModel = viewModel(
                    factory = com.example.app.viewmodel.SplashViewModelFactory(
                        sessionManager = sessionManager,
                        loginViewModel = loginViewModel,
                        context = context                  // Thêm context để đọc DataStore onboarding
                    )
                )
                com.example.app.view.general.SplashScreen(
                    navController   = navController,
                    splashViewModel = splashViewModel
                )
            }

            // ── Onboarding – chỉ hiện lần đầu dùng app ──────────────────────
            composable(
                route = Screen.OnboardingScreen.route,
                exitTransition = { ExitTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                val onboardingViewModel: com.example.app.viewmodel.OnboardingViewModel = viewModel(
                    factory = com.example.app.viewmodel.OnboardingViewModelFactory(context)
                )
                com.example.app.view.general.OnboardingScreen(
                    onFinish = {
                        // Lưu DataStore rồi navigate sang Login, xoá backstack
                        onboardingViewModel.markOnboardingDone()
                        navController.navigate(Screen.LoginScreen.route) {
                            popUpTo(Screen.OnboardingScreen.route) { inclusive = true }
                        }
                    }
                )
            }
//        composable(route = Screen.HomeScreen.route) {
//            HomePage()
//        }
            composable(route = Screen.NavigationDraw.route) { backStackEntry ->
                val name = backStackEntry.arguments?.getString("name") ?: "Guest"
                val loginViewModel : LoginViewModel = viewModel(
                    factory = LoginViewModelFactory(userRepository, sessionManager)
                )
                val editProfileViewModel : EditProfileViewModel = viewModel(
                    factory = EditProfileViewModelFactory(userRepository, loginViewModel, sessionManager)
                )
                val songViewModel : SongViewModel = viewModel(
                    factory = SongViewModelFactory(songRepository)
                )
                val searchViewModel : SearchViewModel = viewModel(
                    factory = SearchViewModelFactory(searchRepository)
                )
                val albumViewModel : AlbumViewModel = viewModel(
                    factory = AlbumViewModelFactory(albumRepository)
                )
                val artistViewModel : ArtistViewModel = viewModel(
                    factory = ArtistViewModelFactory(artistRepository)
                )
                val playlistViewModel : PlaylistViewModel = viewModel(
                    factory = PlaylistViewModelFactory(playlistRepository)
                )
                val userViewModel : UserViewModel = viewModel(
                    factory = UserViewModelFactory(userRepository)
                )
                val reportViewModel : ReportViewModel = viewModel(factory = ReportViewModelFactory(reportRepository))
                val reportState by reportViewModel.reportState.collectAsState()
                val reports = reportState.reports ?: emptyList()
                NetworkAwareWrapper(isConnected = isConnected) {
                    NavigationDraw(
                        navController = navController,
                        loginViewModel = loginViewModel,
                        editProfileViewModel = editProfileViewModel,
                        songViewModel = songViewModel,
                        albumViewModel = albumViewModel,
                        artistViewModel = artistViewModel,
                        searchViewModel = searchViewModel,
                        playlistViewModel = playlistViewModel,
                        userViewModel = userViewModel,
                        reports = reports,
                        darkTheme = darkTheme,
                        onThemeUpdated = onThemeUpdated,
                        name = name,
                        onToReport = {
                            navController.navigate(Screen.AllReportScreen.route)
                        }
                    )
                }
            }
            composable(route = Screen.UserHomePage.route) { backStackEntry ->
                val name = backStackEntry.arguments?.getString("name") ?: "Guest"
                val loginViewModel : LoginViewModel = viewModel(
                    factory = LoginViewModelFactory(userRepository, sessionManager)
                )
                val editProfileViewModel : EditProfileViewModel = viewModel(
                    factory = EditProfileViewModelFactory(userRepository, loginViewModel, sessionManager)
                )
                val songViewModel : SongViewModel = viewModel(
                    factory = SongViewModelFactory(songRepository)
                )
                val searchViewModel : SearchViewModel = viewModel(
                    factory = SearchViewModelFactory(searchRepository)
                )
                val albumViewModel : AlbumViewModel = viewModel(
                    factory = AlbumViewModelFactory(albumRepository)
                )
                val artistViewModel : ArtistViewModel = viewModel(
                    factory = ArtistViewModelFactory(artistRepository)
                )
                val playlistViewModel : PlaylistViewModel = viewModel(
                    factory = PlaylistViewModelFactory(playlistRepository)
                )
                val recommendationViewModel: com.example.app.viewmodel.RecommendationViewModel = viewModel(
                    factory = com.example.app.viewmodel.RecommendationViewModelFactory(songRepository)
                )
                val songState by songViewModel.songState.collectAsState()
                val songs = songState.songs ?: emptyList()
                val userState by editProfileViewModel.editUiState.collectAsState()
                
                LaunchedEffect(userState.userResponse?.result?.id) {
                    val userId = userState.userResponse?.result?.id
                    if (!userId.isNullOrBlank()) {
                        recommendationViewModel.getHomeRecommendations(userId = userId)
                        PlayerManager.currentUserId = userId // Cập nhật userId cho PlayerManager
                        PlayerManager.restorePlaybackState() // Khôi phục trạng thái phát nhạc trước đó
                    }
                }

                LaunchedEffect(userState.userResponse?.result?.id) {
                    // Load danh sách yêu thích khi có userId
                    if (!userState.userResponse?.result?.id.isNullOrEmpty()) {
                        favoriteViewModel.loadFavoriteSongs()
                    }
                }

                // UserHomePage không bọc NetworkAwareWrapper ở đây
                // vì ContentScreen xử lý từng tab riêng: tab 0,1,2 bị chặn,
                // tab Profile (index=3) vẫn hoạt động khi không có mạng
                userState.userResponse?.let {
                    UserHomePage(
                        navController = navController,
                        loginViewModel = loginViewModel,
                        songViewModel = songViewModel,
                        albumViewModel = albumViewModel,
                        artistViewModel = artistViewModel,
                        playlistViewModel = playlistViewModel,
                        searchViewModel = searchViewModel,
                        editProfileViewModel = editProfileViewModel,
                        playerViewModel = playerViewModel,
                        favoriteViewModel = favoriteViewModel,
                        recommendationViewModel = recommendationViewModel,
                        isConnected = isConnected,
                        name = name,
                        user = it,
                        onViewAllSongs = { genreId ->
                            navController.navigate(Screen.ListAllSong.createRoute(genreId))
                        },
                        onPlayerScreen = { song ->
                            playerViewModel.play(song, songs)
                            navController.navigate(Screen.PlayerScreen.createRoute())
                        },
                        onAlbumScreen = { album ->
                            navController.navigate(Screen.AlbumDetailScreen.createRoute(album.id))
                        },
                        onArtistScreen = { artist ->
                            navController.navigate(Screen.ArtistScreen.createRoute(artist.id))
                        },
                        onPlaylistClick = { id ->
                            navController.navigate(Screen.MyPlaylistDetailScreen.createRoute(id))
                        },
                        onClickToTopChart = {
                            navController.navigate(Screen.TopChartPage.route)
                        },
                        onToDetailClick = {
                            navController.navigate(Screen.DetailPlaylistScreen.createRoute(it.id))
                        }
                    )
                }
            }
            composable(route = Screen.SettingPage.route) {
                // SettingPage: không cần mạng (chỉ cài đặt local)
                SettingPage(
                    navController = navController,
                    darkTheme = darkTheme,
                    onThemeUpdated = onThemeUpdated
                )
            }
            composable(route = Screen.EditProfilePage.route) {
                val loginViewModel : LoginViewModel = viewModel(
                    factory = LoginViewModelFactory(userRepository, sessionManager)
                )
                val editProfileViewModel : EditProfileViewModel = viewModel(
                    factory = EditProfileViewModelFactory(userRepository, loginViewModel, sessionManager)
                )
                NetworkAwareWrapper(isConnected = isConnected) {
                    EditProfilePage(
                        navController = navController,
                        loginViewModel = loginViewModel,
                        editProfileViewModel = editProfileViewModel
                    )
                }
            }
            composable(route = Screen.InformationProfilePage.route) {
                val loginViewModel : LoginViewModel = viewModel(
                    factory = LoginViewModelFactory(userRepository, sessionManager)
                )
                val editProfileViewModel : EditProfileViewModel = viewModel(
                    factory = EditProfileViewModelFactory(userRepository, loginViewModel, sessionManager)
                )
                NetworkAwareWrapper(isConnected = isConnected) {
                    InformationProfilePage(
                        navController = navController,
                        editProfileViewModel = editProfileViewModel
                    )
                }
            }
            composable(route = Screen.ListAllSong.route) {
                val genreId = it.arguments?.getString("genreId")
                val songViewModel : SongViewModel = viewModel(
                    factory = SongViewModelFactory(songRepository)
                )
                // Set genre filter ngay khi màn hình khởi tạo
                LaunchedEffect(genreId) {
                    songViewModel.setPagingGenreFilter(genreId)
                }
                val pagingSongs = songViewModel.songsPaging.collectAsLazyPagingItems()
                NetworkAwareWrapper(isConnected = isConnected) {
                    ListAllSong(
                        songs = pagingSongs,
                        onBack = { navController.popBackStack() },
                        songViewModel = songViewModel,
                        playerViewModel = playerViewModel,
                        favoriteViewModel = favoriteViewModel,
                        onSongClick = { song ->
                            val songs = pagingSongs.itemSnapshotList.items.filterNotNull()
                            playerViewModel.play(song, songs)
                            navController.navigate(Screen.PlayerScreen.createRoute())
                        }
                    )
                }
            }
            composable(route = Screen.PlayerScreen.createRoute()) {
                val songViewModel : SongViewModel = viewModel(
                    factory = SongViewModelFactory(songRepository)
                )
                val commentViewModel : CommentViewModel = viewModel(
                    factory = CommentViewModelFactory(commentRepository)
                )
                val reportViewModel : ReportViewModel = viewModel(
                    factory = ReportViewModelFactory(reportRepository)
                )
                val downloadViewModel : com.example.app.viewmodel.DownloadViewModel = viewModel(
                    factory = com.example.app.viewmodel.DownloadViewModelFactory(downloadRepository, sessionManager)
                )
                val loginViewModel : LoginViewModel = viewModel(
                    factory = LoginViewModelFactory(userRepository, sessionManager)
                )
                val editProfileViewModel : EditProfileViewModel = viewModel(
                    factory = EditProfileViewModelFactory(userRepository, loginViewModel, sessionManager)
                )
                // PlayerScreen KHÔNG bọc NetworkAwareWrapper vì:
                // - Bài hát đã tải về (offline) phát từ local file, không cần mạng
                // - PlayerManager tự kiểm tra local file trước khi dùng stream URL
                PlayerScreen(
                    playerViewModel = playerViewModel,
                    songViewModel = songViewModel,
                    favoriteViewModel = favoriteViewModel,
                    reportViewModel = reportViewModel,
                    commentViewModel = commentViewModel,
                    downloadViewModel = downloadViewModel,
                    editProfileViewModel = editProfileViewModel,
                    onBack = {navController.popBackStack()}
                )
            }
            composable(route = Screen.AlbumDetailScreen.route) {
                val albumId = it.arguments?.getString("albumId")
                val albumViewModel: AlbumViewModel = viewModel(factory = AlbumViewModelFactory(albumRepository))
                val reportViewModel : ReportViewModel = viewModel(
                    factory = ReportViewModelFactory(reportRepository)
                )
                val albumState by albumViewModel.albumState.collectAsState()
                val currentAlbum by albumViewModel.currentAlbumDetail.collectAsState()
                LaunchedEffect(albumId) {
                    if (albumId != null) {
                        albumViewModel.getAlbumById(albumId)
                    }
                }
                NetworkAwareWrapper(isConnected = isConnected) {
                    if (albumState.isLoading) {
                        // Giao diện chờ tải dữ liệu mạng
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        // Giao diện khi đã có dữ liệu
                        currentAlbum?.let { album ->
                            AlbumDetailScreen(
                                album = album,
                                reportViewModel = reportViewModel,
                                onSongClick = { song ->
                                    val playlistToPlay = album.songs ?: emptyList()
                                    playerViewModel.play(song, playlistToPlay)
                                    navController.navigate(Screen.PlayerScreen.createRoute())
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
            composable(route = Screen.ArtistScreen.route) {
                val artistId = it.arguments?.getString("artistId")
                val loginViewModel : LoginViewModel = viewModel(
                    factory = LoginViewModelFactory(userRepository, sessionManager)
                )
                val editProfileViewModel : EditProfileViewModel = viewModel(
                    factory = EditProfileViewModelFactory(userRepository, loginViewModel, sessionManager)
                )
                val albumViewModel : AlbumViewModel = viewModel(
                    factory = AlbumViewModelFactory(albumRepository)
                )
                val artistViewModel : ArtistViewModel = viewModel(
                    factory = ArtistViewModelFactory(artistRepository)
                )
                val artistState by artistViewModel.artistState.collectAsState()
                val currentArtist by artistViewModel.currentArtist.collectAsState()
                LaunchedEffect(artistId) {
                    if (artistId != null) {
                        artistViewModel.getArtistById(artistId)
                    }
                }
                NetworkAwareWrapper(isConnected = isConnected) {
                    if (artistState.isLoadingA) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        currentArtist?.let { artist ->
                            ArtistScreen(
                                artist = artist,
                                onSongClick = { song ->
                                    val playlistToPlay = artist.songs ?: emptyList()
                                    playerViewModel.play(song, playlistToPlay)
                                    navController.navigate(Screen.PlayerScreen.createRoute())
                                },
                                onBack = { navController.popBackStack() },
                                albumViewModel = albumViewModel,
                                artistViewModel = artistViewModel,
                                editProfileViewModel = editProfileViewModel,
                                onAlbumClick = { album ->
                                    navController.navigate(Screen.AlbumDetailScreen.createRoute(album.id))
                                }
                            )
                        }
                    }
                }
            }
            composable(route = Screen.MyPlaylistDetailScreen.route) {
                val playlistId = it.arguments?.getString("playlistId")
                val playlistViewModel : PlaylistViewModel = viewModel(
                    factory = PlaylistViewModelFactory(playlistRepository)
                )
                val playlistState by playlistViewModel.playlistState.collectAsState()
                val currentPlaylist by playlistViewModel.currentPlaylistDetail.collectAsState()
                LaunchedEffect(playlistId) {
                    if (playlistId != null) {
                        playlistViewModel.getPlaylistById(playlistId)
                    }
                }
                NetworkAwareWrapper(isConnected = isConnected) {
                    if (playlistState.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        currentPlaylist?.let { playlist ->
                            MyPlaylistDetailScreen(
                                playlist = playlist,
                                playlistViewModel = playlistViewModel,
                                onSongClick = { song ->
                                    playerViewModel.play(song)
                                    navController.navigate(Screen.PlayerScreen.createRoute())
                                },
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
            composable(route = Screen.TopChartPage.route) {
                val songViewModel : SongViewModel = viewModel(
                    factory = SongViewModelFactory(songRepository)
                )
                val songState by songViewModel.songState.collectAsState()
                val songs = songState.songs ?: emptyList()
                NetworkAwareWrapper(isConnected = isConnected) {
                    TopChartPage(
                        topSongs = songViewModel.songState.value.topSongs ?: emptyList(),
                        onSongClick = {
                            playerViewModel.play(it, songs)
                            navController.navigate(Screen.PlayerScreen.createRoute())
                        }
                    )
                }
            }
            composable(route = Screen.AllReportScreen.route) {
                val songViewModel : SongViewModel = viewModel(
                    factory = SongViewModelFactory(songRepository)
                )
                val albumViewModel : AlbumViewModel = viewModel(
                    factory = AlbumViewModelFactory(albumRepository)
                )
                val artistViewModel : ArtistViewModel = viewModel(
                    factory = ArtistViewModelFactory(artistRepository)
                )
                val playlistViewModel : PlaylistViewModel = viewModel(
                    factory = PlaylistViewModelFactory(playlistRepository)
                )
                val reportViewModel : ReportViewModel = viewModel(
                    factory = ReportViewModelFactory(reportRepository)
                )
                val reportState by reportViewModel.reportState.collectAsState()
                NetworkAwareWrapper(isConnected = isConnected) {
                    AllReportScreen(
                        reports = reportState.reports ?: emptyList(),
                        songViewModel = songViewModel,
                        reportViewModel = reportViewModel,
                        albumViewModel = albumViewModel,
                        playlistViewModel = playlistViewModel,
                        artistViewModel = artistViewModel,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
            composable(route = Screen.DetailPlaylistScreen.route) {
                val playlistId = it.arguments?.getString("playlistId")
                val playlistViewModel : PlaylistViewModel = viewModel(
                    factory = PlaylistViewModelFactory(playlistRepository)
                )
                val reportViewModel : ReportViewModel = viewModel(
                    factory = ReportViewModelFactory(reportRepository)
                )
                val playlistState by playlistViewModel.playlistState.collectAsState()
                val currentPlaylist by playlistViewModel.currentPlaylistDetail.collectAsState()
                LaunchedEffect(playlistId) {
                    if (playlistId != null) {
                        playlistViewModel.getPlaylistById(playlistId)
                    }
                }
                NetworkAwareWrapper(isConnected = isConnected) {
                    if (playlistState.isLoading && currentPlaylist?.id != playlistId) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        currentPlaylist?.let { playlist ->
                            DetailPlaylistScreen(
                                playlist = playlist,
                                playlistViewModel = playlistViewModel,
                                reportViewModel = reportViewModel,
                                onBack = { navController.popBackStack() },
                                onSongClick = { song ->
                                    //val playlistToPlay = playlist. ?: emptyList()
                                    playerViewModel.play(song)
                                    navController.navigate(Screen.PlayerScreen.createRoute())
                                }
                            )
                        }
                    }
                }
            }
            composable(route = Screen.DownloadScreen.route) {
                val downloadViewModel : com.example.app.viewmodel.DownloadViewModel = viewModel(
                    factory = com.example.app.viewmodel.DownloadViewModelFactory(downloadRepository, sessionManager)
                )
                val loginViewModel : LoginViewModel = viewModel(
                    factory = LoginViewModelFactory(userRepository, sessionManager)
                )
                val editProfileViewModel : EditProfileViewModel = viewModel(
                    factory = EditProfileViewModelFactory(userRepository, loginViewModel, sessionManager)
                )
                DownloadScreen(
                    downloadViewModel = downloadViewModel,
                    playerViewModel = playerViewModel,
                    editProfileViewModel = editProfileViewModel,
                    onBack = { navController.popBackStack() },
                    onSongClick = { song ->
                        playerViewModel.play(song, emptyList())
                        navController.navigate(Screen.PlayerScreen.createRoute())
                    }
                )
            }
            composable(route = Screen.FollowerArtstScreen.route) {
                val artistViewModel : ArtistViewModel = viewModel(
                    factory = ArtistViewModelFactory(artistRepository)
                )
                val artistState by artistViewModel.artistState.collectAsState()
                val artists = artistState.artists ?: emptyList()
                LaunchedEffect(Unit) {
                    artistViewModel.getFollowerOfUser()
                }
                NetworkAwareWrapper(isConnected = isConnected) {
                    FollowerArtistScreen(
                        artist = artists,
                        onBack = {
                            navController.popBackStack()
                        },
                        onArtistClick = {
                            navController.navigate(Screen.ArtistScreen.createRoute(it.id))
                        }
                    )
                }
            }
            composable(route = Screen.ChangePasswordScreen.route) {
                val loginViewModel : LoginViewModel = viewModel(
                    factory = LoginViewModelFactory(userRepository, sessionManager)
                )
                val editProfileViewModel : EditProfileViewModel = viewModel(
                    factory = EditProfileViewModelFactory(userRepository, loginViewModel, sessionManager)
                )
                val changePasswordViewModel : ChangePasswordViewModel = viewModel(
                    factory = ChangePasswordViewModelFactory(userRepository, loginViewModel, editProfileViewModel)
                )
                NetworkAwareWrapper(isConnected = isConnected) {
                    ChangePasswordScreen(
                        changePasswordViewModel = changePasswordViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // ── Màn hình nghe nhạc offline (không cần đăng nhập) ─────────────
            // Truy cập được từ LoginScreen khi mất mạng qua nút "Nghe nhạc đã tải"
            composable(route = Screen.OfflinePlayerScreen.route) {
                val loginViewModel: LoginViewModel = viewModel(
                    factory = LoginViewModelFactory(userRepository, sessionManager)
                )
                val downloadViewModel: com.example.app.viewmodel.DownloadViewModel = viewModel(
                    factory = com.example.app.viewmodel.DownloadViewModelFactory(
                        downloadRepository = downloadRepository,
                        sessionManager = sessionManager
                    )
                )
                val editProfileViewModel: EditProfileViewModel = viewModel(
                    factory = EditProfileViewModelFactory(userRepository, loginViewModel, sessionManager)
                )
                com.example.app.view.InProfile.DownloadScreen(
                    downloadViewModel    = downloadViewModel,
                    playerViewModel      = playerViewModel,
                    editProfileViewModel = editProfileViewModel,
                    onBack               = { navController.popBackStack() },
                    onSongClick          = { song ->
                        navController.navigate(Screen.PlayerScreen.createRoute())
                        PlayerManager.play(song)
                    }
                )
            }

            // ── Màn hình Showcase Theme Mùa Lễ Hội (Halloween / Tết / Remote Config) ──
            composable(route = Screen.SeasonalThemeScreen.route) {
                com.example.app.theme.ui.HalloweenSeasonalScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // ── Màn hình AI Music Companion (Giai đoạn 2) ──
            composable(route = Screen.AiCompanionScreen.route) {
                val context = LocalContext.current
                val aiRepository = remember { ApiClient.getAiChatRepository(context) }
                val aiViewModel: com.example.app.viewmodel.AiCompanionViewModel = viewModel(
                    factory = com.example.app.viewmodel.AiCompanionViewModelFactory(aiRepository, PlayerManager)
                )
                com.example.app.view.ai.AiCompanionScreen(
                    viewModel = aiViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
        val isUserSection = currentRoute != null && 
                currentRoute != Screen.LoginScreen.route &&
                currentRoute != Screen.RegisterScreen.route &&
                currentRoute != Screen.SplashScreen.route &&
                currentRoute != Screen.NavigationDraw.route

        val isOnPlayer = currentRoute == Screen.PlayerScreen.createRoute() || currentRoute == Screen.AiCompanionScreen.route

        if (isUserSection && !isOnPlayer) {
            MiniPlayer(
                playerViewModel,
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomCenter).zIndex(1f)
                    .navigationBarsPadding()
                    .padding(bottom = 80.dp)
                    .shadow(
                        elevation = 20.dp, // Độ mờ của box
                        shape = RoundedCornerShape(36.dp),
                        spotColor = Color.Black.copy(0.6f)  // màu đậm của bóng
                    )
                    .clip(RoundedCornerShape(36.dp))
                    .border(1.dp, Color(0xFF2B2939).copy(alpha = 0.95f), RoundedCornerShape(36.dp)),
                isOnPlayerScreen = isOnPlayer,
                onBack = {
                    navController.navigate(Screen.PlayerScreen.createRoute()) {
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}