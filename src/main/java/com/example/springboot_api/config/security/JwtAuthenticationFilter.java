package com.example.springboot_api.config.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.shared.AuthRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_COOKIE = "AUTH-TOKEN";

    private final JwtProvider jwtProvider;
    private final AuthRepository userRepository;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, AuthRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && jwtProvider.validateToken(token)) {
            String userId = jwtProvider.extractUserId(token);

            Optional<User> opt = userRepository.findById(UUID.fromString(userId));

            if (opt.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {

                User user = opt.get();


                // ⭐ CHỈNH LẠI NGAY TẠI ĐÂY
                UserPrincipal principal = new UserPrincipal(
                        user,
                        List.of(() -> "ROLE_" + user.getRole()) // <-- chuẩn Spring Security
                );


                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities());

                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {

        // Check cookie
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (AUTH_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // Fallback header
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        return null;
    }
}
