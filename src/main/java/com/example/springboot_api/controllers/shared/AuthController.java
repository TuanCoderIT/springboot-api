package com.example.springboot_api.controllers.shared;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.config.security.JwtAuthenticationFilter;
import com.example.springboot_api.dto.shared.auth.AuthResponse;
import com.example.springboot_api.dto.shared.auth.LoginRequest;
import com.example.springboot_api.dto.shared.auth.RegisterRequest;
import com.example.springboot_api.models.User;
import com.example.springboot_api.services.shared.AuthService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private ResponseCookie makeCookie(String token) {
        return ResponseCookie.from(JwtAuthenticationFilter.AUTH_COOKIE, token)
                .httpOnly(true)
                .secure(false) // set true khi dùng HTTPS
                .path("/")
                .sameSite("Lax") // FE khác domain thì dùng "None"
                .maxAge(24 * 60 * 60)
                .build();
    }

    private ResponseCookie clearCookie() {
        return ResponseCookie.from(JwtAuthenticationFilter.AUTH_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest req,
            HttpServletResponse res) {

        var result = authService.register(req);
        res.addHeader("Set-Cookie", makeCookie(result.token).toString());
        return ResponseEntity.ok(result.userInfo);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req,
            HttpServletResponse res) {

        var result = authService.login(req);
        res.addHeader("Set-Cookie", makeCookie(result.token).toString());
        return ResponseEntity.ok(result.userInfo);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse res) {
        res.addHeader("Set-Cookie", clearCookie().toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal User user) {
        if (user == null)
            return ResponseEntity.status(401).build();

        AuthResponse info = new AuthResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                user.getAvatarUrl());
        return ResponseEntity.ok(info);
    }
}