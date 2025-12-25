package com.example.springboot_api.dto.lecturer.workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO cho tạo/cập nhật workspace của giảng viên.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkspaceRequest {
    
    /**
     * Tiêu đề workspace (tên lớp học phần).
     */
    @NotBlank(message = "Tiêu đề workspace không được để trống")
    @Size(max = 200, message = "Tiêu đề không được vượt quá 200 ký tự")
    private String title;
    
    /**
     * Mô tả workspace (optional).
     */
    @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
    private String description;
    
    /**
     * URL thumbnail cho workspace (optional).
     */
    @Size(max = 500, message = "URL thumbnail không được vượt quá 500 ký tự")
    private String thumbnailUrl;
    
    /**
     * Môn học (optional).
     * Ví dụ: "Cấu trúc dữ liệu và giải thuật", "Lập trình Java"
     */
    @Size(max = 100, message = "Tên môn học không được vượt quá 100 ký tự")
    private String subject;
    
    /**
     * Học kỳ (optional).
     * Ví dụ: "HK1", "HK2", "HK Hè"
     */
    @Size(max = 20, message = "Học kỳ không được vượt quá 20 ký tự")
    private String semester;
    
    /**
     * Năm học (optional).
     * Ví dụ: "2024-2025"
     */
    @Size(max = 20, message = "Năm học không được vượt quá 20 ký tự")
    private String academicYear;
}