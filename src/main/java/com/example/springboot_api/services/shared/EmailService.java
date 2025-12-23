package com.example.springboot_api.services.shared;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.from:noreply@university.edu.vn}")
    private String fromEmail;
    
    @Value("${app.base-url:http://localhost:8386}")
    private String baseUrl;
    
    public boolean sendNewAccountEmail(String toEmail, String fullName, String studentCode, String password) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Tài khoản hệ thống học tập đã được tạo");
            
            String emailContent = buildNewAccountEmailContent(fullName, studentCode, toEmail, password);
            message.setText(emailContent);
            
            mailSender.send(message);
            log.info("Đã gửi email tạo tài khoản thành công cho: {}", toEmail);
            return true;
            
        } catch (Exception e) {
            log.error("Lỗi gửi email cho {}: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }
    
    private String buildNewAccountEmailContent(String fullName, String studentCode, String email, String password) {
        return String.format("""
            Xin chào %s,
            
            Tài khoản hệ thống học tập của bạn đã được tạo thành công.
            
            Thông tin đăng nhập:
            - Email: %s
            - Mật khẩu: %s
            - MSSV: %s
            
            Vui lòng truy cập hệ thống tại: %s
            
            LƯU Ý QUAN TRỌNG:
            - Vui lòng đổi mật khẩu ngay sau lần đăng nhập đầu tiên
            - Không chia sẻ thông tin đăng nhập với người khác
            - Liên hệ giảng viên nếu có vấn đề về tài khoản
            
            Trân trọng,
            Hệ thống quản lý học tập
            """, fullName, email, password, studentCode, baseUrl);
    }
}