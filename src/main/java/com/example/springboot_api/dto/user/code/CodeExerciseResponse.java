package com.example.springboot_api.dto.user.code;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho bài tập code.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeExerciseResponse {

    private UUID id;
    private UUID aiSetId;
    private String title;
    private String description;
    private String difficulty;
    private Integer timeLimit;
    private Long memoryLimit;
    private Integer orderIndex;

    // Language info
    private LanguageInfo language;

    // Files
    private List<CodeFileResponse> starterFiles;
    private List<TestcaseResponse> sampleTestcases;

    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LanguageInfo {
        private UUID id;
        private String name;
        private String version;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeFileResponse {
        private UUID id;
        private String filename;
        private String content;
        private Boolean isMain;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestcaseResponse {
        private UUID id;
        private String input;
        private String expectedOutput;
        private Integer orderIndex;
        private Boolean isHidden;
    }
}
