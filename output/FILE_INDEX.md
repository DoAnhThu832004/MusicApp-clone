# 08. BẢN ĐỒ SOURCE CODE & VỊ TRÍ TRIỂN KHAI (FILE_INDEX.md)

> **Phân loại**: Bản đồ chỉ dẫn mã nguồn (Codebase Navigation Index)  
> **Vai trò**: Senior Software Engineer & Solution Architect  
> **Trạng thái**: [CONFIRMED] Cung cấp đường dẫn tuyệt đối/tương đối chính xác đến từng file phục vụ việc thẩm tra mã nguồn.

---

## 1. Bản Đồ Mã Nguồn Phía Front-end (Android Client)
*Thư mục gốc: `c:\Users\ASUS\AndroidStudioProjects\App`*

### A. Điểm Khởi Chạy, Điều Hướng & Cấu Hình Giao Diện
| Đường dẫn file | Trách nhiệm chính / Chức năng |
| :--- | :--- |
| `app/src/main/java/com/example/app/MainActivity.kt` | Activity chính, khởi tạo môi trường, lắng nghe Remote Config và đặt ngôn ngữ. |
| `app/src/main/java/com/example/app/view/RecipeApp.kt` | Composable gốc chứa `NavHost`, định tuyến toàn bộ màn hình người dùng & admin. |
| `app/src/main/java/com/example/app/view/Screen.kt` | Định nghĩa Sealed Class chứa toàn bộ routes và tham số điều hướng. |
| `app/src/main/java/com/example/app/MainViewModel.kt` | Quản lý trạng thái Theme toàn cục và kiểm tra kết nối mạng. |

### B. Tầng Trình Phát Âm Thanh (Media & Audio Engine)
| Đường dẫn file | Trách nhiệm chính / Chức năng |
| :--- | :--- |
| `app/src/main/java/com/example/app/model/MusicPlayerService.kt` | Foreground Service quản lý vòng đời ExoPlayer, phát nhạc nền và Media Notification. |
| `app/src/main/java/com/example/app/model/MusicPlayerReceiver.kt` | BroadcastReceiver nhận lệnh Play/Pause/Next/Prev từ thanh thông báo hệ thống. |
| `app/src/main/java/com/example/app/viewmodel/PlayerManager.kt` | Singleton quản lý trạng thái phát nhạc (Progress, Shuffle, Repeat) và kết nối Service. |
| `app/src/main/java/com/example/app/viewmodel/PlayerViewModel.kt` | ViewModel liên kết màn hình phát nhạc chi tiết (`PlayerScreen`) với `PlayerManager`. |

### C. Tầng Mạng, Lưu Trữ & Xác Thực (Data & Network Layer)
| Đường dẫn file | Trách nhiệm chính / Chức năng |
| :--- | :--- |
| `app/src/main/java/com/example/app/model/ApiClient.kt` | Cấu hình Retrofit, OkHttp Client và Interceptor tự động Refresh Token. |
| `app/src/main/java/com/example/app/model/ApiService.kt` | Khai báo toàn bộ các phương thức gọi RESTful API đến Backend. |
| `app/src/main/java/com/example/app/viewmodel/SessionManager.kt` | Đọc/ghi Access Token và Refresh Token vào DataStore Preferences. |
| `app/src/main/java/com/example/app/model/room/AppDatabase.kt` | Khởi tạo Room Database (SQLite cục bộ) cho chế độ Offline. |
| `app/src/main/java/com/example/app/model/room/SongDao.kt` | DAO truy vấn bài hát đã tải về máy (`downloaded_songs`). |
| `app/src/main/java/com/example/app/model/room/DownloadedSongEntity.kt` | Thực thể Room lưu trữ đường dẫn file âm thanh và ảnh bìa cục bộ. |

