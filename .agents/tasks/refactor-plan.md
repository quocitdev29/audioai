# Kế hoạch Tái cấu trúc Backend - Phân tách Admin/User/Public Endpoints

## Tổng quan

Hệ thống backend hiện tại có các vấn đề sau:
1. **Typo tên file**: `AuthConrtroller.java` (phải là `AuthController.java`)
2. **Phân quyền không nhất quán**: Mỗi controller tự kiểm tra admin bằng `checkAdminPermission()` trùng lặp
3. **JWT manual extraction**: `OrderController` và `StoryOwnershipController` tự extract JWT thay vì dùng `@AuthenticationPrincipal`
4. **@CrossOrigin lộn xộn**: Một số controller có `@CrossOrigin` riêng lẻ, không cần thiết vì CORS đã config global
5. **Cấu trúc endpoint chưa rõ ràng**: Public/User/Admin endpoints lẫn lộn trong các controller

## Phân tích Endpoints Hiện tại

### 1. AuthConrtroller.java (typo - cần đổi tên)
**Path**: `/api/auth`
- ✅ `POST /register` - Public
- ✅ `POST /login` - Public  
- ✅ `GET /me` - User (dùng `@AuthenticationPrincipal`)
- ✅ `PUT /profile` - User (dùng `@AuthenticationPrincipal`)
- ✅ `PUT /change-password` - User (dùng `@AuthenticationPrincipal`)

**Dependencies**: `AuthService`
**Đánh giá**: Controller này đã tốt, chỉ cần đổi tên file. Đã dùng `@AuthenticationPrincipal UserDetails`.

---

### 2. PublicStoryController.java
**Path**: `/api/stories`
- ✅ `GET /` - Public (lấy danh sách truyện, filter theo status)
- ✅ `GET /latest` - Public (truyện mới nhất)
- ✅ `GET /category/{categoryId}` - Public (truyện theo thể loại)

**Dependencies**: `StoryService`
**Đánh giá**: Controller public tốt, sẽ merge vào `PublicController` mới.

---

### 3. StoryController.java  
**Path**: `/api/admin/stories`
- 🔴 `GET /` - Admin (có `checkAdminPermission()`)
- 🔴 `GET /{id}` - Admin
- 🔴 `POST /` - Admin
- 🔴 `PUT /{id}` - Admin
- 🔴 `DELETE /{id}` - Admin
- 🔴 `GET /category/{categoryId}` - Admin
- 🔴 `GET /status/{status}` - Admin
- 🔴 `POST /upload-cover` - Admin (multipart)

**Dependencies**: `StoryService`, `FileStorageService`
**Đánh giá**: Đổi tên thành `AdminStoryController`, kế thừa `BaseAdminController`.

---

### 4. ChapterController.java
**Path**: `/api/admin/chapters` (admin) và `/api/chapters` (public)
- ✅ `GET /api/chapters/story/{storyId}` - Public (có hỗ trợ optional `@AuthenticationPrincipal`)
- 🔴 `GET /api/admin/chapters` - Admin (có `checkAdminPermission()`)
- 🔴 `GET /api/admin/chapters/story/{storyId}` - Admin
- 🔴 `GET /api/admin/chapters/{id}` - Admin
- 🔴 `POST /api/admin/chapters` - Admin
- 🔴 `POST /api/admin/chapters/upload-audio` - Admin (multipart)
- 🔴 `PUT /api/admin/chapters/{id}` - Admin
- 🔴 `DELETE /api/admin/chapters/{id}` - Admin

**Dependencies**: `ChapterService`
**Đánh giá**: Tách thành 2 controllers:
- Public endpoint vào `PublicController`
- Admin endpoints vào `AdminChapterController` kế thừa `BaseAdminController`

---

### 5. CategoryController.java
**Path**: `/api/categories`
- ✅ `GET /` - Public (lấy tất cả categories)

**Dependencies**: `CategoryRepository` (trực tiếp, không qua Service)
**Đánh giá**: Merge vào `PublicController`, tạo `CategoryService` nếu cần logic phức tạp hơn.

---

### 6. OrderController.java
**Path**: `/api/orders`
- 🔴 `POST /purchase-story` - User (manual JWT extraction ❌)
- 🔴 `GET /my-purchases` - User (manual JWT extraction ❌)
- 🔴 Có `@CrossOrigin(origins = "*")` không cần thiết

