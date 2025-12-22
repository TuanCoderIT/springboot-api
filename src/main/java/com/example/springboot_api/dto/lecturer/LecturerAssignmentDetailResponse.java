package com.example.springboot_api.dto.lecturer;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/**
 * DTO chi tiết phân công giảng dạy - dùng cho trang xem chi tiết.
 * Bao gồm thông tin đầy đủ về môn học, học kỳ, notebook, và danh sách lớp.
 */
@Data
@Builder
public class LecturerAssignmentDetailResponse {
    private UUID id;

    // ========== THÔNG TIN MÔN HỌC ==========
    private UUID subjectId;
    private String subjectCode;
    private String subjectName;
    private Integer subjectCredit;

    // ========== THÔNG TIN HỌC KỲ ==========
    private UUID termId;
    private String termCode;
    private String termName;
    private LocalDate termStartDate;
    private LocalDate termEndDate;
    private Boolean termIsActive;

    // ========== TRẠNG THÁI ==========
    private String status; // ACTIVE, INACTIVE
    private String approvalStatus; // PENDING, APPROVED, REJECTED
    private String termStatus; // ACTIVE, UPCOMING, PAST

    // ========== THỐNG KÊ LỚP HỌC ==========
    private Long classCount;
    private Long studentCount;

    // ========== THỐNG KÊ TÀI LIỆU ==========
    private Long fileCount;
    private Long quizCount;
    private Long flashcardCount;
    private Long summaryCount;
    private Long videoCount;

    // ========== NOTEBOOK ==========
    private UUID notebookId;
    private String notebookTitle;
    private String notebookDescription;
    private String notebookThumbnailUrl;
    private OffsetDateTime notebookCreatedAt;
    private OffsetDateTime notebookUpdatedAt;

    // ========== GHI CHÚ & THỜI GIAN ==========
    private String note;
    private String createdBy; // ADMIN, LECTURER
    private OffsetDateTime createdAt;

    // ========== DANH SÁCH LỚP HỌC PHẦN (TOP 5) ==========
    private List<ClassSummary> recentClasses;

    @Data
    @Builder
    public static class ClassSummary {
        private UUID id;
        private String classCode;
        private String room;
        private Integer dayOfWeek;
        private String periods;
        private Long studentCount;
        private Boolean isActive;
    }
}
