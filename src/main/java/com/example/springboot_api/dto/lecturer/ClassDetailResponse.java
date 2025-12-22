package com.example.springboot_api.dto.lecturer;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response chi tiết cho lớp học phần của giảng viên.
 * Bao gồm thông tin lớp, sinh viên, và thống kê tài liệu từ Assignment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassDetailResponse {
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

    // Thông tin Assignment Parent
    private UUID assignmentId;
    private String assignmentStatus;

    // Resource Counts (từ Assignment's Notebook)
    private Long fileCount;
    private Long quizCount;
    private Long flashcardCount;
    private Long videoCount;
    private Long summaryCount;

    // Notebook Info
    private UUID notebookId;
    private String notebookTitle;
}