**Dependencies**: `OrderService`, `JwtService` (không nên dùng trực tiếp), `UserRepository`
**Đánh giá**: Đổi tên thành `UserController`, loại bỏ manual JWT, dùng `@AuthenticationPrincipal`.

---

### 7. StoryOwnershipController.java
**Path**: `/api/story-ownerships`
- 🔴 `GET /check/{storyId}` - User (manual JWT extraction ❌)
- 🔴 Có `@CrossOrigin(origins = "*")` không cần thiết

**Dependencies**: `OrderService`, `JwtService`, `UserRepository`
**Đánh giá**: Merge vào `UserController`, loại bỏ manual JWT.

---

### 8. VnPayController.java
**Path**: `/api/payment/vnpay`
- 🔴 `POST /create-payment` - User (dùng `@AuthenticationPrincipal` ✅)
- ✅ `GET /return` - Public (VNPay callback)
- ✅ `GET /ipn` - Public (VNPay IPN)

**Dependencies**: `PaymentService`, `VnPayService`
**Đánh giá**: Tách thành 2 controllers:
- `UserPaymentController` (`/api/user/payment/vnpay`) cho `/create-payment`
- `PublicPaymentController` (`/api/public/payment/vnpay`) cho `/return` và `/ipn`

---

### 9. AdminController.java
**Path**: `/api/admin`
- 🔴 `GET /statistics` - Admin (có `checkAdminPermission()`)
- 🔴 `GET /payments` - Admin (có `checkAdminPermission()`)

**Dependencies**: `AdminService`
**Đánh giá**: Giữ nguyên, nhưng kế thừa `BaseAdminController`.

---

## Cấu trúc Mới Được Đề xuất

### Base Classes

#### BaseAdminController.java (Abstract)
```java
@RequiredArgsConstructor
public abstract class BaseAdminController {
    protected void checkAdminPermission(UserDetails userDetails) {
        if (!"admin".equals(userDetails.getUsername())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập");
        }
    }
}
```

### Public Controllers (không cần authentication)

#### 1. PublicController.java
**Path**: `/api/public`
**Endpoints**:
- `GET /stories` - Danh sách truyện (từ PublicStoryController)
- `GET /stories/latest` - Truyện mới nhất (từ PublicStoryController)
- `GET /stories/category/{categoryId}` - Truyện theo thể loại (từ PublicStoryController)
- `GET /categories` - Danh sách thể loại (từ CategoryController)
- `GET /chapters/story/{storyId}` - Chapters của truyện (từ ChapterController, hỗ trợ optional auth)

**Dependencies**: `StoryService`, `CategoryRepository`, `ChapterService`
**Note**: Optional `@AuthenticationPrincipal UserDetails` cho endpoint chapters để unlock nếu user đã mua.

#### 2. PublicPaymentController.java  
**Path**: `/api/public/payment/vnpay`
**Endpoints**:
- `GET /return` - VNPay return callback (từ VnPayController)
- `GET /ipn` - VNPay IPN (từ VnPayController)

**Dependencies**: `PaymentService`, `VnPayService`

---

### Auth Controller

#### 3. AuthController.java (rename từ AuthConrtroller)
**Path**: `/api/auth`
**Endpoints**: Giữ nguyên tất cả
- `POST /register`
- `POST /login`
- `GET /me`
- `PUT /profile`
- `PUT /change-password`

**Dependencies**: `AuthService`
**Changes**: Chỉ đổi tên file từ `AuthConrtroller.java` → `AuthController.java`

---

### User Controllers (require authentication)

#### 4. UserController.java (rename từ OrderController)
**Path**: `/api/user`
**Endpoints**:
- `POST /orders/purchase-story` - Mua truyện (từ OrderController)
- `GET /orders/my-purchases` - Lịch sử mua (từ OrderController)
- `GET /story-ownerships/check/{storyId}` - Kiểm tra ownership (từ StoryOwnershipController)

**Dependencies**: `OrderService`
**Changes**:
- Loại bỏ `JwtService`, `UserRepository` injection
- Loại bỏ `extractToken()` method
- Dùng `@AuthenticationPrincipal UserDetails` cho tất cả endpoints
- Loại bỏ `@CrossOrigin`

#### 5. UserPaymentController.java
**Path**: `/api/user/payment/vnpay`
**Endpoints**:
- `POST /create-payment` - Tạo payment URL (từ VnPayController)

**Dependencies**: `PaymentService`

---

