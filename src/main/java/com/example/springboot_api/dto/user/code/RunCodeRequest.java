package com.example.springboot_api.dto.user.code;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request chạy code của user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunCodeRequest {

    // Optional: Nếu không gửi thì lấy theo ngôn ngữ của Exercise
    private UUID languageId;

    @NotEmpty(message = "Cần ít nhất 1 file code")
    private List<CodeFile> files;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeFile {
        private String filename;
        private String content;
        private Boolean isMain;
    }
}
