# 01. TỔNG QUAN DỰ ÁN (PROJECT_OVERVIEW.md)

> **Phân loại**: Báo cáo kiến trúc hệ thống phục vụ chuyển giao và đánh giá kỹ thuật  
> **Vai trò**: Senior Software Engineer & Solution Architect  
> **Ngày lập**: 03/11/2026  
> **Trạng thái**: [CONFIRMED] Dựa trên phân tích mã nguồn thực tế tại 2 repository Frontend và Backend.

---

## 1. Mục Tiêu Sản Phẩm & Bài Toán Nghiệp Vụ

Dự án là một **Nền tảng Phát nhạc Trực tuyến & Gợi ý Âm nhạc Cá nhân hóa (Music Streaming Platform & Intelligent Recommendation)** hoạt động trên mô hình Client-Server gồm Ứng dụng Di động (Mobile App) và Hệ thống Máy chủ (Backend REST API).

### Đối tượng sử dụng & Vai trò (User Roles) [CONFIRMED]
Dựa trên mã nguồn tại `PredefinedRole.java` (`D:\SpringBoot\identity-service-main\identity-service-main\src\main\java\com\devteria\identityservice\constant\PredefinedRole.java#L5-L6`):
1. **End-User (Role: `USER`)**:
   - Đăng ký tài khoản kèm chọn trước các thể loại yêu thích (`preferredGenres`) để giải quyết bài toán Cold Start.
   - Nghe nhạc trực tuyến (Streaming) và ngoại tuyến (Offline download).
   - Quản lý Playlist cá nhân, danh sách bài hát yêu thích (Favorites), theo dõi nghệ sĩ (Follow Artists).
   - Tương tác xã hội: bình luận bài hát (Comments), gửi báo cáo vi phạm nội dung (Reports).
   - Trải nghiệm giao diện cá nhân hóa: Nhận gợi ý danh sách bài hát dựa trên học máy (Collaborative Filtering & MMR Re-ranking), đổi theme động/theo mùa (Dynamic & Seasonal Themes).
2. **Quản trị viên (Role: `ADMIN`)**:
   - Quản lý danh mục âm nhạc toàn diện: Thêm, sửa, xóa, tải lên file âm thanh (MP3) và hình ảnh bìa cho Bài hát (Songs), Album, Nghệ sĩ (Artists), Thể loại (Genres), Playlist hệ thống.
   - Quản lý người dùng: Xem danh sách phân trang, phân quyền vai trò (Role), khoá/mở khoá tài khoản người dùng (`block/unblock`).
   - Kiểm duyệt nội dung: Tiếp nhận và xử lý các báo cáo vi phạm từ người dùng (`PENDING`, `RESOLVED`, `REJECTED`).
   - Kích hoạt thủ công đường ống tính toán ma trận gợi ý âm nhạc (`RecommendationTriggerController`).

---

## 2. Công Nghệ, Framework & Phiên Bản Thực Tế

### A. Front-end (Mobile Android Client) [CONFIRMED]
*Được xác minh từ file `app/build.gradle.kts` và `gradle/libs.versions.toml` tại `c:\Users\ASUS\AndroidStudioProjects\App`:*

| Thành phần | Công nghệ / Thư viện | Phiên bản thực tế | Ghi chú kiểm chứng |
| :--- | :--- | :--- | :--- |
| **Nền tảng & Ngôn ngữ** | Android OS, Kotlin | Kotlin `2.0.0`, JVM 1.8/Java 17 | `app/build.gradle.kts#L9-L13` |
| **SDK Levels** | Min SDK: 26, Target/Compile SDK: 35 | Android 8.0 -> Android 15 | `app/build.gradle.kts#L15-L18` |
| **UI Framework** | Jetpack Compose | Compose BoM `2024.04.01`, Material 3 | Hoàn toàn Declarative UI |
| **Media Player** | AndroidX Media3 / ExoPlayer | `1.3.1` (ExoPlayer `2.19.1`) | `MusicPlayerService.kt` |
| **Network & REST API** | Retrofit 2 + OkHttp 3 | Retrofit `2.9.0`, OkHttp `4.12.0` | `ApiClient.kt` |
| **Cơ sở dữ liệu cục bộ**| Room Database | `2.6.1` (KSP) | Lưu trữ nhạc ngoại tuyến |
| **Lưu trữ Key-Value** | Jetpack DataStore Preferences | `1.1.1` | Lưu token, theme state |
| **Image Loading** | Coil Compose | `2.4.0` | Tải ảnh từ Cloudinary/URL |
| **Phân trang** | Jetpack Paging 3 Compose | `3.2.1` | Paging Song, Album |
| **Quảng cáo** | Google Mobile Ads (AdMob) | `23.3.0` | Banner & Interstitial ads |
| **Cloud & Analytics** | Firebase BoM (Analytics, Crashlytics, Remote Config) | `33.9.0` | Dynamic Seasonal Themes |

### B. Back-end (REST API Service) [CONFIRMED]
*Được xác minh từ file `pom.xml` tại `D:\SpringBoot\identity-service-main\identity-service-main\pom.xml#L1-L40`:*

