package com.example.springboot_api.dto.admin.major;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.example.springboot_api.dto.admin.lecturer.OrgUnitInfo;

import lombok.Builder;
import lombok.Data;

/**
 * Response chi tiết Major với danh sách môn học.
 */
@Data
@Builder
public class MajorDetailResponse {
    private UUID id;
    private String code;
    private String name;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OrgUnitInfo orgUnit;
    private Long subjectCount;
    private Long studentCount;
    private List<SubjectInMajorInfo> subjects; // Danh sách môn học trong chương trình đào tạo
}
