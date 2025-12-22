package com.example.springboot_api.dto.admin.major;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.springboot_api.dto.admin.lecturer.OrgUnitInfo;

import lombok.Builder;
import lombok.Data;

/**
 * Response thông tin Major cơ bản.
 */
@Data
@Builder
public class MajorResponse {
    private UUID id;
    private String code;
    private String name;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OrgUnitInfo orgUnit; // Đơn vị tổ chức
    private Long subjectCount; // Số môn học trong chương trình đào tạo
    private Long studentCount; // Số sinh viên đang học ngành này
}
