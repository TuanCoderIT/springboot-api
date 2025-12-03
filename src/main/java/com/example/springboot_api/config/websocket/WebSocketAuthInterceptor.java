package com.example.springboot_api.config.websocket;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String query = request.getURI().getQuery();
        if (query != null && query.contains("token=")) {
            String token = query.substring(query.indexOf("token=") + 6);
            if (token.contains("&")) {
                token = token.substring(0, token.indexOf("&"));
            }
            
            if (jwtProvider.validateToken(token)) {
                String userId = jwtProvider.extractUserId(token);
                User user = userRepository.findById(UUID.fromString(userId)).orElse(null);
                if (user != null) {
                    String role = "ROLE_" + user.getRole();
                    UserPrincipal principal = new UserPrincipal(user, 
                        java.util.List.of(new SimpleGrantedAuthority(role)));
                    return principal;
                }
            }
        }
        
        // Try Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtProvider.validateToken(token)) {
                String userId = jwtProvider.extractUserId(token);
                User user = userRepository.findById(UUID.fromString(userId)).orElse(null);
                if (user != null) {
                    String role = "ROLE_" + user.getRole();
                    UserPrincipal principal = new UserPrincipal(user, 
                        java.util.List.of(new SimpleGrantedAuthority(role)));
                    return principal;
                }
            }
        }
        
        return null;
    }
}

