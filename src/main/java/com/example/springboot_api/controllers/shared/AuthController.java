package com.example.springboot_api.controllers.shared;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.config.security.JwtAuthenticationFilter;
import com.example.springboot_api.config.security.UserPrincipal;
import com.example.springboot_api.dto.shared.auth.AuthResponse;
import com.example.springboot_api.dto.shared.auth.LoginRequest;
import com.example.springboot_api.dto.shared.auth.RegisterRequest;
import com.example.springboot_api.dto.user.profile.UpdateProfileRequest;
import com.example.springboot_api.services.shared.AuthService;
import com.example.springboot_api.services.user.ProfileService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ProfileService profileService;

    private ResponseCookie makeCookie(String token) {
        return ResponseCookie.from(JwtAuthenticationFilter.AUTH_COOKIE, token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
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
    public ResponseEntity<AuthResponse> register(@org.springframework.web.bind.annotation.RequestBody RegisterRequest req,
            HttpServletResponse res) {

        var result = authService.register(req);
        res.addHeader("Set-Cookie", makeCookie(result.token).toString());
        return ResponseEntity.ok(result.userInfo);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@org.springframework.web.bind.annotation.RequestBody LoginRequest req,
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
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal UserPrincipal user) {
        if (user == null)
            return ResponseEntity.status(401).build();

        AuthResponse info = profileService.getProfile(user.getId());
        return ResponseEntity.ok(info);
    }

    @PutMapping(value = "/profile", consumes = {"multipart/form-data"})
    public ResponseEntity<AuthResponse> updateProfile(
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            @AuthenticationPrincipal UserPrincipal user) {

        if (user == null)
            return ResponseEntity.status(401).build();

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName(fullName);

        AuthResponse response = profileService.updateProfile(user.getId(), req, avatar);
        return ResponseEntity.ok(response);
    }
}

