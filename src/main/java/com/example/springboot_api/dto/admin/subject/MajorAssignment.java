package com.example.springboot_api.dto.admin.subject;

import java.util.UUID;

import lombok.Data;

/**
 * Thông tin gán Subject vào Major (chương trình đào tạo).
 */
@Data
public class MajorAssignment {
    private UUID majorId; // ID ngành học
    private Integer termNo; // Học kỳ trong chương trình đào tạo
    private Boolean isRequired = true; // Môn bắt buộc hay tự chọn
    private String knowledgeBlock; // Khối kiến thức (VD: "Cơ sở ngành", "Chuyên ngành")
}
