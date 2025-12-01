package com.example.springboot_api.dto.user.profile;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(max = 255, message = "Họ tên không được vượt quá 255 ký tự")
    private String fullName;
}

