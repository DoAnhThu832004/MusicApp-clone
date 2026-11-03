# 07. ĐÁNH GIÁ CHẤT LƯỢNG KỸ THUẬT & NỢ KỸ THUẬT (QUALITY_REVIEW.md)

> **Phân loại**: Báo cáo chất lượng mã nguồn & Nợ kỹ thuật (Code Quality & Technical Debt)  
> **Vai trò**: Senior Software Engineer & Solution Architect  
> **Trạng thái**: [CONFIRMED] Trích xuất từ việc phân tích tĩnh (Static Analysis) trực tiếp trên toàn bộ source code.

---

## 1. Đánh Giá Độ Bao Phủ Kiểm Thử (Test Coverage & Observability)

### A. Kiểm Thử Tự Động (Automated Testing) [CONFIRMED - ĐÁNH GIÁ: BÁO ĐỘNG ĐỎ]
- **Tình trạng Back-end**:
  - Toàn bộ backend chỉ có **3 file test**:
    - `IdentityServiceApplicationTests.java`: Test context loading cơ bản.
    - `UserControllerTest.java`: Test một vài trường hợp của User Controller.
    - `UserServiceTest.java`: Test cơ bản cho UserService.
  - **Khu vực hoàn toàn không có test (0% Coverage)**: `SongService`, `AlbumService`, `ArtistService`, `PlaylistService`, `CommentService`, `FavoriteService`, và đặc biệt là tầng thuật toán phức tạp `RecommendationEngineService`!
  - Không có bất kỳ Integration Test nào kiểm tra luồng phát sinh lỗi của Spring Security FilterChain hoặc JWT Invalidation.
- **Tình trạng Front-end**:
  - Chỉ có `ThemeRepositoryTest.kt` và `ExampleUnitTest.kt`.
  - Toàn bộ ViewModels, Room DAOs, ExoPlayer Manager và Network Interceptors đều **chưa có Unit Test hay UI Test**.

### B. Giám Sát & Ghi Log (Logging & Observability) [CONFIRMED]
- **Điểm sáng**:
  - Android tích hợp **Firebase Crashlytics** (`CrashlyticsHelper.kt`) và **Firebase Analytics** (`AnalyticsHelper.kt`) để ghi nhận lỗi crash runtime và sự kiện hành vi.
  - Backend sử dụng Slf4j ghi log rõ ràng ở tầng Service và ExceptionHandler.
- **Hạn chế**:
  - Chưa có cơ chế Distributed Tracing (như Micrometer Tracing / Zipkin / OpenTelemetry) để theo dõi tương quan giữa một request từ Android gửi đến Backend.

---

## 2. Các Vi Phạm Nguyên Lý Thiết Kế & Code Smells (Code Smells & SOLID Violations)

### 🔴 1. Vi Phạm Nguyên Lý DRY & Nhất Quán Dữ Liệu Trong `ErrorCode.java` [CONFIRMED]
* **Vị trí**: `D:\SpringBoot\identity-service-main\identity-service-main\src\main\java\com\devteria\identityservice\exception\ErrorCode.java#L19-L24`
* **Bằng chứng**:
  ```java
  SONG_NOT_EXISTED(2005,"User not existed",HttpStatus.NOT_FOUND),
  PLAYLIST_NOT_EXISTED(2000,"Playlist not existed", HttpStatus.NOT_FOUND),
  ALBUM_NOT_EXISTED(2000,"Album not existed", HttpStatus.NOT_FOUND),
  ARTIST_NOT_EXISTED(2000,"Artist not existed", HttpStatus.NOT_FOUND),
  ```
* **Phân tích**:
  - `SONG_NOT_EXISTED`: Mã lỗi 2005 nhưng thông điệp lại ghi là `"User not existed"`. Người dùng Android khi mở bài hát bị xóa sẽ nhận được thông báo kỳ lạ "Người dùng không tồn tại".
  - Ba lỗi `PLAYLIST_NOT_EXISTED`, `ALBUM_NOT_EXISTED`, `ARTIST_NOT_EXISTED` đều dùng chung một mã `code: 2000`. Phía Client không thể phân biệt được lỗi nào thuộc về đối tượng nào nếu chỉ dựa vào mã lỗi số.

