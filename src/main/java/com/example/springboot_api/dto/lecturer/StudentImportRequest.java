package com.example.springboot_api.dto.lecturer;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Data
public class StudentImportRequest {
    @NotNull(message = "File Excel không được để trống")
    private MultipartFile excelFile;
    
    @NotNull(message = "ID lớp học phần không được để trống")
    private UUID classId;
}