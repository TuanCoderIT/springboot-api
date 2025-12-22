package com.example.springboot_api.dto.admin.lecturer;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/**
 * DTO chứa thông tin tóm tắt của đơn vị tổ chức (OrgUnit).
 */
@Data
@Builder
public class OrgUnitInfo {
    private UUID id;
    private String code;
    private String name;
    private String type;
}