### Admin Controllers (require admin role)

#### 6. AdminStoryController.java (rename từ StoryController)
**Path**: `/api/admin/stories`
**Endpoints**: Giữ nguyên tất cả
- `GET /`
- `GET /{id}`
- `POST /`
- `PUT /{id}`
- `DELETE /{id}`
- `GET /category/{categoryId}`
- `GET /status/{status}`
- `POST /upload-cover`

**Dependencies**: `StoryService`, `FileStorageService`
**Changes**: Extends `BaseAdminController`, loại bỏ method `checkAdminPermission()` riêng.

#### 7. AdminChapterController.java
**Path**: `/api/admin/chapters`
**Endpoints**: Chỉ các admin endpoints từ ChapterController
- `GET /`
- `GET /story/{storyId}`
- `GET /{id}`
- `POST /`
- `POST /upload-audio`
- `PUT /{id}`
- `DELETE /{id}`

**Dependencies**: `ChapterService`
**Changes**: Extends `BaseAdminController`, loại bỏ method `checkAdminPermission()` riêng.

#### 8. AdminController.java
**Path**: `/api/admin`
**Endpoints**: Giữ nguyên
- `GET /statistics`
- `GET /payments`

**Dependencies**: `AdminService`
**Changes**: Extends `BaseAdminController`, loại bỏ method `checkAdminPermission()` riêng.

---

## Mapping Table: Endpoint Changes

| Current Endpoint | Controller | New Endpoint | New Controller |
|-----------------|------------|--------------|----------------|
| `POST /api/auth/register` | AuthConrtroller | `POST /api/auth/register` | AuthController ✏️ |
| `POST /api/auth/login` | AuthConrtroller | `POST /api/auth/login` | AuthController ✏️ |
| `GET /api/auth/me` | AuthConrtroller | `GET /api/auth/me` | AuthController ✏️ |
| `PUT /api/auth/profile` | AuthConrtroller | `PUT /api/auth/profile` | AuthController ✏️ |
| `PUT /api/auth/change-password` | AuthConrtroller | `PUT /api/auth/change-password` | AuthController ✏️ |
| `GET /api/stories` | PublicStoryController | `GET /api/public/stories` | PublicController 🆕 |
| `GET /api/stories/latest` | PublicStoryController | `GET /api/public/stories/latest` | PublicController 🆕 |
| `GET /api/stories/category/{id}` | PublicStoryController | `GET /api/public/stories/category/{id}` | PublicController 🆕 |
| `GET /api/categories` | CategoryController | `GET /api/public/categories` | PublicController 🆕 |
| `GET /api/chapters/story/{storyId}` | ChapterController | `GET /api/public/chapters/story/{storyId}` | PublicController 🆕 |
| `POST /api/orders/purchase-story` | OrderController | `POST /api/user/orders/purchase-story` | UserController 🆕 |
| `GET /api/orders/my-purchases` | OrderController | `GET /api/user/orders/my-purchases` | UserController 🆕 |
| `GET /api/story-ownerships/check/{id}` | StoryOwnershipController | `GET /api/user/story-ownerships/check/{id}` | UserController 🆕 |
| `POST /api/payment/vnpay/create-payment` | VnPayController | `POST /api/user/payment/vnpay/create-payment` | UserPaymentController 🆕 |
| `GET /api/payment/vnpay/return` | VnPayController | `GET /api/public/payment/vnpay/return` | PublicPaymentController 🆕 |
| `GET /api/payment/vnpay/ipn` | VnPayController | `GET /api/public/payment/vnpay/ipn` | PublicPaymentController 🆕 |
| `GET /api/admin/stories` | StoryController | `GET /api/admin/stories` | AdminStoryController ✏️ |
| `GET /api/admin/stories/{id}` | StoryController | `GET /api/admin/stories/{id}` | AdminStoryController ✏️ |
| `POST /api/admin/stories` | StoryController | `POST /api/admin/stories` | AdminStoryController ✏️ |
| `PUT /api/admin/stories/{id}` | StoryController | `PUT /api/admin/stories/{id}` | AdminStoryController ✏️ |
| `DELETE /api/admin/stories/{id}` | StoryController | `DELETE /api/admin/stories/{id}` | AdminStoryController ✏️ |
| `GET /api/admin/stories/category/{id}` | StoryController | `GET /api/admin/stories/category/{id}` | AdminStoryController ✏️ |
| `GET /api/admin/stories/status/{status}` | StoryController | `GET /api/admin/stories/status/{status}` | AdminStoryController ✏️ |
| `POST /api/admin/stories/upload-cover` | StoryController | `POST /api/admin/stories/upload-cover` | AdminStoryController ✏️ |
| `GET /api/admin/chapters` | ChapterController | `GET /api/admin/chapters` | AdminChapterController 🆕 |
| `GET /api/admin/chapters/story/{id}` | ChapterController | `GET /api/admin/chapters/story/{id}` | AdminChapterController 🆕 |
| `GET /api/admin/chapters/{id}` | ChapterController | `GET /api/admin/chapters/{id}` | AdminChapterController 🆕 |
| `POST /api/admin/chapters` | ChapterController | `POST /api/admin/chapters` | AdminChapterController 🆕 |
| `POST /api/admin/chapters/upload-audio` | ChapterController | `POST /api/admin/chapters/upload-audio` | AdminChapterController 🆕 |
| `PUT /api/admin/chapters/{id}` | ChapterController | `PUT /api/admin/chapters/{id}` | AdminChapterController 🆕 |
| `DELETE /api/admin/chapters/{id}` | ChapterController | `DELETE /api/admin/chapters/{id}` | AdminChapterController 🆕 |
| `GET /api/admin/statistics` | AdminController | `GET /api/admin/statistics` | AdminController ✏️ |
| `GET /api/admin/payments` | AdminController | `GET /api/admin/payments` | AdminController ✏️ |

