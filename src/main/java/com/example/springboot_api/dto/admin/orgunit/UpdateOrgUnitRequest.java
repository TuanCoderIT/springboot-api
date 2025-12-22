package com.example.springboot_api.dto.admin.orgunit;

import java.util.UUID;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * DTO cập nhật đơn vị tổ chức.
 */
@Data
public class UpdateOrgUnitRequest {

    @Pattern(regexp = "^VINH_[A-Z0-9_]+$", message = "Mã đơn vị phải theo format: VINH_MÃ (VD: VINH_IET, VINH_SOE, VINH_FIT)")
    private String code;
    private String name;
    private String type;
    private UUID parentId;
    private Boolean isActive;
}
