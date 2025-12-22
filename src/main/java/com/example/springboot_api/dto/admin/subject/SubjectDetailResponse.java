package com.example.springboot_api.dto.admin.subject;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/**
 * Response chi tiết Subject với danh sách ngành học.
 */
@Data
@Builder
public class SubjectDetailResponse {
    private UUID id;
    private String code;
    private String name;
    private Integer credit;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Long majorCount;
    private Long assignmentCount;
    private Long studentCount; // Tổng số sinh viên đã/đang học
    private List<MajorInSubjectInfo> majors; // Danh sách ngành học có môn này
    private List<AssignmentInfo> assignments; // Danh sách các đợt giảng dạy
}
