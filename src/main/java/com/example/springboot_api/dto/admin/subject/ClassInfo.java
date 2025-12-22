package com.example.springboot_api.dto.admin.subject;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/**
 * Thông tin lớp học thuộc một đợt giảng dạy.
 */
@Data
@Builder
public class ClassInfo {
    private UUID id;
    private String code;
    private String name;
    private Integer maxStudents;
    private String note;
    private Boolean isActive;
}
