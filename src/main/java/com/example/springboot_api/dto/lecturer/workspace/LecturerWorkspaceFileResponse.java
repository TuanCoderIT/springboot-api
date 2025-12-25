package com.example.springboot_api.dto.lecturer.workspace;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho file trong lecturer workspace.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturerWorkspaceFileResponse {
    
    /**
     * ID của file.
     */
    private UUID id;
    
    /**
     * Tên file gốc.
     */
    private String fileName;
    
    /**
     * URL để truy cập file.
     */
    private String fileUrl;
    
    /**
     * Kích thước file (bytes).
     */
    private Long fileSize;
    
    /**
     * MIME type của file.
     */
    private String mimeType;
    
    /**
     * Trạng thái xử lý file.
     * Ví dụ: "uploaded", "processing", "processed", "failed"
     */
    private String status;
    
    /**
     * Chương/phần của tài liệu.
     */
    private String chapter;
    
    /**
     * Mục đích sử dụng tài liệu.
     */
    private String purpose;
    
    /**
     * Thời gian upload.
     */
    private OffsetDateTime uploadedAt;
}