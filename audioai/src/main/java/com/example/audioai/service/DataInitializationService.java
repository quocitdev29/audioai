package com.example.audioai.service;

import com.example.audioai.entity.User;
import com.example.audioai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service khởi tạo dữ liệu ban đầu khi ứng dụng start.
 * Tự động tạo tài khoản admin nếu chưa tồn tại và backfill slug cho truyện cũ.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataInitializationService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StoryService storyService;
    private final CategoryService categoryService;

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_EMAIL = "admin@audiovn.com";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String ADMIN_HOVATEN = "Quản Trị Viên";

    @Override
    public void run(String... args) {
        initializeAdminAccount();
        backfillStorySlugs();
        backfillCategorySlugs();
    }

    /**
     * Sinh slug cho những truyện cũ chưa có slug (nâng cấp dữ liệu sang URL SEO).
     */
    private void backfillStorySlugs() {
        try {
            storyService.backfillSlugs();
        } catch (Exception e) {
            log.error("Failed to backfill story slugs", e);
        }
    }

    /**
     * Sinh slug cho những thể loại cũ chưa có slug (nâng cấp URL /category/{slug}).
     */
    private void backfillCategorySlugs() {
        try {
            categoryService.backfillSlugs();
        } catch (Exception e) {
            log.error("Failed to backfill category slugs", e);
        }
    }

    /**
     * Tạo tài khoản admin mặc định nếu chưa tồn tại.
     */
    private void initializeAdminAccount() {
        if (userRepository.existsByUsername(ADMIN_USERNAME)) {
            log.info("Admin account already exists: {}", ADMIN_USERNAME);
            return;
        }

        try {
            User admin = new User();
            admin.setUsername(ADMIN_USERNAME);
            admin.setEmail(ADMIN_EMAIL);
            admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
            admin.setRole(ADMIN_ROLE);
            admin.setHovaten(ADMIN_HOVATEN);
            admin.setBalance(BigDecimal.ZERO);
            admin.setCreatedAt(LocalDateTime.now());

            userRepository.save(admin);
            
            log.info("=".repeat(60));
            log.info("Admin account created successfully!");
            log.info("Username: {}", ADMIN_USERNAME);
            log.info("Password: {}", ADMIN_PASSWORD);
            log.info("Email: {}", ADMIN_EMAIL);
            log.info("⚠️  PLEASE CHANGE THE DEFAULT PASSWORD AFTER FIRST LOGIN!");
            log.info("=".repeat(60));
        } catch (Exception e) {
            log.error("Failed to create admin account", e);
        }
    }
}
