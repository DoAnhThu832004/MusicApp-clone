# 06. ĐÁNH GIÁ BẢO MẬT HỆ THỐNG (SECURITY_REVIEW.md)

> **Phân loại**: Báo cáo kiểm định an ninh ứng dụng (Application Security Audit)  
> **Vai trò**: Senior Software Engineer & Solution Architect  
> **Trạng thái**: [CONFIRMED] Mọi phát hiện đều có bằng chứng trực tiếp từ dòng mã nguồn cụ thể. Không tự suy diễn hay khẳng định an toàn nếu chưa kiểm chứng.

---

## 1. Tóm Tắt Ma Trận Rủi Ro An Ninh (Security Risk Matrix)

| Mức độ | Số lượng | Vấn đề chính |
| :---: | :---: | :--- |
| 🔴 **CRITICAL** | **2** | Lỗ hổng leo thang đặc quyền (Privilege Escalation), Lộ lọt Secret/Key cứng trong mã nguồn |
| 🟠 **HIGH** | **2** | Cấu hình CORS mở toàn bộ (Wildcard CORS), Tham chiếu đối tượng trực tiếp không an toàn (IDOR) |
| 🟡 **MEDIUM** | **2** | `@PostAuthorize` thực thi sau khi sửa đổi dữ liệu, Thiếu cơ chế dọn dẹp Token Blacklist |
| 🟢 **LOW** | **1** | Thiếu chính sách mật khẩu mạnh (Password Complexity) |

---

## 2. Chi Tiết Các Lỗ Hổng & Rủi Ro Đã Xác Minh

### 🔴 LỖ HỔNG 1: Leo Thang Đặc Quyền Tài Khoản (Privilege Escalation) [CRITICAL]
* **Vị trí bằng chứng**:
  - `D:\SpringBoot\identity-service-main\identity-service-main\src\main\java\com\devteria\identityservice\service\UserService.java#L110-L116`
  - `D:\SpringBoot\identity-service-main\identity-service-main\src\main\java\com\devteria\identityservice\controller\UserController.java#L70-L76`
* **Đoạn code liên quan**:
  ```java
  // Trong UserService.java
  if (request.getRoles() != null && !request.getRoles().isEmpty()) {
      Set<Role> roles = new HashSet<>(
              roleRepository.findAllById(request.getRoles())
      );
      user.setRoles(roles); // Gán trực tiếp role từ DTO của Client gửi lên
  }
  ```
* **Mô tả & Tác động**:
  - API `PUT /users/{userId}` cho phép người dùng tự cập nhật thông tin cá nhân.
  - Tuy nhiên, trong `UserUpdateRequest`, trường `roles` được chấp nhận và cập nhật trực tiếp vào Entity mà **hoàn toàn không kiểm tra xem người gọi có phải là ADMIN hay không**.
  - **Kịch bản tấn công**: Bất kỳ người dùng thông thường (`USER`) nào chỉ cần gửi request HTTP PUT tới tài khoản của mình kèm payload `{"roles": ["ADMIN"]}` là có thể tự phong quyền Quản trị viên tối cao, toàn quyền xóa sửa dữ liệu toàn hệ thống.
* **Biện pháp khắc phục khuyến nghị**:
  - Tuyệt đối không cho phép cập nhật `roles` thông qua API update thông thường của User. Tạo riêng một API `@PreAuthorize("hasRole('ADMIN')")` dành riêng cho việc phân quyền.

---

### 🔴 LỖ HỔNG 2: Hardcoded Secrets & API Keys Trong Repository [CRITICAL]
* **Vị trí bằng chứng**:
  - `D:\SpringBoot\identity-service-main\identity-service-main\src\main\java\com\devteria\identityservice\configuration\CloudinaryConfig.java#L12-L17`
  - `D:\SpringBoot\identity-service-main\identity-service-main\src\main\resources\application.yaml#L21`
* **Mô tả & Tác động**:
  - Thông tin xác thực dịch vụ Cloudinary (`cloud_name`, `api_key`, `api_secret`) bị viết cứng (hardcoded) trực tiếp trong file mã nguồn Java và đã được commit vào Git repository.
  - Khóa ký JWT (`jwt.signerKey`) được đặt giá trị mặc định trực tiếp trong `application.yaml`.
  - **Kịch bản tấn công**: Bất kỳ ai có quyền truy cập mã nguồn hoặc lấy được file cấu hình đều có thể giả mạo chữ ký JWT (tự sinh token của bất kỳ user nào mà không cần mật khẩu) hoặc chiếm quyền kiểm soát toàn bộ kho lưu trữ media trên Cloudinary.
