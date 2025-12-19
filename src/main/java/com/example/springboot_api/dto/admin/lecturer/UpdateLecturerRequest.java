package com.example.springboot_api.dto.admin.lecturer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateLecturerRequest {

    @Email(message = "Email không hợp lệ")
    private String email;

    @Size(max = 255, message = "Họ tên không được quá 255 ký tự")
    private String fullName;

    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    private String avatarUrl;

    private Boolean active;
}
