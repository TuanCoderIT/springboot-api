package com.example.springboot_api.dto.admin.notebook;

import java.util.UUID;

public record ContributorInfo(
        UUID id,
        String fullName,
        String email,
        String avatarUrl,
        Long filesCount) {
}
