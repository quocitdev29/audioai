package com.example.audioai.service;

import com.example.audioai.entity.User;
import com.example.audioai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Nạp thông tin người dùng từ database cho Spring Security.
 * Hỗ trợ đăng nhập bằng username hoặc email.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = findUserByUsernameOrEmail(usernameOrEmail);

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole())
                .build();
    }

    /**
     * Tìm người dùng theo username hoặc email.
     * Ưu tiên tìm theo username trước, nếu không có thì tìm theo email.
     */
    private User findUserByUsernameOrEmail(String usernameOrEmail) {
        String trimmedInput = usernameOrEmail.trim();
        
        // Thử tìm theo username trước
        return userRepository.findByUsername(trimmedInput)
                .or(() -> userRepository.findByEmail(trimmedInput.toLowerCase()))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Không tìm thấy người dùng với username hoặc email: " + trimmedInput));
    }
}