### D. Tầng Màn Hình Người Dùng (User Screens & UI Components)
| Đường dẫn file | Trách nhiệm chính / Chức năng |
| :--- | :--- |
| `app/src/main/java/com/example/app/view/UserHomePage.kt` | Trang chủ người dùng: Bảng xếp hạng, bài hát mới, mục gợi ý cá nhân. |
| `app/src/main/java/com/example/app/view/general/RecommendationSection.kt` | Component hiển thị gợi ý thông minh kèm huy hiệu (`PERSONALIZED`, `COLD_START`). |
| `app/src/main/java/com/example/app/view/Login/LoginScreen.kt` | Giao diện đăng nhập tài khoản người dùng & quản trị viên. |
| `app/src/main/java/com/example/app/view/Login/RegisterScreen.kt` | Giao diện đăng ký bước 1 (thông tin cá nhân). |
| `app/src/main/java/com/example/app/view/general/OnboardingScreen.kt` | Giao diện đăng ký bước 2: Chọn thể loại nhạc yêu thích (Cold Start). |
| `app/src/main/java/com/example/app/view/InProfile/DownloadScreen.kt` | Danh sách bài hát đã tải về máy, phát nhạc không cần mạng. |
| `app/src/main/java/com/example/app/view/Album/AlbumDetailScreen.kt` | Màn hình chi tiết Album và danh sách bài hát thuộc album. |
| `app/src/main/java/com/example/app/view/Artist/DetailArtistScreen.kt` | Màn hình chi tiết Nghệ sĩ, nút Theo dõi (Follow) và các tác phẩm. |

### E. Tầng Màn Hình Quản Trị Viên (Admin Dashboard)
| Đường dẫn file | Trách nhiệm chính / Chức năng |
| :--- | :--- |
| `app/src/main/java/com/example/app/view/admin/SongScreenA.kt` | Quản lý danh mục bài hát, upload MP3 & ảnh bìa. |
| `app/src/main/java/com/example/app/view/admin/AlbumScreenA.kt` | Quản lý tạo/sửa/xóa Album. |
| `app/src/main/java/com/example/app/view/admin/ArtistScreenA.kt` | Quản lý thông tin Nghệ sĩ và gán bài hát. |
| `app/src/main/java/com/example/app/view/admin/UserScreenA.kt` | Quản lý danh sách người dùng, khoá và mở khoá tài khoản. |
| `app/src/main/java/com/example/app/view/admin/ReportScreenA.kt` | Kiểm duyệt và xử lý các báo cáo vi phạm từ người dùng. |

---

## 2. Bản Đồ Mã Nguồn Phía Back-end (Spring Boot REST API)
*Thư mục gốc: `D:\SpringBoot\identity-service-main\identity-service-main`*

### A. Điểm Khởi Chạy & Cấu Hình Hệ Thống
| Đường dẫn file | Trách nhiệm chính / Chức năng |
| :--- | :--- |
| `src/main/java/com/devteria/identityservice/IdentityServiceApplication.java` | Điểm khởi chạy `main()`, cấu hình context Spring Boot. |
| `src/main/resources/application.yaml` | Cấu hình cổng kết nối, MySQL URL, Hibernate, kích thước file và JWT params. |
| `src/main/java/com/devteria/identityservice/configuration/SecurityConfig.java` | Cấu hình Spring Security, phân quyền URL, JWT Resource Server và CORS. |
| `src/main/java/com/devteria/identityservice/configuration/CustomJwtDecoder.java` | Xác thực chữ ký JWT và kiểm tra token blacklist từ DB. |
| `src/main/java/com/devteria/identityservice/configuration/CloudinaryConfig.java` | Cấu hình Bean kết nối lưu trữ đám mây Cloudinary. |
| `src/main/java/com/devteria/identityservice/configuration/ApplicationInitConfig.java` | Tự động tạo tài khoản Admin mặc định khi ứng dụng chạy lần đầu. |

