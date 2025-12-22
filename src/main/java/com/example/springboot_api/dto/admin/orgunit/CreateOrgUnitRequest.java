package com.example.springboot_api.dto.admin.orgunit;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * DTO tạo đơn vị tổ chức mới.
 */
@Data
public class CreateOrgUnitRequest {

    @NotBlank(message = "Mã đơn vị không được để trống")
    @Pattern(regexp = "^VINH_[A-Z0-9_]+$", message = "Mã đơn vị phải theo format: VINH_MÃ (VD: VINH_IET, VINH_SOE, VINH_FIT)")
    private String code;

    @NotBlank(message = "Tên đơn vị không được để trống")
    private String name;

    private String type; // Loại: faculty, department, center...
    private UUID parentId; // ID đơn vị cha (nếu có)
    private Boolean isActive; // Trạng thái hoạt động
}
