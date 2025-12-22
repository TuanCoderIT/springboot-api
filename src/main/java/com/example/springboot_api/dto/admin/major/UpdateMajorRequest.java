package com.example.springboot_api.dto.admin.major;

import java.util.UUID;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request cập nhật Major.
 */
@Data
public class UpdateMajorRequest {

    @Size(max = 50, message = "Mã ngành không được quá 50 ký tự")
    private String code;

    @Size(max = 255, message = "Tên ngành không được quá 255 ký tự")
    private String name;

    private UUID orgUnitId;

    private Boolean isActive;
}