### B. Tầng REST Controllers (Giao Diện API)
| Đường dẫn file | Trách nhiệm chính / Chức năng |
| :--- | :--- |
| `src/main/java/.../controller/AuthenticationController.java` | Endpoint login, refresh, logout, introspect token (`/auth/*`). |
| `src/main/java/.../controller/UserController.java` | Endpoint tạo tài khoản, xem profile, đổi mật khẩu, khoá/mở user (`/users/*`). |
| `src/main/java/.../controller/SongController.java` | Endpoint bài hát: danh sách, top charts, upload file, tăng lượt nghe (`/songs/*`). |
| `src/main/java/.../controller/AlbumController.java` | Endpoint quản lý album (`/albums/*`). |
| `src/main/java/.../controller/ArtistController.java` | Endpoint quản lý nghệ sĩ (`/artists/*`). |
| `src/main/java/.../controller/PlaylistController.java` | Endpoint danh sách phát cá nhân và hệ thống (`/playlists/*`). |
| `src/main/java/.../controller/RecommendationController.java` | Endpoint trả về danh sách nhạc gợi ý cá nhân hóa và trang chủ (`/api/recommendations/*`). |
| `src/main/java/.../controller/RecommendationTriggerController.java` | Endpoint dành cho Admin kích hoạt chạy lại đường ống gợi ý. |
| `src/main/java/.../controller/ReportController.java` | Endpoint gửi và duyệt báo cáo vi phạm (`/reports/*`). |
| `src/main/java/.../controller/CommentController.java` | Endpoint bình luận trên từng bài hát (`/comments/*`). |

### C. Tầng Nghiệp Vụ & Dịch Vụ Cốt Lõi (Service Layer)
| Đường dẫn file | Trách nhiệm chính / Chức năng |
| :--- | :--- |
| `src/main/java/.../service/RecommendationEngineService.java` | Triển khai Item-Based CF, Cosine Similarity và thuật toán MMR Re-ranking. |
| `src/main/java/.../service/InteractionAggregationService.java` | Tổng hợp điểm hành vi (nghe, thích, thêm playlist) thành ma trận xếp hạng. |
| `src/main/java/.../service/UserService.java` | Logic quản trị người dùng, mã hóa mật khẩu, phân quyền và Cold Start genres. |
| `src/main/java/.../service/SongService.java` | Logic xử lý bài hát, upload nhạc/ảnh lên Cloudinary, tính toán lượt phát. |
| `src/main/java/.../service/AuthenticationService.java` | Sinh JWT Token, giải mã claims, gia hạn token và thu hồi token vào blacklist. |

### D. Tầng Cơ Sở Dữ Liệu & Thực Thể (Entities & Repositories)
| Đường dẫn file | Trách nhiệm chính / Chức năng |
| :--- | :--- |
| `src/main/java/.../entity/User.java` | Entity người dùng, liên kết vai trò và thể loại yêu thích. |
| `src/main/java/.../entity/Song.java` | Entity bài hát, liên kết Album, Artist, Thể loại và lượt nghe. |
| `src/main/java/.../entity/SongSimilarity.java` | Entity lưu trữ ma trận điểm tương đồng giữa các cặp bài hát. |
| `src/main/java/.../entity/UserInteraction.java` | Entity lưu trữ điểm tương tác giữa người dùng và bài hát. |
| `src/main/java/.../entity/InvalidatedToken.java` | Entity lưu trữ mã token đã đăng xuất (Blacklist). |
| `src/main/java/.../repository/SongSimilarityRepository.java` | JPA Repository truy vấn điểm tương đồng cao nhất cho gợi ý. |
| `src/main/java/.../repository/UserInteractionRepository.java` | JPA Repository truy vấn điểm tương tác phục vụ thuật toán. |

### E. Tầng Xử Lý Lỗi (Exception Handling)
| Đường dẫn file | Trách nhiệm chính / Chức năng |
| :--- | :--- |
| `src/main/java/.../exception/GlobalExceptionHandler.java` | `@ControllerAdvice` bắt lỗi toàn cục và trả về chuẩn `ApiResponse`. |
| `src/main/java/.../exception/ErrorCode.java` | Khai báo Enum mã lỗi số, thông điệp và HTTP Status code tương ứng. |
| `src/main/java/.../exception/AppException.java` | Custom RuntimeException chứa đối tượng `ErrorCode`. |
