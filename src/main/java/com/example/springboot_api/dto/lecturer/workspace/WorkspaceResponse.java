package com.example.springboot_api.dto.lecturer.workspace;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho workspace của giảng viên.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceResponse {
    
    /**
     * ID của workspace.
     */
    private UUID id;
    
    /**
     * Tiêu đề workspace (tên lớp học phần).
     */
    private String title;
    
    /**
     * Mô tả workspace.
     */
    private String description;
    
    /**
     * URL thumbnail cho workspace.
     */
    private String thumbnailUrl;
    
    /**
     * Môn học.
     */
    private String subject;
    
    /**
     * Học kỳ.
     */
    private String semester;
    
    /**
     * Năm học.
     */
    private String academicYear;
    
    /**
     * Thời gian tạo.
     */
    private OffsetDateTime createdAt;
    
    /**
     * Thời gian cập nhật cuối.
     */
    private OffsetDateTime updatedAt;
}