**Legend**:
- ✏️ = Rename/refactor existing controller
- 🆕 = New controller
- ❌ = Delete controller

---

## Thay đổi trong SecurityConfig.java

Cập nhật `.permitAll()` matchers để phản ánh cấu trúc mới:

```java
.authorizeHttpRequests(auth -> auth
    // Auth endpoints - Public
    .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
    
    // Public endpoints - không cần authentication
    .requestMatchers(HttpMethod.GET,
        "/api/public/**",                          // Tất cả public endpoints
        "/Data/**").permitAll()                    // Static files
    
    // OPTIONS preflight
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
    
    // Error page
    .requestMatchers("/error").permitAll()
    
    // Tất cả còn lại cần authentication
    .anyRequest().authenticated()
)
```

---

## Implementation Plan

### Phase 1: Tạo Base Classes và Cấu trúc mới

- [ ] **1. Tạo BaseAdminController abstract class**
  - Tạo file: `src/main/java/com/example/audioai/controller/BaseAdminController.java`
  - Implement method `checkAdminPermission(UserDetails)`
  - Verify: Compile thành công (`mvn clean compile`)

### Phase 2: Refactor Public Controllers

- [ ] **2. Tạo PublicController (merge PublicStoryController + CategoryController + public chapter endpoint)**
  - Tạo file: `src/main/java/com/example/audioai/controller/PublicController.java`
  - Path: `/api/public`
  - Dependencies: `StoryService`, `CategoryRepository`, `ChapterService`
  - Endpoints:
    - `GET /stories` (từ PublicStoryController)
    - `GET /stories/latest` (từ PublicStoryController)
    - `GET /stories/category/{categoryId}` (từ PublicStoryController)
    - `GET /categories` (từ CategoryController)
    - `GET /chapters/story/{storyId}` (từ ChapterController, optional `@AuthenticationPrincipal`)
  - Verify: Test thủ công với Postman/curl từng endpoint

- [ ] **3. Tạo PublicPaymentController**
  - Tạo file: `src/main/java/com/example/audioai/controller/PublicPaymentController.java`
  - Path: `/api/public/payment/vnpay`
  - Dependencies: `PaymentService`, `VnPayService`
  - Endpoints:
    - `GET /return` (từ VnPayController)
    - `GET /ipn` (từ VnPayController)
  - Verify: Test thủ công với mock VNPay params

### Phase 3: Refactor Auth Controller

- [ ] **4. Rename AuthConrtroller → AuthController**
  - Rename file: `AuthConrtroller.java` → `AuthController.java`
  - Kiểm tra class name trong file cũng đổi thành `AuthController`
  - Verify: `mvn clean compile` không có lỗi

### Phase 4: Refactor User Controllers

