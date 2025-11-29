package com.example.springboot_api.dto.shared.auth;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
