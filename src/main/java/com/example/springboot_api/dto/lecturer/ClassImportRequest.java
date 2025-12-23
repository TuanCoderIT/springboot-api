package com.example.springboot_api.dto.lecturer;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@Data
public class ClassImportRequest {
    @NotNull(message = "File Excel không được để trống")
    private MultipartFile excelFile;
    
    @NotBlank(message = "Tên lớp học phần không được để trống")
    private String className;
    
    @NotNull(message = "ID môn học không được để trống")
    private UUID subjectId;
    
    @NotNull(message = "ID phân công giảng dạy không được để trống")
    private UUID teachingAssignmentId;
    
    private String room;
    private Integer dayOfWeek;
    private String periods;
    private String note;
}