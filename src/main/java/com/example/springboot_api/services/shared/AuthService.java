package com.example.springboot_api.services.shared;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.springboot_api.common.exceptions.ConflictException;
import com.example.springboot_api.common.exceptions.UnauthorizedException;
import com.example.springboot_api.config.security.JwtProvider;
import com.example.springboot_api.dto.shared.auth.AuthResponse;
import com.example.springboot_api.dto.shared.auth.LoginRequest;
import com.example.springboot_api.dto.shared.auth.RegisterRequest;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.shared.AuthRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthRepository userRepository;
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
            throw new ConflictException("Email đã tồn tại");
        }

        User user = User.builder()
                .email(req.getEmail())
                .passwordHash(encoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .role("STUDENT")
                .build();

        userRepository.save(user);

        String token = jwtProvider.generateToken(user.getId().toString(), user.getRole());
        AuthResponse info = new AuthResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                user.getAvatarUrl());

        return new AuthResult(token, info);
    }

    public AuthResult login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Sai email hoặc mật khẩu"));

        if (!encoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Sai email hoặc mật khẩu");
        }

        String token = jwtProvider.generateToken(user.getId().toString(), user.getRole());
        AuthResponse info = new AuthResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                user.getAvatarUrl());

        return new AuthResult(token, info);
    }
}
