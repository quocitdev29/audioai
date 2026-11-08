package com.example.audioai.service;

import com.example.audioai.config.JwtProperties;
import com.example.audioai.dto.reponse.AuthenticationResponse;
import com.example.audioai.dto.reponse.UserResponse;
import com.example.audioai.dto.requests.AuthenticationRequests;
import com.example.audioai.dto.requests.RegisterRequest;
import com.example.audioai.entity.User;
import com.example.audioai.exception.ApiException;
import com.example.audioai.repository.UserRepository;
import com.example.audioai.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Xử lý nghiệp vụ xác thực: đăng ký, đăng nhập và lấy thông tin người dùng hiện tại.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String DEFAULT_ROLE = "USER";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    /**
     * Đăng ký tài khoản mới và trả về token để đăng nhập luôn.
     */
    public AuthenticationResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "Tên đăng nhập đã tồn tại");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(DEFAULT_ROLE);
        user.setBalance(BigDecimal.ZERO);
        user.setCreatedAt(LocalDateTime.now());
        user.setHovaten(request.getHovaten() != null ? request.getHovaten().trim() : "");
        userRepository.save(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole())
                .build();

        return buildAuthenticationResponse(userDetails);
    }

    /**
     * Đăng nhập và trả về token JWT.
     * Hỗ trợ đăng nhập bằng username hoặc email.
     */
    public AuthenticationResponse login(AuthenticationRequests request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail().trim(), request.getPassword()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return buildAuthenticationResponse(userDetails);
    }

    /**
     * Lấy thông tin người dùng đang đăng nhập theo username.
     */
    public UserResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .balance(user.getBalance())
                .createdAt(user.getCreatedAt())
                .hovaten(user.getHovaten())
                .build();
    }

    private AuthenticationResponse buildAuthenticationResponse(UserDetails userDetails) {
        String token = jwtService.generateToken(userDetails);

        return AuthenticationResponse.builder()
                .token(token)
                .username(userDetails.getUsername())
                .build();
    }

    /**
     * Cập nhật thông tin cá nhân (họ và tên).
     */
    public UserResponse updateProfile(String username, String hovaten) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        user.setHovaten(hovaten.trim());
        userRepository.save(user);

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .balance(user.getBalance())
                .createdAt(user.getCreatedAt())
                .hovaten(user.getHovaten())
                .build();
    }

    /**
     * Đổi mật khẩu (phải xác minh mật khẩu cũ trước).
     */
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        // Xác minh mật khẩu hiện tại
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng");
        }

        // Kiểm tra mật khẩu mới không trùng mật khẩu cũ
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu mới không được trùng với mật khẩu cũ");
        }

        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}

