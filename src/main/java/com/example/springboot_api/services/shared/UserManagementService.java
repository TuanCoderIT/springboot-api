package com.example.springboot_api.services.shared;

import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    /**
     * Tìm hoặc tạo user cho sinh viên
     * @param email Email sinh viên
     * @param studentCode MSSV
     * @param fullName Họ tên
     * @return UserCreationResult chứa thông tin user và trạng thái tạo mới
     */
    @Transactional
    public UserCreationResult findOrCreateStudentUser(String email, String studentCode, String fullName) {
        try {
            // Kiểm tra user đã tồn tại theo email
            Optional<User> existingUser = userRepository.findByEmail(email);
            
            if (existingUser.isPresent()) {
                User user = existingUser.get();
                log.info("User đã tồn tại với email: {}", email);
                
                // Cập nhật student code nếu chưa có
                if (user.getStudentCode() == null || user.getStudentCode().isEmpty()) {
                    user.setStudentCode(studentCode);
                    user.setUpdatedAt(Instant.now());
                    userRepository.save(user);
                    log.info("Đã cập nhật student code {} cho user {}", studentCode, email);
                }
                
                return UserCreationResult.builder()
                        .user(user)
                        .isNewUser(false)
                        .emailSent(false)
                        .password(null)
                        .build();
            }
            
            // Tạo user mới
            String randomPassword = generateRandomPassword();
            String hashedPassword = passwordEncoder.encode(randomPassword);
            
            User newUser = User.builder()
                    .email(email)
                    .passwordHash(hashedPassword)
                    .fullName(fullName)
                    .studentCode(studentCode)
                    .role("STUDENT")  // Sửa từ "student" thành "STUDENT"
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            
            newUser = userRepository.save(newUser);
            log.info("Đã tạo user mới cho sinh viên: {} - {}", studentCode, email);
            
            // Gửi email thông báo
            boolean emailSent = emailService.sendNewAccountEmail(email, fullName, studentCode, randomPassword);
            
            return UserCreationResult.builder()
                    .user(newUser)
                    .isNewUser(true)
                    .emailSent(emailSent)
                    .password(randomPassword)
                    .build();
            
        } catch (Exception e) {
            log.error("Lỗi tạo/tìm user cho sinh viên {} - {}: {}", studentCode, email, e.getMessage(), e);
            throw new RuntimeException("Lỗi xử lý tài khoản sinh viên: " + e.getMessage());
        }
    }
    
    private String generateRandomPassword() {
        // Tạo mật khẩu ngẫu nhiên 8 ký tự
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        
        for (int i = 0; i < 8; i++) {
            int index = (int) (Math.random() * chars.length());
            password.append(chars.charAt(index));
        }
        
        return password.toString();
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserCreationResult {
        private User user;
        private boolean isNewUser;
        private boolean emailSent;
        private String password;
    }
}