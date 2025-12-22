package com.example.springboot_api.dto.admin.lecturer;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/**
 * DTO trả về thông tin giảng viên.
 */
@Data
@Builder
public class LecturerResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String role;
    private Boolean active;
    private String avatarUrl;
    private Instant createdAt;
    private Instant updatedAt;

    // === TeacherProfile fields ===
    private String lecturerCode;
    private String academicDegree;
    private String academicRank;
    private String specialization;
    private String phone;

    // === OrgUnit info ===
    private OrgUnitInfo orgUnit;
}
