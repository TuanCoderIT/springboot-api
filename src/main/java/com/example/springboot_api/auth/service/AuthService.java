package com.example.springboot_api.auth.service;

import com.example.springboot_api.auth.dto.AuthResponse;
import com.example.springboot_api.auth.dto.LoginRequest;
import com.example.springboot_api.auth.dto.RegisterRequest;
import com.example.springboot_api.auth.entity.Role;
import com.example.springboot_api.auth.entity.User;
import com.example.springboot_api.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;
    private final JwtProvider jwtProvider;

    public static class AuthResult {
        public final String token;
        public final AuthResponse userInfo;

        public AuthResult(String token, AuthResponse userInfo) {
            this.token = token;
            this.userInfo = userInfo;
        }
    }

    public AuthResult register(RegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại");
        }

        User user = User.builder()
                .email(req.getEmail())
                .passwordHash(encoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .role(Role.STUDENT)
                .build();

        userRepository.save(user);

        String token = jwtProvider.generateToken(user.getId().toString());
        AuthResponse info = new AuthResponse(user.getFullName(), user.getEmail(), user.getRole());

        return new AuthResult(token, info);
    }

    public AuthResult login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Sai email hoặc mật khẩu"));

        if (!encoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Sai email hoặc mật khẩu");
        }

        String token = jwtProvider.generateToken(user.getId().toString());
        AuthResponse info = new AuthResponse(user.getFullName(), user.getEmail(), user.getRole());

        return new AuthResult(token, info);
    }
}
