package com.example.springboot_api.mappers;

import org.springframework.stereotype.Component;

import com.example.springboot_api.dto.shared.auth.AuthResponse;
import com.example.springboot_api.models.User;
import com.example.springboot_api.utils.UrlNormalizer;

import lombok.RequiredArgsConstructor;

/**
 * Mapper chuyển đổi User entities sang DTOs.
 */
@Component
@RequiredArgsConstructor
public class UserMapper {

    private final UrlNormalizer urlNormalizer;

    /**
     * Convert User entity sang AuthResponse DTO.
     */
    public AuthResponse toAuthResponse(User user) {
        if (user == null) {
            return null;
        }

        String avatarUrl = urlNormalizer.normalizeToFull(user.getAvatarUrl());

        return new AuthResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                avatarUrl);
    }
}
