package com.example.springboot_api.dto.admin.orgunit;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/**
 * DTO trả về thông tin đơn vị tổ chức.
 */
@Data
@Builder
public class OrgUnitResponse {
    private UUID id;
    private String code;
    private String name;
    private String type;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Parent info (tránh circular reference)
    private ParentInfo parent;

    @Data
    @Builder
    public static class ParentInfo {
        private UUID id;
        private String code;
        private String name;
    }
}
