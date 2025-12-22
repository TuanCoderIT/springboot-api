package com.example.springboot_api.dto.admin.subject;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/**
 * Response thông tin Subject cơ bản.
 */
@Data
@Builder
public class SubjectResponse {
    private UUID id;
    private String code;
    private String name;
    private Integer credit;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Long majorCount; // Số ngành học có môn này
    private Long assignmentCount; // Số phân công giảng dạy
    private Long studentCount; // Tổng số sinh viên đã/đang học
}