---

### 🟡 2. Không Nhất Quán Trong Sử Dụng Thư Viện MapStruct & Lombok [CONFIRMED]
* **Vị trí 1**: `D:\SpringBoot\identity-service-main\identity-service-main\src\main\java\com\devteria\identityservice\service\UserService.java#L173-L185`
  - Đã có `UserMapper` được inject vào class nhưng trong hàm `getUsers()` lại viết code mapping thủ công:
    ```java
    // MAPPING THỦ CÔNG TỪ ENTITY SANG DTO
    List<UserResponse> userResponses = pageUser.getContent().stream()
            .map(user -> {
                UserResponse res = new UserResponse();
                res.setId(user.getId());
                res.setUsername(user.getUsername());
                ...
    ```
* **Vị trí 2**: So sánh `User.java` và `Song.java`
  - Trong khi `User.java` sử dụng đầy đủ các annotation tinh gọn của Lombok (`@Getter`, `@Setter`, `@Builder`, `@FieldDefaults`), thì `Song.java` lại tự viết tay hàng chục hàm Getter/Setter truyền thống dài hơn 180 dòng, gây phân mảnh phong cách lập trình.
  - Trong `Song.java#L7` có import thừa thư viện desktop của Java: `import javax.swing.plaf.BorderUIResource;` (hoàn toàn không liên quan đến backend web).

---

### 🟡 3. Nợ Tên Gọi Di Sản (Legacy Naming Debt) Trên Android [CONFIRMED]
* **Vị trí**: `app/src/main/java/com/example/app/view/RecipeApp.kt`
* **Mô tả**:
  - File điều hướng trung tâm và composable root của toàn bộ ứng dụng âm nhạc lại có tên là `RecipeApp.kt` (Ứng dụng dạy nấu ăn).
  - Đây là tàn dư (legacy artifact) từ project mẫu hoặc template ban đầu khi khởi tạo dự án. Dù không ảnh hưởng đến việc biên dịch, việc giữ tên `RecipeApp` làm giảm tính chuyên nghiệp của codebase và gây khó hiểu cho các lập trình viên mới tiếp nhận.

---

### 🟡 4. Rủi Ro Rò Rỉ Bộ Nhớ Của Trình Phát Nhạc (Memory Leak & Resource Cleanup) [CONFIRMED]
* **Vị trí**: `app/src/main/java/com/example/app/viewmodel/PlayerManager.kt` & `MusicPlayerService.kt`
* **Mô tả**:
  - `PlayerManager` duy trì tham chiếu Singleton tới context và danh sách bài hát.
  - Khi người dùng dừng phát nhạc hoặc đổi danh sách phát liên tục, nếu `ExoPlayer.release()` không được đảm bảo gọi trong mọi nhánh lỗi (Lifecycle Destroy), các Audio Buffer và Notification Manager có thể gây rò rỉ bộ nhớ (leak native media resources) trên các dòng máy Android đời cũ.

---

## 3. Đánh Giá Khả Năng Mở Rộng & Hiệu Năng (Scalability & Performance)

1. **Thuật toán Gợi ý In-Memory vs Database Scaling**:
   - `RecommendationEngineService.java` hiện đang tính toán trực tiếp ma trận tương đồng trên RAM của ứng dụng Spring Boot.
   - Khi kho nhạc vượt quá 10.000 bài hát và hàng trăm nghìn tương tác người dùng, việc tính toán trực tiếp này sẽ gây nghẽn CPU và có thể gây lỗi `OutOfMemoryError (OOM)`. Cần chuyển việc tính toán offline sang Apache Spark hoặc một nền tảng tính toán phân tán độc lập.
2. **Truy vấn N+1 trong JPA**:
   - Trong `Song.java`, các mối quan hệ `@ManyToMany` và `@ManyToOne` (như `Set<Genre>`, `Album`, `Set<Artist>`) cần được theo dõi kỹ trong các câu lệnh truy vấn danh sách để đảm bảo sử dụng `JOIN FETCH` hoặc `EntityGraph`, tránh phát sinh hàng chục câu query phụ (N+1 query problem).
