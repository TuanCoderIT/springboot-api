package com.example.springboot_api.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    @Override
    public UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // TODO: cast sang UserDetails/CustomPrincipal của bạn để lấy ID
        // Ví dụ:
        // CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        // return user.getId();
        throw new UnsupportedOperationException("Implement getCurrentUserId() theo cấu trúc security của bạn");
    }
}
