# 🎵 Music Streaming & Recommendation Android App

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?logo=android" alt="Android Platform" />
  <img src="https://img.shields.io/badge/Kotlin-2.0.0-purple?logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-blue?logo=jetpackcompose" alt="Compose" />
  <img src="https://img.shields.io/badge/Media3-ExoPlayer_2.19.1-orange" alt="ExoPlayer" />
  <img src="https://img.shields.io/badge/Architecture-MVVM_%2B_Clean-brightgreen" alt="Architecture" />
  <img src="https://img.shields.io/badge/Min_SDK-26-yellow" alt="Min SDK" />
  <img src="https://img.shields.io/badge/Target_SDK-35-blue" alt="Target SDK" />
  <img src="https://img.shields.io/badge/Firebase-Analytics_%7C_Crashlytics_%7C_Remote_Config-FFCA28?logo=firebase" alt="Firebase" />
</p>

Ứng dụng nghe nhạc trực tuyến và ngoại tuyến (Offline) hiện đại trên nền tảng Android, xây dựng hoàn toàn bằng **Jetpack Compose**, tích hợp hệ thống **gợi ý nhạc thông minh (Recommendation System)** dựa trên Collaborative Filtering, kiến trúc **MVVM** và các dịch vụ đám mây của **Firebase**, **Google AdMob**.

---

