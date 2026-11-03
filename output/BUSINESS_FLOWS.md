# 03. CÁC LUỒNG NGHIỆP VỤ END-TO-END (BUSINESS_FLOWS.md)

> **Phân loại**: Báo cáo phân tích luồng dữ liệu hệ thống (Traceability Matrix)  
> **Vai trò**: Senior Software Engineer & Solution Architect  
> **Trạng thái**: [CONFIRMED] Truy vết chính xác từ dòng code UI đến Controller, Service và Database.

---

## 1. Luồng Xác Thực & Đăng Ký Cold Start (Authentication & Onboarding Flow)

Luồng giải quyết bài toán Cold Start ngay khi người dùng tạo tài khoản mới.

```mermaid
sequenceDiagram
    autonumber
    actor User as Người dùng
    participant UI as RegisterScreen / OnboardingScreen
    participant VM as RegisterViewModel
    participant API as ApiClient / ApiService
    participant Ctr as UserController
    participant Svc as UserService
    participant Repo as UserRepository & GenreRepository
    participant DB as MySQL Database

    User->>UI: Nhập thông tin (username, password, tên, ngày sinh)
    UI->>VM: validateStep1()
    User->>UI: Chọn danh sách thể loại yêu thích (Genre Chips)
    UI->>VM: onGenreSelected(genreIds)
    VM->>API: createUser(UserCreationRequest)
    API->>Ctr: POST /identity/users
    Ctr->>Svc: createUser(request)
    Svc->>Svc: passwordEncoder.encode(password)
    Svc->>Repo: genreRepository.findAllById(preferredGenreIds)
    Repo-->>Svc: List<Genre>
    Svc->>Svc: user.setPreferredGenres(genres)
    Svc->>Repo: userRepository.save(user)
    Repo->>DB: INSERT into user, user_preferred_genres
    DB-->>Repo: Saved entity
    Repo-->>Svc: User instance
    Svc-->>Ctr: UserResponse
    Ctr-->>API: ApiResponse<UserResponse>
    API-->>VM: Response<UserResponse>
    VM-->>UI: Chuyển hướng sang LoginScreen
```

* **Truy vết mã nguồn [CONFIRMED]**:
  - UI: `app/src/main/java/com/example/app/view/Login/RegisterScreen.kt` & `OnboardingScreen.kt`
  - ViewModel: `app/src/main/java/com/example/app/viewmodel/RegisterViewModel.kt#L120-L150`
  - API Client: `app/src/main/java/com/example/app/model/ApiService.kt#L57` (`POST users`)
  - Controller: `D:\SpringBoot\identity-service-main\identity-service-main\src\main\java\com\devteria\identityservice\controller\UserController.java#L33-L38`
  - Service: `D:\SpringBoot\identity-service-main\identity-service-main\src\main\java\com\devteria\identityservice\service\UserService.java#L60-L83`
  - Database: Bảng `user` và bảng liên kết `user_preferred_genres`.

---

## 2. Luồng Phát Nhạc Trực Tuyến & Ghi Nhận Lượt Nghe (Streaming & Interaction Flow)

Luồng kết hợp giữa việc phát nhạc qua ExoPlayer và gửi tương tác nền về máy chủ để phục vụ thuật toán Recommendation.

```mermaid
sequenceDiagram
    autonumber
    actor User as Người dùng
    participant UI as UserHomePage / PlayerScreen
    participant PM as PlayerManager
    participant SvcPlayer as MusicPlayerService (Foreground)
    participant Cloud as Cloudinary CDN
    participant API as ApiClient / ApiService
    participant Ctr as InteractionController / SongController
    participant SvcBiz as InteractionService & SongService
    participant DB as MySQL Database

    User->>UI: Nhấn vào bài hát (SongItem)
    UI->>PM: playSong(song, songList)
    PM->>SvcPlayer: startService(ACTION_PLAY, audioUrl)
    SvcPlayer->>Cloud: Yêu cầu tải luồng audio (MP3 Stream)
    Cloud-->>SvcPlayer: Audio data buffer
    SvcPlayer->>SvcPlayer: ExoPlayer.prepare() & play()
    SvcPlayer->>UI: Cập nhật MiniPlayer (Playing State, Notification)
    PM->>API: recordListen(songId) & incrementPlayCount(songId)
    API->>Ctr: POST /identity/interactions/listen/{songId}
    Ctr->>SvcBiz: recordInteraction(userId, songId, "LISTEN")
    SvcBiz->>DB: INSERT/UPDATE user_interactions (ratingScore += weight)
    SvcBiz->>DB: UPDATE song SET play_count = play_count + 1
```

