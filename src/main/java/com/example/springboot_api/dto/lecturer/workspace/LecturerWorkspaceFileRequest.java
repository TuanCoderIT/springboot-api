package com.example.springboot_api.dto.lecturer.workspace;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO cho upload file trong lecturer workspace.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturerWorkspaceFileRequest {
    
    /**
     * Chương/phần của tài liệu (optional).
     * Ví dụ: "Chương 1", "Bài 2", "Midterm"
     */
    @Size(max = 100, message = "Tên chương không được vượt quá 100 ký tự")
    private String chapter;
    
    /**
     * Mục đích sử dụng tài liệu (optional).
     * Ví dụ: "teaching_material", "exam_preparation", "assignment"
     */
    @Size(max = 50, message = "Mục đích sử dụng không được vượt quá 50 ký tự")
    private String purpose;
    
    /**
     * Ghi chú thêm về tài liệu (optional).
     */
    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String notes;
}