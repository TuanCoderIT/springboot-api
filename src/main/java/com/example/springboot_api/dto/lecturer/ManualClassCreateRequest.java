package com.example.springboot_api.dto.lecturer;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Data
public class ManualClassCreateRequest {
    
    @NotBlank(message = "Tên lớp không được để trống")
    private String className;
    
    @NotNull(message = "ID môn học không được để trống")
    private UUID subjectId;
    
    private String room;
    private Integer dayOfWeek;
    private String periods;
    private String note;
}