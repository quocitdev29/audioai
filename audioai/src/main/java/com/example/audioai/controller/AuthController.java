package com.example.audioai.controller;

import com.example.audioai.dto.reponse.ApiResponse;
import com.example.audioai.dto.reponse.AuthenticationResponse;
import com.example.audioai.dto.reponse.UserResponse;
import com.example.audioai.dto.requests.AuthenticationRequests;
import com.example.audioai.dto.requests.ChangePasswordRequest;
import com.example.audioai.dto.requests.RegisterRequest;
import com.example.audioai.dto.requests.UpdateProfileRequest;
import com.example.audioai.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REGISTER_SUCCESS_MESSAGE = "Đăng ký thành công";
    private static final String LOGIN_SUCCESS_MESSAGE = "Đăng nhập thành công";
    private static final String GET_PROFILE_SUCCESS_MESSAGE = "Lấy thông tin người dùng thành công";

    private final AuthService authService;

    /**
     * POST /api/auth/register - Đăng ký tài khoản mới.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthenticationResponse data = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, REGISTER_SUCCESS_MESSAGE, data));
    }

    /**
     * POST /api/auth/login - Đăng nhập, nhận token JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> login(@Valid @RequestBody AuthenticationRequests request) {
        AuthenticationResponse data = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(LOGIN_SUCCESS_MESSAGE, data));
    }

    /**
     * GET /api/auth/me - Lấy thông tin người dùng đang đăng nhập (yêu cầu Bearer token).
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponse data = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(GET_PROFILE_SUCCESS_MESSAGE, data));
    }

    /**
     * PUT /api/auth/profile - Cập nhật thông tin cá nhân (họ và tên).
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse data = authService.updateProfile(userDetails.getUsername(), request.getHovaten());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin thành công", data));
    }

    /**
     * PUT /api/auth/change-password - Đổi mật khẩu (yêu cầu xác minh mật khẩu cũ).
     */
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(
                userDetails.getUsername(),
                request.getCurrentPassword(),
                request.getNewPassword()
        );
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công", null));
    }
}
