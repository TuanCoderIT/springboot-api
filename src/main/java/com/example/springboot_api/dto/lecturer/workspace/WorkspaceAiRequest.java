package com.example.springboot_api.dto.lecturer.workspace;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO cho tạo AI content trong lecturer workspace.
 * Tái sử dụng logic AI generation từ notebook user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceAiRequest {
    
    /**
     * Danh sách ID của files để tạo AI content.
     */
    @NotEmpty(message = "Phải chọn ít nhất 1 file để tạo nội dung AI")
    private List<UUID> fileIds;
    
    /**
     * Tiêu đề cho AI content (optional).
     */
    @Size(max = 200, message = "Tiêu đề không được vượt quá 200 ký tự")
    private String title;
    
    /**
     * Mô tả cho AI content (optional).
     */
    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;
    
    /**
     * Chương/phần liên quan (optional).
     */
    @Size(max = 100, message = "Tên chương không được vượt quá 100 ký tự")
    private String chapter;
    
    /**
     * Model AI sử dụng (optional, default: "gemini").
     * Ví dụ: "gemini", "groq"
     */
    @Size(max = 50, message = "Mã model không được vượt quá 50 ký tự")
    private String modelCode;
    
    /**
     * Prompt tùy chỉnh (optional).
     */
    @Size(max = 2000, message = "Prompt tùy chỉnh không được vượt quá 2000 ký tự")
    private String customPrompt;
    
    /**
     * Số lượng câu hỏi quiz (chỉ dùng cho quiz generation).
     */
    @Min(value = 1, message = "Số lượng câu hỏi phải ít nhất 1")
    @Max(value = 50, message = "Số lượng câu hỏi không được vượt quá 50")
    private Integer quizCount;
    
    /**
     * Số lượng flashcard (chỉ dùng cho flashcard generation).
     */
    @Min(value = 1, message = "Số lượng flashcard phải ít nhất 1")
    @Max(value = 100, message = "Số lượng flashcard không được vượt quá 100")
    private Integer flashcardCount;
}