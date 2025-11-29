package com.example.springboot_api.dto.shared.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

import com.example.springboot_api.models.Role;

@Data
@AllArgsConstructor
public class AuthResponse {
    private UUID id;
    private String fullName;
    private String email;
    private Role role;
    private String avatarUrl;
}