* **Truy vết mã nguồn [CONFIRMED]**:
  - UI: `app/src/main/java/com/example/app/view/UserHomePage.kt#L280` & `PlayerScreen.kt`
  - Manager: `app/src/main/java/com/example/app/viewmodel/PlayerManager.kt#L80-L130`
  - Service nền: `app/src/main/java/com/example/app/model/MusicPlayerService.kt#L70-L120`
  - Backend Controller: `InteractionController.java#L20` (`POST /interactions/listen/{songId}`) và `SongController.java#L110` (`POST /songs/{songId}/play`)
  - Backend Service: `InteractionService.java#L40` & `SongService.java#L220`
  - Database: Bảng `user_interactions` và `song`.

---

## 3. Luồng Gợi Ý Âm Nhạc Cá Nhân Hóa (Recommendation Flow)

Hệ thống tính toán thông minh dựa trên 3 tầng dự phòng: `PERSONALIZED`, `COLD_START_GENRE`, `COLD_START_GLOBAL`.

```mermaid
sequenceDiagram
    autonumber
    participant UI as UserHomePage (Compose)
    participant VM as RecommendationViewModel
    participant API as ApiService
    participant Ctr as RecommendationController
    participant Engine as RecommendationEngineService
    participant SimRepo as SongSimilarityRepository
    participant DB as MySQL Database

    UI->>VM: loadHomeRecommendations(userId)
    VM->>API: getHomeRecommendations(userId)
    API->>Ctr: GET /identity/api/recommendations/home?userId={id}
    Ctr->>Engine: getHomeRecommendations(userId)
    alt User có lịch sử tương tác đủ lớn
        Engine->>SimRepo: findSimilarSongs(userInteractedSongIds)
        Engine->>Engine: Áp dụng MMR Re-ranking (lambda = 0.7)
        Engine-->>Ctr: Nguồn: PERSONALIZED
    else User mới có chọn thể loại lúc Onboarding
        Engine->>DB: findSongsByGenreIds(user.getPreferredGenres())
        Engine-->>Ctr: Nguồn: COLD_START_GENRE
    else Khách hoặc chưa có dữ liệu thể loại
        Engine->>DB: findTopPlayedSongs()
        Engine-->>Ctr: Nguồn: COLD_START_GLOBAL
    end
    Ctr-->>API: ApiResponse<HomeRecommendationResponse>
    API-->>VM: Response data
    VM-->>UI: Hiển thị RecommendationSection với Badge tương ứng
```

* **Truy vết mã nguồn [CONFIRMED]**:
  - UI Component: `app/src/main/java/com/example/app/view/general/RecommendationSection.kt`
  - ViewModel: `app/src/main/java/com/example/app/viewmodel/RecommendationViewModel.kt#L40-L75`
  - Backend Engine: `D:\SpringBoot\identity-service-main\identity-service-main\src\main\java\com\devteria\identityservice\service\RecommendationEngineService.java#L80-L240`
  - Thuật toán áp dụng: Item-Based Collaborative Filtering với Adjusted Cosine Similarity và Maximal Marginal Relevance (MMR) để đa dạng hóa danh sách đề xuất.

---

## 4. Luồng Tải Về & Phát Ngoại Tuyến (Offline Download & Playback Flow)

```mermaid
sequenceDiagram
    autonumber
    actor User as Người dùng
    participant UI as SongItem / DownloadScreen
    participant VM as DownloadViewModel
    participant Repo as DownloadRepository
    participant API as ApiService
    participant Room as SongDao (Room Database)
    participant Disk as Internal Storage (/music)

    User->>UI: Nhấn biểu tượng Download bài hát
    UI->>VM: downloadSong(song)
    VM->>Repo: downloadSong(song)
    Repo->>API: downloadSong(songId) [Streaming GET]
    API-->>Repo: ResponseBody (Stream)
    Repo->>Disk: Ghi file MP3 vào local directory
    Repo->>Disk: Tải và ghi cover image vào local directory
    Repo->>Room: songDao.insert(DownloadedSongEntity)
    Room-->>Repo: Success
    Repo-->>VM: Cập nhật trạng thái Download thành công
    VM-->>UI: Hiển thị Icon đã tải
    Note over User,Room: Khi mất mạng hoàn toàn (Offline)
    User->>UI: Mở màn hình Thư viện Offline
    UI->>VM: loadDownloadedSongs()
    VM->>Room: songDao.getAllDownloadedSongs(userId)
    Room-->>VM: List<DownloadedSongEntity>
    VM-->>UI: Hiển thị danh sách phát Offline (phát trực tiếp từ file path)
```

* **Truy vết mã nguồn [CONFIRMED]**:
  - UI: `app/src/main/java/com/example/app/view/InProfile/DownloadScreen.kt`
  - ViewModel: `app/src/main/java/com/example/app/viewmodel/DownloadViewModel.kt#L50-L100`
  - Repository: `app/src/main/java/com/example/app/model/repository/DownloadRepository.kt#L45-L120`
  - Local DB: `app/src/main/java/com/example/app/model/room/SongDao.kt` và `DownloadedSongEntity.kt`
  - Độc lập mạng: Phát qua `File(localAudioPath).toURI()` mà không cần gọi Retrofit hay Backend.
