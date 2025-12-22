package com.example.springboot_api.dto.admin.major;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/**
 * Thông tin môn học trong chương trình đào tạo của ngành.
 */
@Data
@Builder
public class SubjectInMajorInfo {
    private UUID id;
    private String code;
    private String name;
    private Integer credit;
    private Integer termNo; // Học kỳ trong chương trình đào tạo
    private Boolean isRequired; // Môn bắt buộc hay tự chọn
    private String knowledgeBlock; // Khối kiến thức
}