| Thành phần | Công nghệ / Thư viện | Phiên bản thực tế | Ghi chú kiểm chứng |
| :--- | :--- | :--- | :--- |
| **Nền tảng & Ngôn ngữ** | Java | `Java 21` | `pom.xml#L17` |
| **Core Framework** | Spring Boot | `3.2.2` | `pom.xml#L8` |
| **Security & Auth** | Spring Security, OAuth2 Resource Server | `3.2.2` | Stateless JWT, Nimbus JOSE |
| **Database & ORM** | MySQL 8.x, Spring Data JPA / Hibernate | Hibernate 6.x | `application.yaml#L8-L15` |
| **File Storage** | Cloudinary Java SDK | `1.36.0` (`cloudinary-http44`) | `CloudinaryConfig.java` |
| **Recommendation Engine**| In-memory Item-Based CF + MMR | Tự xây dựng (Custom Algorithm) | `RecommendationEngineService.java` |
| **Boilerplate & Mapper** | Project Lombok `1.18.36`, MapStruct `1.6.3` | Java Annotation Processors | `pom.xml#L18-L20` |
| **Validation** | Hibernate Validator (`jakarta.validation`) | Chuẩn Jakarta EE 10 | `DobValidator.java`, DTOs |

---

## 3. Cấu Trúc Repository & Điểm Khởi Chạy (Entry Points)

### Front-end: `c:\Users\ASUS\AndroidStudioProjects\App`
- **Loại hình**: Single Android Application Project (Gradle Kotlin DSL).
- **Điểm khởi chạy ứng dụng (Entry Point)** [CONFIRMED]:
  - `app/src/main/java/com/example/app/MainActivity.kt#L45`: Khởi tạo Activity chính, kiểm tra Remote Config, Locale và render `RecipeApp()`.
  - `app/src/main/java/com/example/app/view/RecipeApp.kt#L98`: Composable chứa toàn bộ `NavHost`, điều phối routing từ Splash/Login đến User/Admin screens.
  - `app/src/main/java/com/example/app/model/MusicPlayerService.kt#L30`: Android Foreground Service quản lý vòng đời ExoPlayer và Media Notification.

### Back-end: `D:\SpringBoot\identity-service-main\identity-service-main`
- **Loại hình**: Single-module Spring Boot Maven Project.
- **Điểm khởi chạy ứng dụng (Entry Point)** [CONFIRMED]:
  - `src/main/java/com/devteria/identityservice/IdentityServiceApplication.java#L10`: Chứa hàm `main(String[] args)` khởi chạy Spring Boot Container trên cổng `8080` với context-path `/identity`.
  - `src/main/java/com/devteria/identityservice/configuration/ApplicationInitConfig.java#L28`: Runner khởi tạo tài khoản `admin` mặc định khi database rỗng.

---

## 4. Trạng Thái Phạm Vi Tính Năng (Implementation Status)

| Phạm vi tính năng | Trạng thái | Đánh giá chi tiết từ mã nguồn |
| :--- | :---: | :--- |
| **Xác thực JWT & Đăng ký Cold Start** | `Đã hoàn thành` [CONFIRMED] | Login, Onboarding chọn thể loại, Auto Refresh Token qua Interceptor hoạt động tốt. |
| **Phát nhạc (Streaming + ExoPlayer)** | `Đã hoàn thành` [CONFIRMED] | Foreground Service, MiniPlayer, Notification Media3, PlayerScreen đầy đủ. |
| **Nghe nhạc ngoại tuyến (Offline Mode)** | `Đã hoàn thành` [CONFIRMED] | Tải file audio/image về bộ nhớ máy, lưu metadata vào Room DB (`DownloadedSongEntity`). |
| **Quản trị danh mục (Admin Mode)** | `Đã hoàn thành` [CONFIRMED] | Quản lý Song, Album, Artist, Playlist, tải file đa phương tiện lên Cloudinary. |
| **Gợi ý nhạc thông minh (Recommendation)**| `Đã hoàn thành` [CONFIRMED] | 3 nguồn gợi ý: `PERSONALIZED`, `COLD_START_GENRE`, `COLD_START_GLOBAL` với MMR Re-ranking. |
| **Tương tác xã hội (Like, Follow, Comment)**| `Đã hoàn thành` [CONFIRMED] | Thả tim (Favorites), bình luận phân trang, theo dõi nghệ sĩ. |
| **Dynamic Themes (Mùa lễ hội)** | `Đã hoàn thành` [CONFIRMED] | Firebase Remote Config điều khiển Halloween/Tet theme. |
| **Tích hợp Generative AI (LLM / RAG / Agent)** | `Chưa triển khai` [CONFIRMED] | Chưa có module AI/LLM nào trong cả 2 repository; chỉ mới có thuật toán Recommendation giải tích truyền thống. |
| **Automated Testing & CI/CD** | `Thiếu hụt nghiêm trọng` [CONFIRMED] | Chỉ có 2-3 file test mẫu; chưa có CI/CD pipeline tự động. |
