package com.example.springboot_api.dto.lecturer;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO cho giảng viên gửi yêu cầu dạy môn học
 */
@Data
public class RequestTeachingRequest {

    @NotNull(message = "Vui lòng chọn học kỳ")
    private UUID termId;

    @NotNull(message = "Vui lòng chọn môn học")
    private UUID subjectId;

    private String note;
}
