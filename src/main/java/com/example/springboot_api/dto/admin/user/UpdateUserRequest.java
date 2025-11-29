package com.example.springboot_api.dto.admin.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
  @Size(max = 255, message = "Họ tên không được vượt quá 255 ký tự")
  private String fullName;

  @Email(message = "Email không hợp lệ")
  @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
  private String email;
}
