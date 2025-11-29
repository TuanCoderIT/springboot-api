package com.example.springboot_api.dto.shared.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class AuthResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String role;
    private String avatarUrl;
}
