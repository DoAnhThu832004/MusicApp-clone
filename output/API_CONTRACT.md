# 04. HỢP ĐỒNG API & TÍCH HỢP CLIENT-SERVER (API_CONTRACT.md)

> **Phân loại**: Báo cáo giao thức kết nối & Hợp đồng API (API Contract Specification)  
> **Vai trò**: Senior Software Engineer & Solution Architect  
> **Trạng thái**: [CONFIRMED] Đối soát 1:1 giữa `ApiService.kt` (Android) và các `@RestController` (Spring Boot).

---

## 1. Chuẩn Định Dạng Phản Hồi (API Response Envelope)

Tất cả các API trong hệ thống đều tuân thủ cấu trúc đóng gói chuẩn `ApiResponse<T>`:

```json
{
  "code": 1000,
  "message": "Thông điệp phản hồi nghiệp vụ",
  "result": {
    /* Dữ liệu thực tế tùy thuộc vào từng API */
  }
}
```

*Trong đó*:
- `code = 1000`: Thành công (Success).
- `code != 1000`: Thất bại; mã lỗi được định nghĩa trong `ErrorCode.java`.

---

## 2. Bảng Mã Lỗi Hệ Thống (Error Codes Matrix) [CONFIRMED]

*Trích xuất từ `D:\SpringBoot\identity-service-main\identity-service-main\src\main\java\com\devteria\identityservice\exception\ErrorCode.java`:*

| Code | Tên Enum | HTTP Status | Thông điệp mặc định | Ghi chú bất thường phát hiện |
| :--- | :--- | :--- | :--- | :--- |
| **1000** | `SUCCESS` | `200 OK` | Thành công | Mặc định |
| **1001** | `INVALID_KEY` | `400 Bad Request` | Uncategorized error | Lỗi validate key không hợp lệ |
| **1002** | `USER_EXISTED` | `400 Bad Request` | User existed | Trùng username khi đăng ký |
| **1003** | `USERNAME_INVALID` | `400 Bad Request` | Username must be at least {min} characters | Kiểm tra độ dài username |
| **1004** | `INVALID_PASSWORD` | `400 Bad Request` | Password must be at least {min} characters | Kiểm tra độ dài mật khẩu |
| **1005** | `USER_NOT_EXISTED` | `404 Not Found` | User not existed | Không tìm thấy người dùng |
| **1006** | `UNAUTHENTICATED` | `401 Unauthorized` | Unauthenticated | Chưa đăng nhập hoặc token hết hạn |
| **1007** | `UNAUTHORIZED` | `403 Forbidden` | You do not have permission | Không có quyền (Role không đủ) |
| **1008** | `INVALID_DOB` | `400 Bad Request` | Your age must be at least {min} | Kiểm tra tuổi tối thiểu |
| **1009** | `WRONG_PASSWORD` | `400 Bad Request` | Old password is incorrect | Đổi mật khẩu: sai pass cũ |
| **1010** | `PASSWORD_MISMATCH` | `400 Bad Request` | New password and confirm password do not match | Mật khẩu xác nhận không khớp |
| **1011** | `USER_BLOCKED` | `403 Forbidden` | Your account has been blocked by admin | Tài khoản đã bị Admin khoá |
| **2000** | `PLAYLIST_NOT_EXISTED` | `404 Not Found` | Playlist not existed | ⚠️ **Trùng mã code 2000** |
| **2000** | `ALBUM_NOT_EXISTED` | `404 Not Found` | Album not existed | ⚠️ **Trùng mã code 2000** |
| **2000** | `ARTIST_NOT_EXISTED` | `404 Not Found` | Artist not existed | ⚠️ **Trùng mã code 2000** |
| **2005** | `SONG_NOT_EXISTED` | `404 Not Found` | User not existed | ⚠️ **Bug chuỗi thông điệp**: Bài hát không tồn tại nhưng message ghi "User not existed" |
| **2006** | `GENRE_NOT_EXISTED` | `404 Not Found` | Genre not existed | Thể loại không tồn tại |
| **2007** | `UPLOAD_FAILED` | `400 Bad Request` | Failed to upload files to Cloudinary | Lỗi upload file |
| **9999** | `UNCATEGORIZED_EXCEPTION`| `500 Server Error` | Uncategorized error | Lỗi hệ thống không xác định |

---

## 3. Danh Mục REST Endpoints Chi Tiết

### A. Authentication & User Management
*Context-Path: `/identity`*

