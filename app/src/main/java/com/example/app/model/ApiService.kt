package com.example.app.model

import android.content.Context
import com.example.app.model.request.AlbumCreationRequest
import com.example.app.model.request.AlbumUpdateRequest
import com.example.app.model.request.ArtistUpdateRequest
import com.example.app.model.request.AuthenticationRequest
import com.example.app.model.request.CommentCreationRequest
import com.example.app.model.request.CommentUpdateRequest
import com.example.app.model.request.LogoutRequest
import com.example.app.model.request.PlaylistCreateRequest
import com.example.app.model.request.PlaylistUpdateRequest
import com.example.app.model.request.RefreshRequest
import com.example.app.model.request.ReportCreationRequest
import com.example.app.model.request.ReportUpdateRequest
import com.example.app.model.request.SongCreationRequest
import com.example.app.model.request.SongUpdateRequest
import com.example.app.model.request.ChangePasswordRequest
import com.example.app.model.request.UserCreationRequest
import com.example.app.model.request.UserUpdateRequest
import com.example.app.model.response.Album
import com.example.app.model.response.ApiError
import com.example.app.model.response.ApiResponse
import com.example.app.model.response.Artist
import com.example.app.model.response.AuthenticationResponse
import com.example.app.model.response.Comment
import com.example.app.model.response.PageResponse
import com.example.app.model.response.Playlist
import com.example.app.model.response.Report
import com.example.app.model.response.Song
import com.example.app.model.response.UserResponse
import com.example.app.model.response.HomeRecommendationResponse
import com.example.app.viewmodel.SessionManager
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ApiService {
    @POST("auth/token")
    suspend fun authenticate(@Body request: AuthenticationRequest): Response<AuthenticationResponse>
    @POST("users")
    suspend fun createUser(@Body request: UserCreationRequest): Response<UserResponse>
    @POST("auth/logout")
    suspend fun logout(@Body request: LogoutRequest): Response<ApiError>
    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): Response<AuthenticationResponse>
    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") id : String, @Body request: UserUpdateRequest): Response<UserResponse>
    @PUT("users/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ApiResponse<String>>
    @GET("users/my-info")
    suspend fun getUserInfo(): Response<UserResponse>
    @Multipart
    @POST("users/{id}/upload")
    suspend fun uploadUserImage(
        @Path("id") id: String,
        @Part image: MultipartBody.Part
    ): Response<UserResponse>

    @GET("users")
    suspend fun getUsers(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<ApiResponse<PageResponse<com.example.app.model.response.UserResult>>>

    @PATCH("users/{userId}/block")
    suspend fun blockUser(@Path("userId") userId: String): Response<UserResponse>

    @PATCH("users/{userId}/unblock")
    suspend fun unblockUser(@Path("userId") userId: String): Response<UserResponse>

    @GET("songs")
    suspend fun getSongs(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 10,
        @Query("genreId") genreId: String? = null
    ): Response<ApiResponse<PageResponse<Song>>>
    @GET("songs/top-charts")
    suspend fun getTopSongs(): Response<ApiResponse<List<Song>>>
    @POST("songs")
    suspend fun createSong(@Body request: SongCreationRequest): Response<ApiResponse<Song>>
    @GET("songs/searchKey")
    suspend fun searchSongs(
        @Query("name") name: String
    ): Response<ApiResponse<List<Song>>>
    @GET("songs/admin/searchKey")
    suspend fun searchSongsForAdmin(
        @Query("name") name: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<PageResponse<Song>>>
    @DELETE("songs/{id}")
    suspend fun deleteSong(@Path("id") id: String): Response<ApiError>
    @Multipart
    @POST("songs/{id}/upload")
    suspend fun uploadSongFiles(
        @Path("id") id: String,
        @Part image: MultipartBody.Part,
        @Part audio: MultipartBody.Part
    ): Response<ApiResponse<Song>>
    @PUT("songs/{id}")
    suspend fun updateSong(
        @Path("id") id: String,
        @Body request: SongUpdateRequest
    ): Response<ApiResponse<Song>>

    @POST("interactions/listen/{songId}")
    suspend fun recordListen(
        @Path("songId") songId: String
    ): Response<ApiResponse<String>>

    @GET("songs/recently-played")
    suspend fun getRecentlyPlayedSongs(): Response<ApiResponse<List<Song>>>
    @GET("albums")
    suspend fun getAlbums(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 10
    ): Response<ApiResponse<PageResponse<Album>>>
    @GET("albums/genre/{genreId}")
    suspend fun getAlbumsByGenre(
        @Path("genreId") genreId: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 10
    ): Response<ApiResponse<PageResponse<Album>>>
    @GET("albums/{albumId}")
    suspend fun getAlbumById(@Path("albumId") albumId: String): Response<ApiResponse<Album>>
    @GET("albums/searchKey")
    suspend fun searchAlbums(
        @Query("name") name: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 10
    ): Response<ApiResponse<PageResponse<Album>>>
    @GET("albums/admin/searchKey")
    suspend fun searchAlbumsForAdmin(
        @Query("name") name: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<PageResponse<Album>>>
    @POST("albums")
    suspend fun createAlbum(@Body request: AlbumCreationRequest): Response<ApiResponse<Album>>
    @DELETE("albums/{id}")
    suspend fun deleteAlbum(@Path("id") id: String): Response<ApiError>
    @PUT("albums/{id}")
    suspend fun updateAlbum(
        @Path("id") id: String,
        @Body request: AlbumUpdateRequest
    ): Response<ApiResponse<Album>>
    @DELETE("albums/{albumId}/songs/{songId}")
    suspend fun deleteSongFromAlbum(
        @Path("albumId") albumId: String,
        @Path("songId") songId: String
    ) : Response<ApiError>
    @PUT("albums/{albumId}/songs/{songId}")
    suspend fun addSongToAlbum(
        @Path("albumId") albumId: String,
        @Path("songId") songId: String
    ): Response<ApiResponse<String>>
    @GET("artists")
    suspend fun getArtists(): Response<ApiResponse<List<Artist>>>
    @GET("artists/{artistId}")
    suspend fun getArtistById(@Path("artistId") artistId: String): Response<ApiResponse<Artist>>
    @GET("artists/searchKey")
    suspend fun searchArtists(
        @Query("name") name: String
    ): Response<ApiResponse<List<Artist>>>
    @GET("artists/admin/searchKey")
    suspend fun searchArtistsForAdmin(
        @Query("name") name: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<PageResponse<Artist>>>
    @POST("artists")
    suspend fun createArtist(@Body request: com.example.app.model.request.ArtistCreationRequest): Response<ApiResponse<Artist>>
    @DELETE("artists/{id}")
    suspend fun deleteArtist(@Path("id") id: String): Response<ApiError>
    @PUT("artists/{id}")
    suspend fun updateArtist(
        @Path("id") id: String,
        @Body request: ArtistUpdateRequest
    ): Response<ApiResponse<Artist>>
    @PUT("artists/{artistId}/songs/{songId}")
    suspend fun addSongToArtist(
        @Path("artistId") artistId: String,
        @Path("songId") songId: String
    ): Response<ApiResponse<String>>
    @PUT("artists/{artistId}/albums/{albumId}")
    suspend fun addAlbumToArtist(
        @Path("artistId") artistId: String,
        @Path("albumId") albumId: String
    ): Response<ApiResponse<String>>
    @DELETE("artists/{artistId}/albums/{albumId}")
    suspend fun deleteAlbumFromArtist(
        @Path("artistId") artistId: String,
        @Path("albumId") albumId: String
    ) : Response<ApiError>
    @DELETE("artists/{artistId}/songs/{songId}")
    suspend fun deleteSongFromArtist(
        @Path("artistId") artistId: String,
        @Path("songId") songId: String
    ) : Response<ApiError>
    @GET("playlists")
    suspend fun getPlaylists(): Response<ApiResponse<List<Playlist>>>
    @GET("playlists/admin/searchKey")
    suspend fun searchPlaylistsForAdmin(
        @Query("name") name: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<PageResponse<Playlist>>>
    @GET("playlists/genre/{genreId}")
    suspend fun getPlaylistsByGenre(
        @Path("genreId") genreId: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 10
    ): Response<ApiResponse<PageResponse<Playlist>>>
    @GET("playlists/{playlistId}")
    suspend fun getPlaylistById(@Path("playlistId") playlistId: String): Response<ApiResponse<Playlist>>
    @PUT("playlists/{id}")
    suspend fun updatePlaylist(
        @Path("id") id: String,
        @Body request: PlaylistUpdateRequest
    ): Response<ApiResponse<Playlist>>
    @GET("playlists/myList")
    suspend fun getMyPlaylists(): Response<ApiResponse<List<Playlist>>>
    @GET("playlists/{playlistId}/songs")
    suspend fun getSongsInPlaylist(
        @Path("playlistId") playlistId: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 10
    ): Response<ApiResponse<PageResponse<Song>>>
    @POST("favorites/{songId}")
    suspend fun addSongToFavorite(@Path("songId") songId: String): Response<ApiError>
    @DELETE("favorites/{songId}")
    suspend fun deleteSongFromFavorite(@Path("songId") songId: String): Response<ApiError>
    @GET("favorites/my-favorites")
    suspend fun getFavoriteSongs(): Response<ApiResponse<List<Song>>>
    @GET("followers/artists/{artistId}/count")
    suspend fun getFollowerCount(@Path("artistId") artistId: String): Response<ApiResponse<Int>>
    @POST("followers/{artistId}")
    suspend fun followArtist(@Path("artistId") artistId: String): Response<ApiResponse<String>>
    @DELETE("followers/{artistId}")
    suspend fun unfollowArtist(@Path("artistId") artistId: String) : Response<ApiResponse<String>>
    @GET("followers/my-artists")
    suspend fun getMyFollowers(): Response<ApiResponse<List<Artist>>>
    @POST("playlists")
    suspend fun createPlaylist(@Body request: PlaylistCreateRequest): Response<ApiResponse<Playlist>>
    @DELETE("playlists/{id}")
    suspend fun deletePlaylist(@Path("id") id: String): Response<ApiError>
    @Multipart
    @POST("playlists/{playlistId}/upload")
    suspend fun uploadPlaylistImage(
        @Path("playlistId") playlistId: String,
        @Part image: MultipartBody.Part
    ): Response<ApiResponse<Playlist>>
    @POST("playlists/{playlistId}/songs/{songId}")
    suspend fun addSongToPlaylist(
        @Path("playlistId") playlistId: String,
        @Path("songId") songId: String
    ): Response<ApiResponse<Playlist>>
    @DELETE("playlists/{playlistId}/songs/{songId}")
    suspend fun deleteSongFromPlaylist(
        @Path("playlistId") playlistId: String,
        @Path("songId") songId: String
    ): Response<ApiResponse<Playlist>>
    @Multipart
    @POST("albums/{albumId}/upload")
    suspend fun uploadAlbumImage(
        @Path("albumId") albumId: String,
        @Part image: MultipartBody.Part
    ): Response<ApiResponse<Album>>
    @Multipart
    @POST("artists/{artistId}/upload")
    suspend fun uploadArtistImage(
        @Path("artistId") artistId: String,
        @Part image: MultipartBody.Part
    ): Response<ApiResponse<Artist>>
    @GET("comments/song/{songId}")
    suspend fun getComments(
        @Path("songId") songId: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 10
    ): Response<ApiResponse<PageResponse<Comment>>>
    @POST("comments/song/{songId}/comments")
    suspend fun createComment(
        @Path("songId") songId: String,
        @Body request: CommentCreationRequest
    ): Response<ApiResponse<Comment>>
    @DELETE("comments/{commentId}")
    suspend fun deleteComment(
        @Path("commentId") commentId: String
    ): Response<ApiResponse<String>>
    @PUT("comments/{commentId}")
    suspend fun updateComment(
        @Path("commentId") commentId: String,
        @Body request: CommentUpdateRequest
    ): Response<ApiResponse<Comment>>
    @GET("reports")
    suspend fun getReport(): Response<ApiResponse<List<Report>>>
    @POST("reports")
    suspend fun createReport(
        @Body request: ReportCreationRequest
    ): Response<ApiResponse<Report>>
    @PUT("reports/{reportId}/status")
    suspend fun updateReport(
        @Path("reportId") reportId: String,
        @Body request: ReportUpdateRequest
    ): Response<ApiResponse<Report>>
    @DELETE("reports/{reportId}")
    suspend fun deleteReport(
        @Path("reportId") reportId: String
    ): Response<ApiResponse<String>>
    @Streaming
    @GET("songs/{songId}/download")
    suspend fun downloadSong(@Path("songId") id: String): Response<ResponseBody>

    @GET("songs/{songId}/check-downloaded")
    suspend fun checkDownloaded(@Path("songId") id: String): Response<ApiResponse<Boolean>>

    @POST("songs/{songId}/play")
    suspend fun incrementPlayCount(@Path("songId") songId: String): Response<ApiResponse<String>>

    @GET("genres")
    suspend fun getGenres(): Response<ApiResponse<List<com.example.app.model.response.Genre>>>

    // ===== Recommendation APIs =====
    @GET("api/recommendations")
    suspend fun getRecommendations(
        @Query("userId") userId: String,
        @Query("limit") limit: Int = 10
    ): Response<ApiResponse<com.example.app.model.response.RecommendationResponse>>

    @GET("api/recommendations/home")
    suspend fun getHomeRecommendations(
        @Query("userId") userId: String
    ): Response<ApiResponse<HomeRecommendationResponse>>

    @POST("api/admin/recommendations/trigger-full-pipeline")
    suspend fun triggerFullPipeline(): Response<ApiResponse<String>>

    @POST("api/admin/recommendations/trigger-aggregation")
    suspend fun triggerAggregation(): Response<ApiResponse<String>>
}