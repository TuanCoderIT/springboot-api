package com.example.springboot_api.dto.lecturer;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/**
 * DTO trả về thông tin phân công giảng dạy cho chính giảng viên.
 */
@Data
@Builder
public class LecturerAssignmentResponse {
    private UUID id;

    // Thông tin môn học
    private UUID subjectId;
    private String subjectCode;
    private String subjectName;
    private Integer subjectCredit;

    // Thông tin học kỳ
    private UUID termId;
    private String termCode;
    private String termName;
    private LocalDate termStartDate;
    private LocalDate termEndDate;

    // Trạng thái
    private String status;
    private String approvalStatus;

    // Thống kê lớp học & sinh viên
    private Long classCount;
    private Long studentCount;

    // Thống kê tài liệu (từ Notebook)
    private Long fileCount; // Số lượng file
    private Long quizCount; // Số lượng câu hỏi quiz
    private Long flashcardCount; // Số lượng flashcard
    private Long summaryCount; // Số lượng bài giảng tóm tắt
    private Long videoCount; // Số lượng video

    // Ghi chú và notebook
    private String note;
    private UUID notebookId;

    // Thời gian
    private OffsetDateTime createdAt;

    /**
     * Trạng thái học kỳ dựa trên ngày hiện tại:
     * - ACTIVE: Đang trong thời gian hoạt động
     * - UPCOMING: Chưa đến
     * - PAST: Đã kết thúc
     */
    private String termStatus;
}
