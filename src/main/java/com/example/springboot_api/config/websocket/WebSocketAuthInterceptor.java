package com.example.springboot_api.config.websocket;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.server.ServerHttpRequest;

import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import com.example.springboot_api.config.security.JwtProvider;
import com.example.springboot_api.config.security.UserPrincipal;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.shared.AuthRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class WebSocketAuthInterceptor extends DefaultHandshakeHandler {

    private final JwtProvider jwtProvider;
    private final AuthRepository userRepository;

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        String query = request.getURI().getQuery();
        String token = null;
        String roleName = null;

        // Parse query parameters
        if (query != null) {
            // Hỗ trợ cả access_token (format mới) và token (format cũ)
            if (query.contains("access_token=")) {
                token = extractQueryParam(query, "access_token");
            } else if (query.contains("token=")) {
                token = extractQueryParam(query, "token");
            }


            // Lấy role_name nếu có
            if (query.contains("role_name=")) {
                roleName = extractQueryParam(query, "role_name");
            }
        }

        // Nếu không có token trong query, thử Authorization header
        if (token == null) {
            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        // Validate token và tạo Principal
        if (token != null && jwtProvider.validateToken(token)) {
            String userId = jwtProvider.extractUserId(token);
            User user = userRepository.findById(UUID.fromString(userId)).orElse(null);
            if (user != null) {
                // Validate role_name nếu được cung cấp
                if (roleName != null && !roleName.isEmpty() && !roleName.equalsIgnoreCase(user.getRole())) {
                    log.warn("Role mismatch: expected {}, got {}", roleName, user.getRole());
                    return null;

                }

                UserPrincipal principal = new UserPrincipal(user,
                        java.util.List.of(() -> "ROLE_" + user.getRole()));
                return principal;
            }
        }

        return null;
    }

    /**
     * Extract query parameter value from query string
     */
    private String extractQueryParam(String query, String paramName) {
        String param = paramName + "=";
        if (query.contains(param)) {
            int startIndex = query.indexOf(param) + param.length();
            int endIndex = query.indexOf("&", startIndex);
            if (endIndex == -1) {
                endIndex = query.length();
            }
            String value = query.substring(startIndex, endIndex);
            try {
                // Decode URL encoding
                return java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                return value;
            }
        }
        return null;
    }
}
