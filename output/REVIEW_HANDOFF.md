# 09. TÀI LIỆU BÀN GIAO THẨM ĐỊNH KIẾN TRÚC (REVIEW_HANDOFF.md)

> **Dành cho**: AI Reviewer / Solution Architect tiếp quản thẩm tra hệ thống  
> **Người lập**: Senior Software Engineer & Solution Architect  
> **Mục đích**: Cung cấp bức tranh toàn cảnh, cô đọng và có thể kiểm chứng ngay lập tức về toàn bộ hệ thống mà không cần đọc lại hàng nghìn file code.

---

## 1. Bản Chất Dự Án & Bài Toán Cốt Lõi
Hệ thống là một **Nền tảng Phát nhạc Trực tuyến & Gợi ý Âm nhạc Cá nhân hóa (Music Streaming & Recommendation Platform)** hoàn chỉnh gồm 2 repository:
- **Mobile Client (Front-end)**: `c:\Users\ASUS\AndroidStudioProjects\App`
- **REST API Server (Back-end)**: `D:\SpringBoot\identity-service-main\identity-service-main`

Ứng dụng giải quyết 2 bài toán lớn:
1. **Trải nghiệm nghe nhạc trực tuyến & ngoại tuyến**: Phát nhạc mượt mà qua ExoPlayer nền (Foreground Service) kết hợp tải nhạc về máy nghe offline qua Room DB.
2. **Cá nhân hóa trải nghiệm âm nhạc**: Kết hợp giải thuật **Item-Based Collaborative Filtering**, **Cosine Similarity** và **MMR Re-ranking** với cơ chế giải quyết bài toán người dùng mới (**Cold Start**) bằng cách khảo sát thể loại nhạc yêu thích ngay khi đăng ký tài khoản.

---

## 2. Tech Stack & Kiến Trúc Tổng Thể

```
[Android Jetpack Compose UI] ──(StateFlow)──> [ViewModels / PlayerManager]
               │                                      │
               ▼                                      ▼
     [Room DB (Offline)]                  [Retrofit 2 / OkHttp 3]
                                                      │
                       (Bearer JWT / HTTP REST / JSON)│
                                                      ▼
                           [Spring Boot 3.2.2 Gateway /identity]
                                         │
                    ┌────────────────────┼────────────────────┐
                    ▼                    ▼                    ▼
           [Spring Security]   [Recommendation Engine]   [Cloudinary CDN]
                    │                    │                    │
                    ▼                    ▼                    ▼
              [JPA Repos]     [In-Memory Matrix / Cosine]  (MP3 & Covers)
                    │                    │
                    └──────────┬─────────┘
                               ▼
                       [MySQL 8.x DB]
```

- **Front-end**: Kotlin 2.0.0, Jetpack Compose Material 3, AndroidX Media3 / ExoPlayer 1.3.1, Room 2.6.1, Retrofit 2.9.0, Firebase BoM 33.9.0, AdMob.
- **Back-end**: Java 21, Spring Boot 3.2.2, Spring Security OAuth2 Resource Server, Nimbus JWT, Spring Data JPA / Hibernate, MySQL 8.x, Cloudinary SDK 1.36.0, MapStruct 1.6.3, Lombok.

---

## 3. Các Module & Nghiệp Vụ Cốt Lõi
1. **Authentication & Identity**: Đăng nhập, đăng ký 2 bước (bước 1: thông tin cá nhân; bước 2: chọn `preferredGenres`), làm mới token (`/auth/refresh`), thu hồi token đã logout vào bảng blacklist (`InvalidatedToken`).
2. **Catalog Management**: Quản trị bài hát, album, nghệ sĩ, thể loại, danh sách phát. Tải trực tiếp file MP3 và ảnh bìa lên Cloudinary.
3. **Media Playback Engine**: Trình phát nhạc nền chạy qua `MusicPlayerService`, hiển thị notification điều khiển media chuẩn Android 13+, Mini Player cố định ở đáy màn hình.
4. **Offline Mode**: Tải stream nhị phân bài hát về bộ nhớ thiết bị (`filesDir/music/`), lưu thông tin vào Room DB với khóa chính kết hợp `(id, userId)` để phát lại khi mất mạng hoàn toàn.
5. **Recommendation Pipeline**: Gợi ý nhạc 3 tầng: `PERSONALIZED` (lọc cộng tác dựa trên tương tác), `COLD_START_GENRE` (thể loại đã chọn khi đăng ký), `COLD_START_GLOBAL` (bài hát nghe nhiều nhất hệ thống).

---

## 4. Ánh Xạ API Front-end ↔ Back-end (Core Mapping)

| Nghiệp vụ chính | Android Interface (`ApiService.kt`) | Spring Boot Controller | Đường dẫn HTTP |
| :--- | :--- | :--- | :--- |
| **Đăng nhập** | `authenticate()` | `AuthenticationController` | `POST /identity/auth/token` |
| **Đăng ký Onboarding**| `createUser()` | `UserController` | `POST /identity/users` |
| **Refresh Token** | `refreshToken()` | `AuthenticationController` | `POST /identity/auth/refresh` |
| **Gợi ý trang chủ** | `getHomeRecommendations()` | `RecommendationController` | `GET /identity/api/recommendations/home` |
| **Ghi nhận lượt nghe** | `recordListen()` | `InteractionController` | `POST /identity/interactions/listen/{songId}` |
| **Tăng Play Count** | `incrementPlayCount()` | `SongController` | `POST /identity/songs/{songId}/play` |
| **Tải file Offline** | `downloadSong()` | `SongController` | `GET /identity/songs/{songId}/download` |
| **Upload File Nhạc** | `uploadSongFiles()` | `SongController` | `POST /identity/songs/{id}/upload` |

