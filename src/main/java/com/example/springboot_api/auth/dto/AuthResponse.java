package com.example.springboot_api.auth.dto;

import com.example.springboot_api.auth.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String fullName;
    private String email;
    private Role role;
}
