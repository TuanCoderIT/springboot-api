package com.example.springboot_api.modules.auth.dto;

import com.example.springboot_api.modules.auth.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String fullName;
    private String email;
    private Role role;
}
