package com.example.springboot_api.dto.lecturer;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho thông tin lớp học phần của giảng viên.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassResponse {
    private UUID id;
    private String classCode;
    private String subjectCode;
    private String subjectName;
    private String termName; // Tên học kỳ
    private String room;
    private Integer dayOfWeek;
    private String periods;
    private LocalDate startDate;
    private LocalDate endDate;
    private String note;
    private Boolean isActive;
    private Long studentCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
