package com.example.springboot_api.models.exam;

public enum ExamStatus {
    DRAFT, // Bản nháp - đang soạn thảo
    GENERATING, // Đang tạo câu hỏi (AI)
    PUBLISHED, // Đã xuất bản - sẵn sàng cho sinh viên
    ACTIVE, // Đang diễn ra
    COMPLETED, // Đã hoàn thành
    CANCELLED // Đã hủy
}