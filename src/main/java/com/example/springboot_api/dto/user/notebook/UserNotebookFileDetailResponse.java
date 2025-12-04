package com.example.springboot_api.dto.user.notebook;

import java.util.Map;

import com.example.springboot_api.dto.shared.notebook.NotebookFileResponse;

public record UserNotebookFileDetailResponse(
        NotebookFileResponse fileInfo,
        String fullContent,
        Map<String, Long> generatedContentCounts,
        NotebookFileResponse.UploaderInfo contributor) {
}
