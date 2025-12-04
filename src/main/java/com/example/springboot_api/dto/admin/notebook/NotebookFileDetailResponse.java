package com.example.springboot_api.dto.admin.notebook;

import java.util.Map;

import com.example.springboot_api.dto.shared.notebook.NotebookFileResponse;

public record NotebookFileDetailResponse(
        NotebookFileResponse fileInfo,
        Long totalTextChunks,
        Map<String, Integer> generatedContentCounts) {
}