## 📑 Mục Lục
- [Tổng Quan Dự Án](#-tổng-quan-dự-án)
- [Tính Năng Chính](#-tính-năng-chính)
  - [Dành cho Người Dùng (User Mode)](#1-dành-cho-người-dùng-user-mode)
  - [Dành cho Quản Trị Viên (Admin Mode)](#2-dành-cho-quản-trị-viên-admin-mode)
- [Hệ Thống Gợi Ý Nhạc (Recommendation Engine)](#-hệ-thống-gợi-ý-nhạc-recommendation-engine)
- [Kiến Trúc & Công Nghệ](#-kiến-trúc--công-nghệ)
- [Cấu Trúc Thư Mục](#-cấu-trúc-thư-mục)
- [Cài Đặt & Khởi Chạy](#-cài-đặt--khởi-chạy)
  - [Yêu cầu tiên quyết](#yêu-cầu-tiên-quyết)
  - [Cấu hình Backend API](#cấu-hình-backend-api)
  - [Cấu hình Firebase & AdMob](#cấu-hình-firebase--admob)
  - [Biên dịch & Khởi chạy](#biên-dịch--khởi-chạy)
- [Xử Lý Ngoại Tuyến (Offline Mode)](#-xử-lý-ngoại-tuyến-offline-mode)
- [Tác Giả & Bản Quyền](#-tác-giả--bản-quyền)

---

## 🌟 Tổng Quan Dự Án

Dự án là giải pháp toàn diện cho nền tảng âm nhạc trực tuyến kết hợp di động:
- **Người dùng**: Khám phá các bài hát, nghệ sĩ, album, danh sách phát được cá nhân hoá theo sở thích; thưởng thức âm nhạc mượt mà dù đang online hay offline.
- **Hệ thống gợi ý**: Ứng dụng kỹ thuật gợi ý lọc cộng tác (Item-Based Collaborative Filtering) kết hợp xử lý bài toán **Cold Start** ngay từ luồng đăng ký ban đầu.
- **Quản trị viên**: Dashboard quản lý tập trung toàn bộ danh mục bài hát, album, nghệ sĩ, playlist, kiểm duyệt nội dung và quản lý tài khoản người dùng.

---

## 🚀 Tính Năng Chính

### 1. Dành cho Người Dùng (User Mode)
* **Xác thực & Cá nhân hoá khởi đầu**:
  * Đăng nhập bảo mật với JWT Token và cơ chế tự động làm mới token (`Token Authenticator`).
  * Đăng ký 2 bước (2-step onboarding): Nhập thông tin cá nhân và chọn các **Thể loại âm nhạc yêu thích** (Genre chips) để giải quyết bài toán Cold Start.
  * Quản lý thông tin cá nhân, chỉnh sửa avatar, đổi mật khẩu an toàn với xác thực đa tầng.
* **Trình phát nhạc mạnh mẽ (Music Player)**:
  * Phát trực tuyến từ backend hoặc phát ngoại tuyến từ bộ nhớ thiết bị.
  * Hỗ trợ đầy đủ các thao tác: Play/Pause, Next/Previous, Seek bar, Shuffle, Repeat.
  * **Mini Player** cố định dưới màn hình giúp người dùng tiếp tục nghe nhạc trong lúc duyệt ứng dụng.
  * Chạy nền thông qua **Foreground Service** (`MusicPlayerService`) với Notification điều khiển media đầy đủ chuẩn Android Media3.
* **Khám phá âm nhạc đa dạng**:
  * Xem danh sách bài hát phân trang mượt mà bằng **Paging 3**.
  * Duyệt bài hát theo thể loại (Genre), Album, Nghệ sĩ (Artist) và Bảng xếp hạng (Top Charts).
  * Tìm kiếm tức thời bài hát, nghệ sĩ, album và playlist.
  * Tương tác bình luận (Comments) và báo cáo vi phạm (Reports).
* **Danh sách phát & Yêu thích**:
  * Thả tim bài hát với cơ chế **Optimistic Update** (cập nhật UI tức thì, tự động đồng bộ API).
  * Tạo, chỉnh sửa và quản lý Playlist cá nhân.
  * Theo dõi các nghệ sĩ yêu thích.
* **Chế độ Ngoại tuyến (Offline Mode)**:
  * Tải bài hát về thiết bị lưu trữ cục bộ.
  * Lưu trữ thông tin bài hát tải xuống trong **Room Database**.
  * Phát nhạc offline hoàn toàn không cần kết nối mạng hoặc không cần đăng nhập lại.
* **Giao diện & Trải nghiệm người dùng**:
  * Giao diện Material 3 hiện đại, mượt mà với hoạt ảnh chuyển cảnh `AnimatedContent`.
  * Hỗ trợ chế độ Sáng / Tối (Light / Dark Mode).
  * **Chủ đề động & Chủ đề theo mùa (Dynamic Seasonal Theme)**: Tự động đổi giao diện theo mùa lễ hội (ví dụ: Halloween, Tết) qua Firebase Remote Config.
  * Hỗ trợ đa ngôn ngữ: **Tiếng Việt** và **Tiếng Anh**.

### 2. Dành cho Quản Trị Viên (Admin Mode)
* **Quản lý Bài Hát (Song Management)**: Xem danh sách, thêm bài hát mới, chỉnh sửa thông tin, tải lên file audio (MP3) và hình ảnh bìa.
* **Quản lý Album & Nghệ Sĩ**: Thêm/sửa/xoá album và nghệ sĩ, gán bài hát vào album, tải lên ảnh đại diện nghệ sĩ và ảnh bìa album.
* **Quản lý Playlist hệ thống**: Thiết lập và cập nhật các danh sách phát đặc sắc của hệ thống.
* **Quản lý Người Dùng**: Danh sách người dùng, phân quyền (Role), khoá / mở khoá tài khoản vi phạm.
* **Xử lý Báo cáo (Report Moderation)**: Tiếp nhận các phản hồi, báo cáo từ người dùng và xử lý trạng thái (*Chưa giải quyết*, *Đang chờ*, *Đã giải quyết*).

---

## 🧠 Hệ Thống Gợi Ý Nhạc (Recommendation Engine)

Màn hình trang chủ tích hợp mục **"Gợi ý cho bạn"** (`RecommendationSection`) sử dụng thuật toán **Item-Based Collaborative Filtering** từ Backend:

| Huy hiệu (Badge) | Nguồn gợi ý | Mô tả |
| :--- | :--- | :--- |
| 🎯 **Dành riêng cho bạn** | `PERSONALIZED` | Dựa trên lịch sử nghe nhạc, lượt thích và hành vi tương tác của người dùng. |
| 🎵 **Theo sở thích** | `COLD_START_GENRE` | Dành cho người dùng mới dựa trên các thể loại nhạc đã chọn khi đăng ký. |
| 🔥 **Đang thịnh hành** | `COLD_START_GLOBAL` | Dành cho khách vãng lai hoặc khi chưa đủ dữ liệu cá nhân hoá. |

> 💡 **Hiệu ứng Shimmer Skeleton**: Hiển thị khung chờ đẹp mắt trong lúc hệ thống tính toán và tải gợi ý từ API.

---

## 🛠️ Kiến Trúc & Công Nghệ

### 1. Kiến trúc phần mềm
* **MVVM (Model - View - ViewModel)**: Phân tách rõ ràng giữa tầng dữ liệu, logic nghiệp vụ và giao diện người dùng.
* **Clean Architecture** (đặc biệt trong module Theme): Chia thành các tầng `data`, `domain` (UseCases, Repositories), và `ui`.
* **StateFlow / SharedFlow**: Quản lý trạng thái UI phản ứng (Reactive UI) một chiều (Unidirectional Data Flow).

### 2. Ngăn xếp công nghệ (Tech Stack)

| Thành phần | Công nghệ / Thư viện | Phiên bản |
| :--- | :--- | :--- |
| **Ngôn ngữ** | Kotlin | 2.0.0 |
| **Giao diện (UI)** | Jetpack Compose (BOM 2024.04.01), Material 3 | 1.10.1 / 1.7.2 |
| **Media & Audio Engine**| AndroidX Media3 / ExoPlayer | 1.3.1 / 2.19.1 |
| **Cơ sở dữ liệu cục bộ**| Room Database | 2.6.1 |
| **Lưu trữ cấu hình** | Jetpack DataStore Preferences | 1.1.1 |
| **Mạng & REST API** | Retrofit 2 + OkHttp 3, Gson Converter | 2.9.0 |
| **Tải ảnh (Image Loading)**| Coil Compose | 2.4.0 |
| **Phân trang (Pagination)**| Jetpack Paging 3 Compose | 3.2.1 |
| **Bảo mật** | JWT Decode | 2.0.1 |
| **Quảng cáo** | Google Mobile Ads (AdMob) | 23.3.0 |
| **Cloud & Analytics** | Firebase BoM (Analytics, Crashlytics, Remote Config) | 33.9.0 |

---

## 📂 Cấu Trúc Thư Mục

```plaintext
app/src/main/java/com/example/app/
├── MainActivity.kt               # Entry point, quản lý Locale, Service & Dynamic Theme
├── MainViewModel.kt              # Quản lý Theme & Dynamic state toàn cục
├── admob/                        # Quản lý hiển thị Banner & Interstitial Ads
│   └── AdMobManager.kt
├── analytics/                    # Firebase Analytics, Crashlytics & Remote Config
│   ├── AnalyticsHelper.kt
│   ├── CrashlyticsHelper.kt
│   └── RemoteConfigManager.kt
├── model/                        # Tầng Dữ Liệu (Data Layer)
│   ├── ApiClient.kt              # Retrofit & OkHttp với Auto-Refresh Authenticator
│   ├── ApiService.kt             # Khai báo các RESTful API endpoints
│   ├── MusicPlayerService.kt     # Foreground Service chạy nhạc nền
│   ├── MusicPlayerReceiver.kt    # Nhận broadcast action từ thông báo phát nhạc
│   ├── paging/                   # PagingSource cho Song, Album
│   ├── repository/               # Repositories (Song, Album, Artist, Playlist, Download...)
│   ├── request/                  # DTOs gửi lên Server
│   ├── response/                 # DTOs nhận về từ Server
│   └── room/                     # Room DB: AppDatabase, SongDao, DownloadedSongEntity
├── theme/                        # Module Theme (Clean Architecture)
│   ├── data/                     # Datasource (Local / Remote Config), DTOs, Repository Impl
│   ├── domain/                   # Models, Repository interfaces, UseCases
│   ├── ui/                       # Theme Composables, HalloweenSeasonalScreen, Preview
│   └── viewmodel/                # DynamicThemeViewModel
├── view/                         # Tầng Giao Diện (Presentation Layer)
│   ├── RecipeApp.kt              # NavHost chính, điều hướng toàn bộ app
│   ├── Screen.kt                 # Khai báo Sealed Class các routes điều hướng
│   ├── admin/                    # Màn hình quản trị (Song, Album, Artist, Playlist, User, Report)
│   ├── ads/                      # UI Components cho quảng cáo
│   ├── Album/                    # Màn hình danh sách & chi tiết Album
│   ├── Artist/                   # Màn hình nghệ sĩ
│   ├── general/                  # UI dùng chung: SearchBar, ProgressBar, NoInternet, Onboarding...
│   ├── InProfile/                # Đổi mật khẩu, danh sách tải về, theo dõi nghệ sĩ
│   ├── Login/                    # Màn hình Đăng nhập & Đăng ký (2 bước)
│   ├── Player/                   # PlayerScreen, MiniPlayer, Comments BottomSheet
│   ├── Playlist/                 # Danh sách & chi tiết Playlist
│   ├── Song/                     # SongScreen, RecommendationSection, ListAllSong
│   └── user/                     # UserHomePage, ProfilePage, FavoritePage, SettingPage
└── viewmodel/                    # Tầng ViewModel & State Management
    ├── PlayerManager.kt          # Trình điều khiển phát nhạc ExoPlayer dùng chung
    ├── SongViewModel.kt
    ├── RecommendationViewModel.kt
    ├── FavoriteViewModel.kt      # Quản lý yêu thích với Optimistic Update
    ├── DownloadViewModel.kt      # Quản lý tải xuống & offline
    ├── LoginViewModel.kt / RegisterViewModel.kt
    ├── SessionManager.kt         # Lưu trữ Token & DataStore
    └── ...
```

---

## ⚙️ Cài Đặt & Khởi Chạy

### Yêu cầu tiên quyết
- **Android Studio**: Koala (2024.1.1) hoặc Ladybug trở lên.
- **JDK**: Java 11 hoặc Java 17.
- **Android SDK**: `compileSdk = 35`, `minSdk = 26`, `targetSdk = 35`.
- **Hệ điều hành thiết bị/máy ảo**: Android 8.0 (API Level 26) trở lên.

### Cấu hình Backend API
Mở file `app/src/main/java/com/example/app/model/ApiClient.kt` và thay đổi địa chỉ IP phù hợp với máy chủ Backend của bạn:

```kotlin
// Thay đổi IP theo mạng LAN máy chủ của bạn
private const val BASE_URL = "http://<YOUR_BACKEND_IP>:8080/identity/"
```

> **Lưu ý**: Đảm bảo thiết bị Android hoặc máy ảo cùng lớp mạng Wi-Fi với máy chủ backend và cổng `8080` không bị tường lửa chặn.

### Cấu hình Firebase & AdMob
1. Đặt tệp `google-services.json` từ dự án Firebase Console vào thư mục `app/`.
2. Kiểm tra App ID của Google AdMob trong `app/src/main/AndroidManifest.xml`:
   ```xml
   <meta-data
       android:name="com.google.android.gms.ads.APPLICATION_ID"
       android:value="ca-app-pub-3940256099942544~3347511713"/> <!-- Test App ID -->
   ```

### Biên dịch & Khởi chạy

Sử dụng Gradle Wrapper qua Terminal:

```bash
# Kiểm tra biên dịch Kotlin
./gradlew compileDebugKotlin

# Build file APK Debug
./gradlew assembleDebug

# Cài đặt trực tiếp lên thiết bị đang kết nối
./gradlew installDebug
```

Hoặc nhấn nút **Run ▶** trên thanh công cụ của Android Studio.

---

## 📶 Xử Lý Ngoại Tuyến (Offline Mode)

Ứng dụng được tối ưu hoá cho trải nghiệm người dùng khi mạng gián đoạn:
1. **Lưu trữ cục bộ**: Bài hát được tải về lưu trữ an toàn trong thư mục app sandbox, metadata được lưu vào Room DB (`DownloadedSongEntity`).
2. **Khôi phục phiên làm việc**: Khi khởi chạy không có mạng, thông tin user cơ bản được nạp trực tiếp từ DataStore để tránh màn hình trắng.
3. **Phát nhạc không cần Internet**: Trình phát `PlayerManager` tự động phân giải đường dẫn file cục bộ thành định dạng `file://` cho ExoPlayer giải mã mượt mà mà không kích hoạt lỗi Network error.
4. **Nhận diện trạng thái mạng**: Tự động hiển thị thanh thông báo `OfflineBanner` khi mất kết nối mạng và ẩn khi có mạng trở lại.

---

## 👥 Tác Giả & Bản Quyền

Dự án được xây dựng và phát triển bởi:
* **GitHub**: [@DoAnhThu832004](https://github.com/DoAnhThu832004)
* **Kho lưu trữ**: [DAT-login-logout-theme-register-....](https://github.com/DoAnhThu832004/DAT-login-logout-theme-register-.....git)

Được phát hành dưới giấy phép mã nguồn mở tự do phục vụ mục đích học tập và nghiên cứu.
