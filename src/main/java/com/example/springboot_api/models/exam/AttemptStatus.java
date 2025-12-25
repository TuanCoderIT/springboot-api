package com.example.springboot_api.models.exam;

public enum AttemptStatus {
    IN_PROGRESS,    // Đang làm bài
    SUBMITTED,      // Đã nộp bài (thủ công)
    AUTO_SUBMITTED, // Tự động nộp bài (hết giờ)
    CANCELLED,      // Đã hủy
    GRADED          // Đã chấm điểm
}