| Method | Endpoint | Yêu cầu Auth | Request DTO | Response DTO | Hàm trên Android (`ApiService.kt`) |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `POST` | `/auth/token` | Public | `AuthenticationRequest` | `AuthenticationResponse` | `authenticate()` |
| `POST` | `/auth/refresh` | Public | `RefreshRequest` | `AuthenticationResponse` | `refreshToken()` |
| `POST` | `/auth/logout` | Public | `LogoutRequest` | `ApiError` | `logout()` |
| `POST` | `/users` | Public | `UserCreationRequest` | `UserResponse` | `createUser()` |
| `GET` | `/users/my-info` | Bearer Token | Không | `UserResponse` | `getUserInfo()` |
| `PUT` | `/users/change-password` | Bearer Token | `ChangePasswordRequest` | `ApiResponse<String>` | `changePassword()` |
| `PUT` | `/users/{id}` | Bearer Token | `UserUpdateRequest` | `UserResponse` | `updateUser()` |
| `POST` | `/users/{id}/upload` | Bearer Token | `Multipart (image)` | `UserResponse` | `uploadUserImage()` |
| `GET` | `/users` | Admin Role | `page`, `size` | `ApiResponse<PageResponse<User>>` | `getUsers()` |
| `PATCH`| `/users/{id}/block` | Admin Role | Không | `UserResponse` | `blockUser()` |
| `PATCH`| `/users/{id}/unblock` | Admin Role | Không | `UserResponse` | `unblockUser()` |

---

### B. Music Catalog (Songs, Albums, Artists, Playlists)

| Method | Endpoint | Yêu cầu Auth | Request DTO / Params | Response DTO | Hàm trên Android (`ApiService.kt`) |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `GET` | `/songs` | User/Admin | `page`, `size`, `genreId` | `ApiResponse<PageResponse<Song>>` | `getSongs()` |
| `GET` | `/songs/top-charts` | User | Không | `ApiResponse<List<Song>>` | `getTopSongs()` |
| `GET` | `/songs/searchKey` | User | `name` | `ApiResponse<List<Song>>` | `searchSongs()` |
| `GET` | `/songs/recently-played` | Bearer Token | Không | `ApiResponse<List<Song>>` | `getRecentlyPlayedSongs()` |
| `POST` | `/songs` | Admin Role | `SongCreationRequest` | `ApiResponse<Song>` | `createSong()` |
| `POST` | `/songs/{id}/upload` | Admin Role | `Multipart (image, audio)` | `ApiResponse<Song>` | `uploadSongFiles()` |
| `PUT` | `/songs/{id}` | Admin Role | `SongUpdateRequest` | `ApiResponse<Song>` | `updateSong()` |
| `DELETE`| `/songs/{id}` | Admin Role | Không | `ApiError` | `deleteSong()` |
| `POST` | `/songs/{id}/play` | User | Không | `ApiResponse<String>` | `incrementPlayCount()` |
| `GET` | `/songs/{id}/download` | Bearer Token | Không | `ResponseBody (Binary Stream)` | `downloadSong()` |
| `GET` | `/albums` | User | `page`, `size` | `ApiResponse<PageResponse<Album>>` | `getAlbums()` |
| `GET` | `/albums/{id}` | User | Không | `ApiResponse<Album>` | `getAlbumById()` |
| `GET` | `/artists` | User | Không | `ApiResponse<List<Artist>>` | `getArtists()` |
| `GET` | `/playlists/myList` | Bearer Token | Không | `ApiResponse<List<Playlist>>` | `getMyPlaylists()` |
| `POST` | `/playlists` | Bearer Token | `PlaylistCreateRequest` | `ApiResponse<Playlist>` | `createPlaylist()` |

---

### C. Recommendation Engine APIs

| Method | Endpoint | Yêu cầu Auth | Query Params | Response DTO | Ý nghĩa nghiệp vụ |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `GET` | `/api/recommendations` | User | `userId`, `limit` | `ApiResponse<RecommendationResponse>` | Gợi ý danh sách bài hát cá nhân |
| `GET` | `/api/recommendations/home` | User | `userId` | `ApiResponse<HomeRecommendationResponse>` | Dữ liệu gợi ý tổng hợp cho trang chủ |
| `POST` | `/api/admin/recommendations/trigger-full-pipeline` | Admin | Không | `ApiResponse<String>` | Kích hoạt tính toán ma trận gợi ý |
| `POST` | `/api/admin/recommendations/trigger-aggregation` | Admin | Không | `ApiResponse<String>` | Kích hoạt tổng hợp điểm tương tác |

---

## 4. Các Điểm Bất Khớp & Rủi Ro Hợp Đồng Phát Hiện (Contract Discrepancies) [CONFIRMED]

1. **Sai lệch tên tham số URL trong `ApiService.kt`**:
   - `ApiService.kt#L302`: `@DELETE("comments/{songId}") suspend fun deleteComment(@Path("songId") songId: String)`
   - Trong khi `CommentController.java#L51` khai báo: `@DeleteMapping("/{commentId}") public ApiResponse<String> deleteComment(@PathVariable("commentId") String commentId)`
   - Mặc dù HTTP path hoạt động (do Retrofit thế giá trị vào `{songId}` trên URL), việc đặt tên biến `songId` thay vì `commentId` gây nhầm lẫn nghiêm trọng cho lập trình viên bảo trì.
2. **Không nhất quán trong kiểu dữ liệu đóng gói Response**:
   - Một số API trả về `Response<ApiResponse<T>>`, trong khi một số khác trả về `Response<ApiError>` (ví dụ: `logout`, `deleteSong`, `addSongToFavorite`) hoặc `Response<UserResponse>` không có vỏ bọc `ApiResponse`. Điều này khiến code tầng Repository trên Android phải viết nhiều nhánh parse response khác nhau.