* **Biện pháp khắc phục khuyến nghị**:
  - Lập tức thu hồi (Revoke/Rotate) API Key trên dashboard Cloudinary và đổi JWT Signer Key.
  - Đưa toàn bộ các giá trị nhạy cảm vào biến môi trường hệ thống (`${CLOUDINARY_API_KEY}`, `${JWT_SIGNER_KEY}`) và không bao giờ commit giá trị thật vào Git.

---

### 🟠 LỖ HỔNG 3: Tham Chiếu Đối Tượng Trực Tiếp Không An Toàn (IDOR) [HIGH]
* **Vị trí bằng chứng**:
  - `D:\SpringBoot\identity-service-main\identity-service-main\src\main\java\com\devteria\identityservice\controller\RecommendationController.java#L24-L38`
  - `D:\SpringBoot\identity-service-main\identity-service-main\src\main\java\com\devteria\identityservice\controller\UserController.java#L78-L87`
* **Mô tả & Tác động**:
  - Trong `RecommendationController`: API lấy gợi ý cá nhân hóa yêu cầu truyền `userId` qua query param: `GET /api/recommendations/home?userId=xxx`. Controller không hề đối chiếu xem `userId` này có trùng với `authentication.getName()` của token người dùng đang đăng nhập hay không.
  - Trong `UserController#uploadProfileImage`: Endpoint nhận `/{userId}/upload` mà không xác minh quyền sở hữu tài khoản trước khi ghi đè ảnh đại diện.
  - **Tác động**: Người dùng A có thể xem trộm lịch sử nghe nhạc, hành vi gợi ý và can thiệp ảnh đại diện của người dùng B chỉ bằng cách thay đổi tham số ID.
* **Biện pháp khắc phục khuyến nghị**:
  - Loại bỏ hoàn toàn tham số `userId` trên URL/Query param đối với các API cá nhân. Lấy thông tin user hiện tại trực tiếp từ `SecurityContextHolder.getContext().getAuthentication().getName()`.

---

### 🟠 LỖ HỔNG 4: Cấu Hình CORS Mở Toàn Bộ (Wildcard CORS) [HIGH]
* **Vị trí bằng chứng**:
  - `D:\SpringBoot\identity-service-main\identity-service-main\src\main\java\com\devteria\identityservice\configuration\SecurityConfig.java#L55-L57`
* **Mô tả**:
  ```java
  corsConfiguration.addAllowedOrigin("*");
  corsConfiguration.addAllowedMethod("*");
  corsConfiguration.addAllowedHeader("*");
  ```
  - Cấu hình cho phép mọi domain web (`*`) đều có thể gửi request đến API. Khi triển khai thêm Web Client hoặc ứng dụng web, cấu hình này có thể dẫn đến tấn công Cross-Origin Request Forgery hoặc rò rỉ dữ liệu qua trình duyệt.

---

### 🟡 VẤN ĐỀ 5: Sử Dụng `@PostAuthorize` Trên Phương Thức Có `@Transactional` [MEDIUM]
* **Vị trí bằng chứng**:
  - `D:\SpringBoot\identity-service-main\identity-service-main\src\main\java\com\devteria\identityservice\service\UserService.java#L93-L95`
* **Mô tả**:
  - `@PostAuthorize` chỉ kiểm tra quyền truy cập **sau khi** logic bên trong hàm đã chạy xong.
  - Trong hàm `updateUser()`, các thao tác `userMapper.updateUser(user, request)` và `userRepository.save(user)` đã được thực thi. Mặc dù lỗi `AccessDeniedException` được quăng ra sau đó sẽ trigger rollback transaction, việc để logic sửa đổi chạy trước khi kiểm tra quyền là một Anti-pattern bảo mật, dễ tạo side-effects nếu có các hành động không hỗ trợ rollback (như gửi email, gọi external API).

---

### 🟡 VẤN ĐỀ 6: Cơ Chế Dọn Dẹp Token Hết Hạn Trên Database [MEDIUM]
* **Vị trí bằng chứng**:
  - `D:\SpringBoot\identity-service-main\identity-service-main\src\main\java\com\devteria\identityservice\repository\InvalidatedTokenRepository.java`
* **Mô tả**:
  - Bảng `invalidated_token` lưu các token đã logout để ngăn chặn tái sử dụng.
  - Tuy nhiên, trong mã nguồn hoàn toàn **không có Scheduled Job** nào để định kỳ xóa các bản ghi token mà `expiryTime` đã trôi qua. Theo thời gian, bảng này sẽ phình to không giới hạn, làm chậm các truy vấn kiểm tra token khi giải mã JWT (`CustomJwtDecoder`).
