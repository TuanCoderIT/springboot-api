package com.example.springboot_api.services.shared.ai;

/**
 * Kết quả phân loại mode cho message.
 */
public enum ClassifierMode {
    NO_SEARCH, // Giao tiếp xã giao, không cần tìm tài liệu
    REUSE, // Hỏi tiếp ý trước, dùng lại context cũ
    SEARCH // Hỏi kiến thức mới, cần tìm tài liệu mới
}
