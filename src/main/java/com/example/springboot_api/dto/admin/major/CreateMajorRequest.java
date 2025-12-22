package com.example.springboot_api.dto.admin.major;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request tạo Major mới.
 */
@Data
public class CreateMajorRequest {

    @NotBlank(message = "Mã ngành không được để trống")
    @Size(max = 50, message = "Mã ngành không được quá 50 ký tự")
    private String code;

    @NotBlank(message = "Tên ngành không được để trống")
    @Size(max = 255, message = "Tên ngành không được quá 255 ký tự")
    private String name;

    private UUID orgUnitId; // Đơn vị tổ chức (Khoa)

    private Boolean isActive = true;
}
