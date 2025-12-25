package com.example.springboot_api.models.exam;

public enum QuestionType {
    MCQ,         // Multiple Choice Question - Trắc nghiệm nhiều lựa chọn
    ESSAY,       // Essay Question - Câu hỏi tự luận
    CODING,      // Coding Question - Câu hỏi lập trình
    TRUE_FALSE,  // True/False Question - Câu hỏi đúng/sai
    FILL_BLANK,  // Fill in the blank - Điền vào chỗ trống
    MATCHING     // Matching Question - Câu hỏi ghép đôi
}