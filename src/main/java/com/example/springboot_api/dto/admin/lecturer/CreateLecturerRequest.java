package com.example.springboot_api.dto.admin.lecturer;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateLecturerRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 255, message = "Họ tên không được quá 255 ký tự")
    private String fullName;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    private String avatarUrl;

    // === TeacherProfile fields ===
    @NotBlank(message = "Mã giảng viên không được để trống")
    private String lecturerCode; // Mã giảng viên (unique, required)

    private UUID orgUnitId; // ID đơn vị tổ chức chính

    private String academicDegree; // Học vị (ThS, TS, PGS.TS...)
    private String academicRank; // Học hàm (PGS, GS...)
    private String specialization; // Chuyên ngành
    private String phone; // Số điện thoại
}