- [ ] **5. Refactor OrderController → UserController**
  - Rename file: `OrderController.java` → `UserController.java`
  - Đổi class name thành `UserController`
  - Đổi path: `/api/orders` → `/api/user`
  - Loại bỏ dependencies: `JwtService`, `UserRepository`
  - Loại bỏ method `extractToken()`
  - Loại bỏ `@CrossOrigin`
  - Refactor `purchaseStory()`:
    - Thêm parameter `@AuthenticationPrincipal UserDetails userDetails`
    - Loại bỏ manual JWT extraction
    - Gọi service với `userDetails.getUsername()`
  - Refactor `getMyPurchases()`:
    - Thêm parameter `@AuthenticationPrincipal UserDetails userDetails`
    - Loại bỏ manual JWT extraction
    - Gọi service với `userDetails.getUsername()`
  - Merge endpoint từ StoryOwnershipController:
    - `GET /story-ownerships/check/{storyId}` → `GET /api/user/story-ownerships/check/{storyId}`
    - Dùng `@AuthenticationPrincipal UserDetails`
  - Verify: Test với token JWT hợp lệ

- [ ] **6. Tạo UserPaymentController**
  - Tạo file: `src/main/java/com/example/audioai/controller/UserPaymentController.java`
  - Path: `/api/user/payment/vnpay`
  - Dependencies: `PaymentService`
  - Endpoint:
    - `POST /create-payment` (từ VnPayController, đã dùng `@AuthenticationPrincipal`)
  - Verify: Test tạo payment URL với user đã login

### Phase 5: Refactor Admin Controllers

- [ ] **7. Refactor StoryController → AdminStoryController**
  - Rename file: `StoryController.java` → `AdminStoryController.java`
  - Đổi class name thành `AdminStoryController`
  - Extend `BaseAdminController`
  - Loại bỏ method `checkAdminPermission()` riêng, dùng từ base class
  - Path giữ nguyên: `/api/admin/stories`
  - Verify: Test với admin user

- [ ] **8. Tạo AdminChapterController (tách từ ChapterController)**
  - Tạo file: `src/main/java/com/example/audioai/controller/AdminChapterController.java`
  - Path: `/api/admin/chapters`
  - Extend `BaseAdminController`
  - Dependencies: `ChapterService`
  - Chỉ chứa admin endpoints từ ChapterController (không có public endpoint)
  - Verify: Test với admin user

- [ ] **9. Refactor AdminController**
  - Sửa file: `src/main/java/com/example/audioai/controller/AdminController.java`
  - Extend `BaseAdminController`
  - Loại bỏ method `checkAdminPermission()` riêng, dùng từ base class
  - Path giữ nguyên: `/api/admin`
  - Verify: Test statistics và payments endpoints

### Phase 6: Cập nhật SecurityConfig

- [ ] **10. Cập nhật SecurityConfig.java**
  - Sửa file: `src/main/java/com/example/audioai/config/SecurityConfig.java`
  - Cập nhật `.permitAll()` matchers:
    - Auth: `/api/auth/register`, `/api/auth/login`
    - Public: `/api/public/**`, `/Data/**`
    - Options & Error: `OPTIONS /**`, `/error`
  - Verify: Compile và restart server, test toàn bộ public endpoints không cần token

### Phase 7: Xóa Controllers không dùng

- [ ] **11. Xóa các controller cũ**
  - ❌ Delete: `PublicStoryController.java` (đã merge vào PublicController)
  - ❌ Delete: `CategoryController.java` (đã merge vào PublicController)
  - ❌ Delete: `ChapterController.java` (đã tách thành AdminChapterController + public endpoint vào PublicController)
  - ❌ Delete: `StoryOwnershipController.java` (đã merge vào UserController)
  - ❌ Delete: `VnPayController.java` (đã tách thành UserPaymentController + PublicPaymentController)
  - Verify: `mvn clean compile` không có lỗi import

### Phase 8: Testing & Verification

- [ ] **12. Kiểm tra toàn bộ hệ thống**
  - Build project: `mvn clean package`
  - Run application: `mvn spring-boot:run`
  - Test matrix:
    - ✅ Public endpoints không cần token
    - ✅ Auth endpoints (register/login) không cần token
    - ✅ User endpoints cần token hợp lệ
    - ✅ Admin endpoints cần token admin
    - ✅ Invalid token trả về 401
    - ✅ Non-admin token truy cập admin endpoint trả về 403
  - Verify: Tất cả test cases pass

### Phase 9: Cập nhật Frontend (nếu cần)

