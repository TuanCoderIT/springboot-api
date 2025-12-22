package com.example.springboot_api.dto.lecturer;

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
    private String subjectCode;
    private String subjectName;
    private String termName;
    private String status;
    private String approvalStatus;
    private Long classCount;
    private Long studentCount;
    private OffsetDateTime createdAt;

    /**
     * Trạng thái học kỳ dựa trên ngày hiện tại:
     * - ACTIVE: Đang trong thời gian hoạt động
     * - UPCOMING: Chưa đến
     * - PAST: Đã kết thúc
     */
    private String termStatus;
}
