package com.example.springboot_api.dto.admin.assignment;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAssignmentRequest {
    @NotNull(message = "ID học kỳ không được để trống")
    private UUID termId;

    @NotNull(message = "ID môn học không được để trống")
    private UUID subjectId;

    @NotNull(message = "ID giảng viên không được để trống")
    private UUID teacherUserId;

    private String note;
}
