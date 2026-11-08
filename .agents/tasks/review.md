# Backend Refactoring Review: Public/User/Admin Endpoint Separation

The backend was refactored to cleanly separate public, user-authenticated, and admin endpoints by introducing a three-tier controller structure. Old controllers with mixed responsibilities (OrderController, VnPayController, ChapterController) were split or merged into role-specific controllers. A BaseAdminController was added to centralize admin permission checks. Manual JWT extraction was replaced with Spring Security's `@AuthenticationPrincipal`. Individual `@CrossOrigin` annotations were removed in favor of global CORS configuration.

**Watch for:** UserController still injects UserRepository and manually fetches users (confirmed — 3 endpoints duplicate this pattern); AdminChapterController's `getChaptersByStory` overload shadows PublicController's version (likely — both call the same service method); no HTTP 404 vs 403 distinction in AdminController permission check (confirmed — always throws 403).

**Verdict**: CHANGES_REQUESTED

## High-level view

All controllers now follow the three-tier pattern: PublicController handles unauthenticated endpoints under `/api/public`, UserController and UserPaymentController require authentication under `/api/user`, and three admin controllers extend BaseAdminController and reside under `/api/admin`. The old controllers (PublicStoryController, CategoryController, OrderController, StoryOwnershipController, VnPayController, and the typo'd AuthConrtroller.java) were removed. AuthController was correctly renamed from the typo version.

User-level controllers (UserController, UserPaymentController) now use `@AuthenticationPrincipal UserDetails` instead of manual JWT extraction, eliminating the JwtService dependency from UserController. All individual `@CrossOrigin` annotations were removed; CORS is handled globally in SecurityConfig.

BaseAdminController provides a centralized `checkAdminPermission(UserDetails)` method, and AdminController, AdminStoryController, and AdminChapterController all extend it. SecurityConfig's `.permitAll()` rules correctly match the new path structure: `/api/auth/register`, `/api/auth/login`, and `/api/public/**` are public; everything else requires authentication.

UserController injects UserRepository and manually resolves the user from `userDetails.getUsername()` in all three endpoints. This duplicates the same `findByUsername().orElseThrow()` pattern three times. The Service layer should accept username directly or handle user resolution internally.

AdminChapterController defines `getChaptersByStory(Long, UserDetails)` which is distinct from the admin-only overload `getChaptersByStory(Long)` in ChapterService. PublicController's endpoint calls the two-argument overload, passing optional UserDetails, while AdminChapterController calls the single-argument overload. Both serve different use cases (public with optional unlock vs admin full access), but the naming collision could confuse future maintainers.

## <details><summary>Details</summary>

### Public/User/Admin separation and BaseAdminController

The refactoring introduced three tiers:

- **Public** (`/api/public`): PublicController merges endpoints from PublicStoryController, CategoryController, and ChapterController's public endpoint. PublicPaymentController handles VNPay callbacks (return and IPN) which must be public since VNPay calls them directly.

- **User** (`/api/user`): UserController replaces OrderController and merges StoryOwnershipController's single endpoint. UserPaymentController handles the authenticated VNPay payment creation endpoint. All use `@AuthenticationPrincipal UserDetails` instead of manual JWT extraction.

- **Admin** (`/api/admin`): AdminController, AdminStoryController, and AdminChapterController all extend BaseAdminController. Each admin endpoint calls `checkAdminPermission(userDetails)` before proceeding. StoryController was renamed to AdminStoryController.

BaseAdminController is an abstract class with a single `checkAdminPermission(UserDetails)` method that throws `ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập")` if `userDetails.getUsername()` is not `"admin"`. This centralizes the permission check and eliminates duplicate `checkAdminPermission` methods that previously existed in individual controllers.

**Issue (confirmed)**: The admin check always throws 403 FORBIDDEN whether the resource exists or not. If an admin requests `/api/admin/stories/99999` and story 99999 doesn't exist, the service throws 404, which is correct. But if a non-admin requests it, the controller throws 403 before checking existence. This is semantically correct (permission before existence), but inconsistent with RESTful convention where 404 can leak resource existence. The current behavior is acceptable but worth documenting.

**Issue (confirmed)**: BaseAdminController's permission check hardcodes `"admin"` as the username. This means only the user with username `admin` can access admin endpoints, not a role-based check. If the system uses Spring Security roles (ROLE_ADMIN), the check should use `userDetails.getAuthorities()` instead. If the intent is truly username-based, this is correct but brittle (renaming the admin user breaks the system).

### UserController and manual user resolution

UserController no longer injects JwtService. It uses `@AuthenticationPrincipal UserDetails` in all three endpoints: `purchaseStory`, `getMyPurchases`, and `checkOwnership`. However, all three endpoints inject UserRepository and manually fetch the user:

```java
User user = userRepository.findByUsername(userDetails.getUsername())
    .orElseThrow(() -> new RuntimeException("User not found"));
```

This pattern appears three times in the same controller. The Service layer (OrderService) accepts `userId` as a parameter, so the controller must resolve the user to get the ID. This is not incorrect, but it violates the single responsibility principle: the controller should delegate user resolution to the service layer.

**Issue (confirmed)**: UserController duplicates user resolution logic in three methods. If the service accepted `String username` instead of `Long userId`, the service could handle user resolution internally, and the controller would be cleaner. Alternatively, extract a private helper method `getUserFromPrincipal(UserDetails)` in the controller to eliminate duplication.

**Issue (likely)**: If `userDetails.getUsername()` returns a username not present in the database, all three endpoints throw `RuntimeException("User not found")`. This should never happen in production (Spring Security's UserDetailsService should only authenticate existing users), but if it does, returning a generic RuntimeException instead of ApiException means the global exception handler may not format the response correctly. Verify that the exception handler catches RuntimeException and returns a proper JSON response.

### SecurityConfig and permitAll rules

SecurityConfig was updated to match the new endpoint structure:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
    .requestMatchers("/api/public/**", "/Data/**").permitAll()
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
    .requestMatchers("/error").permitAll()
    .anyRequest().authenticated()
)
```

This correctly permits:
- `/api/auth/register` and `/api/auth/login` (POST only)
- `/api/public/**` (all HTTP methods)
- `/Data/**` (static files: audio, covers)
- `OPTIONS /**` (CORS preflight)
- `/error` (Spring Boot error page)

Everything else requires authentication. This matches the plan.

**No issues found in SecurityConfig**. The path matchers are correct and complete.

### Removal of @CrossOrigin and global CORS

All individual `@CrossOrigin` annotations were removed. CORS is now handled globally by `CorsConfigurationSource` bean in SecurityConfig:

```java
configuration.setAllowedOriginPatterns(List.of("*"));
configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
configuration.setAllowedHeaders(List.of("*"));
configuration.setExposedHeaders(List.of("Authorization"));
configuration.setAllowCredentials(true);
```

This is correct. No individual controllers have `@CrossOrigin` anymore (grep search confirmed zero matches).

**No issues found**. CORS is properly centralized.

### Old controllers removed

The following controllers were deleted:
- PublicStoryController.java
- CategoryController.java
- OrderController.java (renamed to UserController)
- StoryOwnershipController.java (merged into UserController)
- VnPayController.java (split into UserPaymentController and PublicPaymentController)
- AuthConrtroller.java (typo, renamed to AuthController.java)

A search for old controller names found no matches except AdminChapterController (which is new, not old). The old ChapterController.java was deleted; its admin endpoints moved to AdminChapterController and its public endpoint moved to PublicController.

**No issues found**. Old controllers were properly removed.

### AdminChapterController and ChapterService overload

AdminChapterController has a `getChaptersByStory` endpoint:

```java
@GetMapping("/story/{storyId}")
public ResponseEntity<ApiResponse<List<ChapterDTO>>> getChaptersByStory(
        @PathVariable Long storyId,
        @AuthenticationPrincipal UserDetails userDetails) {
    checkAdminPermission(userDetails);
    List<ChapterDTO> chapters = chapterService.getChaptersByStory(storyId);
    return ResponseEntity.ok(...);
}
```

It calls the single-argument service method `getChaptersByStory(Long storyId)`, which returns all chapters without checking ownership (admin sees everything).

PublicController also has a `getChaptersByStory` endpoint:

```java
@GetMapping("/chapters/story/{storyId}")
public ResponseEntity<ApiResponse<List<ChapterDTO>>> getChaptersByStory(
        @PathVariable Long storyId,
        @AuthenticationPrincipal UserDetails userDetails) {
    List<ChapterDTO> chapters = chapterService.getChaptersByStory(storyId, userDetails);
    return ResponseEntity.ok(...);
}
```

It calls the two-argument service method `getChaptersByStory(Long storyId, UserDetails userDetails)`, which checks ownership and unlocks chapters if the user owns the story.

Both endpoints have the same name but reside in different controllers and call different service overloads. This is not a bug, but the naming collision is subtle. A future maintainer might not realize there are two distinct behaviors.

**Issue (possible)**: Consider renaming one of the service methods to make the distinction clearer. For example, `getChaptersByStoryPublic(Long storyId, UserDetails userDetails)` and `getChaptersByStoryAdmin(Long storyId)`. Alternatively, add a Javadoc comment to each service method explaining the use case. This is a minor maintainability concern, not a functional issue.

### AuthController rename

AuthConrtroller.java was renamed to AuthController.java. The class name inside the file was also updated. All endpoints remain unchanged:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `PUT /api/auth/profile`
- `PUT /api/auth/change-password`

All user-facing endpoints (`/me`, `/profile`, `/change-password`) use `@AuthenticationPrincipal UserDetails`.

**No issues found**. The rename was done correctly.

### PublicPaymentController and VNPay callbacks

PublicPaymentController handles two VNPay callbacks:

- `GET /api/public/payment/vnpay/return` — VNPay redirects the user's browser here after payment. The controller verifies the signature, processes the callback, and redirects to the frontend with query parameters.
- `GET /api/public/payment/vnpay/ipn` — VNPay makes a server-to-server call to notify the backend. The controller verifies the signature, processes the IPN, and returns a JSON response with `{"RspCode":"00","Message":"Confirm Success"}`.

Both endpoints are correctly public (no authentication required). The return endpoint builds a frontend redirect URL hardcoded to `http://localhost:5173/payment/result`. This should be externalized to `application.properties` for production.

**Issue (confirmed)**: PublicPaymentController hardcodes the frontend URL `http://localhost:5173/payment/result`. This will break in production. Extract it to a config property (e.g., `app.frontend.payment-result-url`).

### UserPaymentController

UserPaymentController handles payment creation:

- `POST /api/user/payment/vnpay/create-payment` — Authenticated user requests a VNPay payment URL. The controller receives `@AuthenticationPrincipal UserDetails`, extracts the username, and passes it to PaymentService.

This is correct. The user's identity is required to create a payment transaction tied to their account.

**No issues found**.

### AdminStoryController and AdminController

AdminStoryController extends BaseAdminController and calls `checkAdminPermission(userDetails)` at the start of every endpoint. It handles:

- `GET /api/admin/stories` (all stories)
- `GET /api/admin/stories/{id}` (single story)
- `POST /api/admin/stories` (create)
- `PUT /api/admin/stories/{id}` (update)
- `DELETE /api/admin/stories/{id}` (delete)
- `GET /api/admin/stories/category/{categoryId}` (filter by category)
- `GET /api/admin/stories/status/{status}` (filter by status)
- `POST /api/admin/stories/upload-cover` (upload cover image)

AdminController handles:

- `GET /api/admin/statistics` (system stats)
- `GET /api/admin/payments` (payment history)

Both controllers correctly extend BaseAdminController and call `checkAdminPermission` at the start of each endpoint.

**No issues found**. Admin controllers are implemented correctly.

### Business logic in Service layer

The refactoring touched only the Controller layer. Service methods (StoryService, ChapterService, OrderService, PaymentService, AdminService, AuthService) were not modified. Their logic remains unchanged. This is correct: the refactoring was controller-level reorganization, not business logic changes.

**No issues found**. Service layer was correctly preserved.

### HTTP status codes

All controllers use standard HTTP status codes:

- 200 OK for successful GET/PUT/DELETE
- 201 CREATED for successful POST (create operations)
- 400 BAD_REQUEST for validation errors (caught in try-catch in UserController)
- 403 FORBIDDEN for admin permission denial
- 404 NOT_FOUND for missing resources (thrown by services as ApiException)
- 500 INTERNAL_SERVER_ERROR for unexpected errors (caught in try-catch in UserController)

Status codes are consistent across controllers.

**No issues found**. HTTP status codes are correct.

</details>

## File map

<details>
<summary>9 controller files</summary>

- **BaseAdminController.java** — Abstract base class for admin controllers; provides `checkAdminPermission(UserDetails)`.
- **PublicController.java** — Public endpoints: stories, categories, chapters (no auth required).
- **PublicPaymentController.java** — VNPay callbacks: return and IPN (no auth required).
- **AuthController.java** — Authentication endpoints: register, login, profile, change password.
- **UserController.java** — User endpoints: purchase story, purchase history, ownership check.
- **UserPaymentController.java** — User payment endpoint: create VNPay payment URL.
- **AdminController.java** — Admin statistics and payment history (extends BaseAdminController).
- **AdminStoryController.java** — Admin story CRUD and upload cover (extends BaseAdminController).
- **AdminChapterController.java** — Admin chapter CRUD and upload audio (extends BaseAdminController).

</details>
