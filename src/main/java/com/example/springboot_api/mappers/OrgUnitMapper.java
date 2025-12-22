package com.example.springboot_api.mappers;

import org.springframework.stereotype.Component;

import com.example.springboot_api.dto.admin.orgunit.OrgUnitResponse;
import com.example.springboot_api.models.OrgUnit;

/**
 * Mapper chuyển đổi OrgUnit entities sang DTOs.
 */
@Component
public class OrgUnitMapper {

    /**
     * Convert OrgUnit entity sang OrgUnitResponse DTO.
     */
    public OrgUnitResponse toOrgUnitResponse(OrgUnit orgUnit) {
        if (orgUnit == null) {
            return null;
        }

        return OrgUnitResponse.builder()
                .id(orgUnit.getId())
                .code(orgUnit.getCode())
                .name(orgUnit.getName())
                .type(orgUnit.getType())
                .isActive(orgUnit.getIsActive())
                .createdAt(orgUnit.getCreatedAt())
                .updatedAt(orgUnit.getUpdatedAt())
                .parent(toParentInfo(orgUnit.getParent()))
                .build();
    }

    /**
     * Convert parent OrgUnit sang ParentInfo DTO (tránh circular reference).
     */
    public OrgUnitResponse.ParentInfo toParentInfo(OrgUnit parent) {
        if (parent == null) {
            return null;
        }

        return OrgUnitResponse.ParentInfo.builder()
                .id(parent.getId())
                .code(parent.getCode())
                .name(parent.getName())
                .build();
    }
}
