package com.example.springboot_api.dto.admin.lecturer;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public class LecturerResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String role;
    private Boolean active;
    private String avatarUrl;
    private Instant createdAt;
    private Instant updatedAt;
}
