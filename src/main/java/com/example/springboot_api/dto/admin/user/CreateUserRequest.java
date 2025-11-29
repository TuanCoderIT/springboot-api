package com.example.springboot_api.dto.admin.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequest {
  @NotBlank(message = "Họ tên không được để trống")
  @Size(max = 255, message = "Họ tên không được vượt quá 255 ký tự")
  private String fullName;

  @NotBlank(message = "Email không được để trống")
  @Email(message = "Email không hợp lệ")
  @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
  private String email;

  @NotBlank(message = "Mật khẩu không được để trống")
  @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
  @Size(max = 100, message = "Mật khẩu không được vượt quá 100 ký tự")
  private String password;
}
