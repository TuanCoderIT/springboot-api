package com.example.springboot_api.dto.lecturer.workspace;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho AI content trong lecturer workspace.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceAiResponse {
    
    /**
     * ID của AI content.
     */
    private UUID id;
    
    /**
     * Loại nội dung AI.
     * Ví dụ: "summary", "quiz", "flashcard", "video"
     */
    private String contentType;
    
    /**
     * Tiêu đề của AI content.
     */
    private String title;
    
    /**
     * Mô tả của AI content.
     */
    private String description;
    
    /**
     * Trạng thái xử lý.
     * Ví dụ: "queued", "processing", "completed", "failed"
     */
    private String status;
    
    /**
     * Thông báo trạng thái (user-friendly).
     */
    private String statusMessage;
    
    /**
     * Model AI đã sử dụng.
     */
    private String modelCode;
    
    /**
     * Chương/phần liên quan.
     */
    private String chapter;
    
    /**
     * Thời gian tạo.
     */
    private OffsetDateTime createdAt;
    
    /**
     * Thời gian hoàn thành (nếu có).
     */
    private OffsetDateTime finishedAt;
}