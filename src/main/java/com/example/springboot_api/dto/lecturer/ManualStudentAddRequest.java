package com.example.springboot_api.dto.lecturer;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class ManualStudentAddRequest {
    
    @NotNull(message = "ID lớp học phần không được để trống")
    private UUID classId;
    
    @NotBlank(message = "MSSV không được để trống")
    private String studentCode;
    
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;
    
    private LocalDate dateOfBirth;
    
    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    private String email;
}