---

## 5. Danh Sách Các Vấn Đề Đã Phát Hiện (Xếp Theo Mức Độ Ưu Tiên)

### 🔴 CRITICAL (Cần xử lý ngay lập tức trước khi deploy)
1. **Lỗ hổng leo thang đặc quyền (Privilege Escalation)**:
   - *Bằng chứng*: `UserService.java#L110-L116` & `UserController.java#L70`.
   - *Mô tả*: `updateUser()` cho phép cập nhật trực tiếp `request.getRoles()` mà không kiểm tra quyền `ADMIN`. Bất kỳ user thông thường nào cũng có thể tự thăng cấp lên `ADMIN`.
2. **Hardcoded Secrets & API Key trong Source Code**:
   - *Bằng chứng*: `CloudinaryConfig.java#L12-L17` & `application.yaml#L21`.
   - *Mô tả*: API Key, Secret của Cloudinary và Khóa ký JWT (`signerKey`) bị hardcode trực tiếp trong mã nguồn đã commit vào Git.

### 🟠 HIGH (Rủi ro an ninh & toàn vẹn dữ liệu cao)
3. **Lỗ hổng IDOR trên API Gợi ý và Upload ảnh cá nhân**:
   - *Bằng chứng*: `RecommendationController.java#L24` (`@RequestParam("userId") String userId`) và `UserController.java#L78`.
   - *Mô tả*: Người dùng A có thể xem trộm danh sách gợi ý hoặc can thiệp thông tin của người dùng B chỉ bằng cách truyền `userId` khác trên URL mà không bị chặn.
4. **Cấu hình Wildcard CORS**:
   - *Bằng chứng*: `SecurityConfig.java#L55-L57`.
   - *Mô tả*: Cho phép tất cả các nguồn truy cập không giới hạn (`*`).
5. **Thiếu công cụ quản lý Database Migration (Flyway/Liquibase)**:
   - *Bằng chứng*: `application.yaml#L14` sử dụng `hibernate.ddl-auto: update`. Nguy cơ xung đột schema và lỗi dữ liệu trên Production.

### 🟡 MEDIUM (Chất lượng code & Rủi ro tiềm ẩn)
6. **Destructive Migration trên Room DB**:
   - *Bằng chứng*: `AppDatabase.kt#L23` dùng `fallbackToDestructiveMigration()`. Nâng version Room sẽ xóa sạch nhạc đã tải về của người dùng.
7. **Bất nhất và lỗi thông điệp trong `ErrorCode.java`**:
   - *Bằng chứng*: `ErrorCode.java#L19` (`SONG_NOT_EXISTED` trả về message "User not existed") và dùng trùng mã `2000` cho Playlist, Album, Artist.
8. **Sai lệch tên tham số URL trong `ApiService.kt#L302`**:
   - Dùng `@DELETE("comments/{songId}")` thay vì `commentId`.
9. **Độ bao phủ kiểm thử tự động (Test Coverage) gần như bằng 0**:
   - Chỉ có 3 file test trên backend và 2 file test trên frontend. Toàn bộ logic gợi ý, streaming và repository đều chưa được kiểm thử tự động.

---

## 6. Những Điểm Chưa Thể Xác Minh (Unknowns & Data Gaps)
1. **Cấu hình Production Environment**: File `application-prod.yaml` có kích thước rất nhỏ (83 bytes), chưa xác minh được thông tin triển khai cụ thể trên Cloud (AWS/GCP/Docker) ngoài việc có sẵn một file `Dockerfile` mẫu.
2. **Hành vi chạy tải cao của Recommendation Engine**: Chưa có dữ liệu thực nghiệm về mức tiêu thụ CPU/RAM khi kho nhạc đạt quy mô trên 10.000 bài hát với thuật toán tính toán ma trận in-memory hiện tại.

---

## 7. Danh Sách File Cần Đọc Tiếp Để Thẩm Tra Sâu
Để kiểm chứng các phát hiện trên, AI reviewer tiếp theo chỉ cần tập trung đọc các file sau:
1. `D:\SpringBoot\identity-service-main\identity-service-main\src\main\java\com\devteria\identityservice\service\UserService.java#L93-L128` (Kiểm chứng lỗ hổng Privilege Escalation).
2. `D:\SpringBoot\identity-service-main\identity-service-main\src\main\java\com\devteria\identityservice\configuration\CloudinaryConfig.java` (Kiểm chứng Secret hardcoded).
3. `D:\SpringBoot\identity-service-main\identity-service-main\src\main\java\com\devteria\identityservice\controller\RecommendationController.java` (Kiểm chứng IDOR).
4. `D:\SpringBoot\identity-service-main\identity-service-main\src\main\java\com\devteria\identityservice\service\RecommendationEngineService.java` (Đánh giá thuật toán CF & MMR).
5. `c:\Users\ASUS\AndroidStudioProjects\App\app\src\main\java\com\example\app\model\ApiClient.kt` & `ApiService.kt` (Kiểm chứng contract và Refresh Token).
6. `c:\Users\ASUS\AndroidStudioProjects\App\app\src\main\java\com\example\app\viewmodel\PlayerManager.kt` & `MusicPlayerService.kt` (Kiểm chứng vòng đời ExoPlayer).
