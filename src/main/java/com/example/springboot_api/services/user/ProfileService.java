package com.example.springboot_api.services.user;

import java.io.IOException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.shared.auth.AuthResponse;
import com.example.springboot_api.dto.user.profile.UpdateProfileRequest;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.shared.AuthRepository;
import com.example.springboot_api.services.shared.FileStorageService;
import com.example.springboot_api.utils.UrlNormalizer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final AuthRepository userRepository;
    private final FileStorageService fileStorageService;
    private final UrlNormalizer urlNormalizer;

    @Transactional
    public AuthResponse updateProfile(UUID userId, UpdateProfileRequest req, MultipartFile avatar) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));

        if (req.getFullName() != null && !req.getFullName().trim().isEmpty()) {
            user.setFullName(req.getFullName().trim());
        }

        if (avatar != null && !avatar.isEmpty()) {
            try {
                if (user.getAvatarUrl() != null) {
                    fileStorageService.deleteFile(user.getAvatarUrl());
                }

                String avatarUrl = fileStorageService.storeFile(avatar);
                user.setAvatarUrl(avatarUrl);
            } catch (IOException e) {
                throw new RuntimeException("Không thể upload avatar", e);
            }
        }

        user.setUpdatedAt(java.time.Instant.now());
        userRepository.save(user);

        String avatarUrl = urlNormalizer.normalizeToFull(user.getAvatarUrl());
        return new AuthResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(), avatarUrl);
    }

    public AuthResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));

        String avatarUrl = urlNormalizer.normalizeToFull(user.getAvatarUrl());
        return new AuthResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(), avatarUrl);
    }

}