- [ ] **13. Cập nhật Frontend API calls**
  - **Breaking changes** cần update trong frontend:
    - `/api/stories` → `/api/public/stories`
    - `/api/stories/latest` → `/api/public/stories/latest`
    - `/api/stories/category/{id}` → `/api/public/stories/category/{id}`
    - `/api/categories` → `/api/public/categories`
    - `/api/chapters/story/{storyId}` → `/api/public/chapters/story/{storyId}`
    - `/api/orders/purchase-story` → `/api/user/orders/purchase-story`
    - `/api/orders/my-purchases` → `/api/user/orders/my-purchases`
    - `/api/story-ownerships/check/{id}` → `/api/user/story-ownerships/check/{id}`
    - `/api/payment/vnpay/create-payment` → `/api/user/payment/vnpay/create-payment`
    - `/api/payment/vnpay/return` → `/api/public/payment/vnpay/return`
    - `/api/payment/vnpay/ipn` → `/api/public/payment/vnpay/ipn`
  - Tạo file constants cho API URLs
  - Verify: Test E2E toàn bộ flows

---

## Files Summary

### Files to CREATE (7 new files):
1. `BaseAdminController.java`
2. `PublicController.java`
3. `PublicPaymentController.java`
4. `UserController.java` (refactor từ OrderController)
5. `UserPaymentController.java`
6. `AdminStoryController.java` (rename từ StoryController)
7. `AdminChapterController.java` (split từ ChapterController)

### Files to RENAME (1 file):
1. `AuthConrtroller.java` → `AuthController.java`

### Files to MODIFY (2 files):
1. `AdminController.java` (extend BaseAdminController)
2. `SecurityConfig.java` (update permitAll paths)

### Files to DELETE (5 files):
1. `PublicStoryController.java`
2. `CategoryController.java`
3. `ChapterController.java`
4. `StoryOwnershipController.java`
5. `VnPayController.java`

---

## Service Dependencies Matrix

| Controller | Services Used |
|-----------|--------------|
| PublicController | StoryService, CategoryRepository, ChapterService |
| PublicPaymentController | PaymentService, VnPayService |
| AuthController | AuthService |
| UserController | OrderService |
| UserPaymentController | PaymentService |
| AdminStoryController | StoryService, FileStorageService |
| AdminChapterController | ChapterService |
| AdminController | AdminService |

---

## Rủi ro và Breaking Changes

### 🔴 Breaking Changes cho Frontend:
Tất cả các public và user endpoints đều đổi path prefix:
- `/api/stories/*` → `/api/public/stories/*`
- `/api/categories` → `/api/public/categories`
- `/api/chapters/story/*` → `/api/public/chapters/story/*`
- `/api/orders/*` → `/api/user/orders/*`
- `/api/story-ownerships/*` → `/api/user/story-ownerships/*`
- `/api/payment/vnpay/*` → `/api/user/payment/vnpay/*` (user) và `/api/public/payment/vnpay/*` (public)

**Giải pháp**: Cập nhật frontend cùng lúc hoặc tạo version migration (giữ cả 2 path trong 1 sprint).

### ⚠️ Rủi ro Database:
Không có - tái cấu trúc này chỉ ảnh hưởng Controller layer, không đụng Service/Repository/Entity.

### ⚠️ Rủi ro Authentication:
Thay đổi từ manual JWT extraction sang `@AuthenticationPrincipal` là **an toàn hơn** và theo best practice.

---

## Build & Test Commands

```bash
# Clean build
mvn clean compile

# Run tests (nếu có)
mvn test

# Package
mvn clean package

# Run application
mvn spring-boot:run

# Hoặc run JAR
java -jar target/audioai-0.0.1-SNAPSHOT.jar
```

---

## Kết luận

Sau khi hoàn thành tái cấu trúc này:
- ✅ Cấu trúc endpoints rõ ràng: Public (`/api/public`), User (`/api/user`), Admin (`/api/admin`), Auth (`/api/auth`)
- ✅ Loại bỏ duplicate code: `checkAdminPermission()` tập trung trong `BaseAdminController`
- ✅ Security best practice: Dùng `@AuthenticationPrincipal` thay vì manual JWT extraction
- ✅ CORS clean: Loại bỏ `@CrossOrigin` riêng lẻ, dùng global config
- ✅ Dễ maintain và scale: Thêm endpoint mới chỉ cần vào đúng controller theo role

**Estimated effort**: 4-6 hours cho backend, 2-3 hours cho frontend update.
