package com.example.app.view

sealed class Screen(val route : String) {
    object LoginScreen : Screen("login_screen")
    object SplashScreen : Screen("splash_screen")
    object OnboardingScreen : Screen("onboarding_screen")
    object HomeScreen : Screen("home_screen")
    object UserHomePage : Screen("user_home_page/{name}") {
        fun createRoute(name: String) = "user_home_page/$name"
    }
    object RegisterScreen : Screen("register_screen")
    object NavigationDraw : Screen("navigation_draw/{name}") {
        fun createRoute(name : String) = "navigation_draw/$name"
    }
    object SettingPage : Screen("setting_page")
    object SettingPageA : Screen("setting_page_a")
    object ProfilePage : Screen("profile_page")
    object EditProfilePage : Screen("edit_profile_page")
    object InformationProfilePage : Screen("information_profile_page")
    object ListAllSong : Screen("list_all_song?genreId={genreId}") {
        fun createRoute(genreId: String? = null) =
            if (genreId != null) "list_all_song?genreId=$genreId" else "list_all_song"
    }
    object AlbumScreen: Screen("album_screen")
    object ArtistScreenA : Screen("artist_screen_a")
    object UploadFileSong : Screen("uploadFileSong?songId={songId}") {
        fun createRoute(songId: String) = "uploadFileSong?songId=$songId"
    }
    object EditSongScreen : Screen("editSongScreen?songId={songId}") {
        fun createRoute(songId: String) = "editSongScreen?songId=$songId"
    }
    object PlayerScreen : Screen("playerScreen?songs={songs}") {  // Dùng query param
        fun createRoute() = "playerScreen"
    }
    object AlbumDetailScreen : Screen("albumDetailScreen?albumId={albumId}") {
        fun createRoute(albumId: String) = "albumDetailScreen?albumId=$albumId"
    }
    object UpdateAlbumScreen : Screen("updateAlbumScreen?albumId={albumId}") {
        fun createRoute(albumId: String) = "updateAlbumScreen?albumId=$albumId"
    }
    object AlbumScreenDetailA : Screen("updateAlbumScreenA?albumId={albumId}") {
        fun createRoute(albumId: String) = "updateAlbumScreenA?albumId=$albumId"
    }
    object ArtistScreen : Screen("artistScreen?artistId={artistId}") {
        fun createRoute(artistId: String) = "artistScreen?artistId=$artistId"
    }
    object DetailArtistScreen : Screen("detailArtistScreen?artistId={artistId}") {
        fun createRoute(artistId: String) = "detailArtistScreen?artistId=$artistId"
    }
    object UpdateArtistScreen : Screen("updateArtistScreen?artistId={artistId}") {
        fun createRoute(artistId: String) = "updateArtistScreen?artistId=$artistId"
    }
    object MyPlaylistDetailScreen : Screen("myPlaylistDetailScreen?playlistId={playlistId}") {
        fun createRoute(playlistId: String) = "myPlaylistDetailScreen?playlistId=$playlistId"
    }
    object PlaylistScreenA : Screen("playlistScreenA")
    object UpdatePlaylistScreen : Screen("updatePlaylistScreen?playlistId={playlistId}") {
        fun createRoute(playlistId: String) = "updatePlaylistScreen?playlistId=$playlistId"
    }
    object DetailPlaylistScreen: Screen("detailPlaylistScreen?playlistId={playlistId}") {
        fun createRoute(playlistId: String) = "detailPlaylistScreen?playlistId=$playlistId"
    }
    object UploadFileAlbum : Screen("uploadFileAlbum?albumId={albumId}") {
        fun createRoute(albumId: String) = "uploadFileAlbum?albumId=$albumId"
    }
    object UploadFileArtist : Screen("uploadFileArtist?artistId={artistId}") {
        fun createRoute(artistId : String) = "uploadFileArtist?artistId=$artistId"
    }
    object UploadFilePlaylist : Screen("uploadFilePlaylist?playlistId={playlistId}") {
        fun createRoute(playlistId : String) = "uploadFilePlaylist?playlistId=$playlistId"
    }
    object TopChartPage: Screen("topChartPage")
    object AllReportScreen: Screen("allReportScreen")
    object DetailPlaylistScreenA: Screen("detailPlaylistScreenA?playlistId={playlistId}") {
        fun createRoute(playlistId: String) = "detailPlaylistScreenA?playlistId=$playlistId"
    }
    object DownloadScreen : Screen("downloadScreen")
    object FollowerArtstScreen : Screen("followerArtstScreen")
    object ChangePasswordScreen : Screen("changePasswordScreen")
    object UserManagementScreen : Screen("user_management_screen")
    // Màn hình nghe nhạc offline (không cần đăng nhập)
    object OfflinePlayerScreen : Screen("offline_player_screen")
    // Màn hình Seasonal Theme (Halloween / Tết / Lễ hội Remote Config)
    object SeasonalThemeScreen : Screen("seasonal_theme_screen")
    // Màn hình AI Companion Chatbot (Giai đoạn 2)
    object AiCompanionScreen : Screen("ai_companion_screen")
}