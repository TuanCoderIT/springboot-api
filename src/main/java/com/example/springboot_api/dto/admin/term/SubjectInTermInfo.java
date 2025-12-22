package com.example.springboot_api.dto.admin.term;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Thông tin môn học được mở trong học kỳ.
 */
@Data
@Builder
public class SubjectInTermInfo {
    private UUID id;
    private String code;
    private String name;
    private Integer credit;
    private Long teacherCount;  // Số giảng viên đang dạy môn này trong kỳ
}
