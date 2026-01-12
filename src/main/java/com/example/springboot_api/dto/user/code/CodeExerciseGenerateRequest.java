package com.example.springboot_api.dto.user.code;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request tạo bài tập code từ AI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeExerciseGenerateRequest {

    /**
     * Danh sách file IDs để generate bài tập.
     */
    @NotEmpty(message = "Cần ít nhất 1 file")
    private List<UUID> fileIds;

    /**
     * Số bài tập tối đa (1-5).
     */
    @Builder.Default
    private Integer maxExercises = 3;

    /**
     * Ngôn ngữ output (vi/en).
     */
    @Builder.Default
    private String language = "vi";

    /**
     * Yêu cầu thêm cho AI.
     */
    private String additionalRequirements;
}
