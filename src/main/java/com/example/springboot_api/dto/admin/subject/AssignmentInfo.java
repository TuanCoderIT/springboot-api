package com.example.springboot_api.dto.admin.subject;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/**
 * Thông tin đợt giảng dạy của môn học (Nâng cao).
 */
@Data
@Builder
public class AssignmentInfo {
    private UUID id;
    private String termName; // Tên học kỳ
    private String lecturerName; // Tên giảng viên
    private String lecturerEmail; // Email giảng viên (Mới)
    private String status; // Trạng thái đợt dạy (ACTIVE, CLOSED)
    private String approvalStatus; // Trạng thái phê duyệt (Mới: APPROVED, PENDING, ...)
    private String note; // Ghi chú của đợt dạy (Mới)
    private Long classCount; // Số lượng lớp học
    private OffsetDateTime createdAt;
    private List<ClassInfo> classes; // Danh sách chi tiết lớp học (Mới)
}
