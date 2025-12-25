package com.example.springboot_api.mappers;

import org.springframework.stereotype.Component;

import com.example.springboot_api.dto.user.regulation.UserRegulationFileResponse;
import com.example.springboot_api.dto.user.regulation.UserRegulationNotebookResponse;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.utils.UrlNormalizer;

import lombok.RequiredArgsConstructor;

/**
 * Mapper cho user regulation DTOs.
 */
@Component
@RequiredArgsConstructor
public class UserRegulationMapper {

    private final UrlNormalizer urlNormalizer;

    public UserRegulationNotebookResponse toNotebookResponse(Notebook notebook, Long totalFiles) {
        if (notebook == null) {
            return null;
        }

        return UserRegulationNotebookResponse.builder()
                .id(notebook.getId())
                .title(notebook.getTitle())
                .description(notebook.getDescription())
                .createdAt(notebook.getCreatedAt())
                .updatedAt(notebook.getUpdatedAt())
                .totalFiles(totalFiles != null ? totalFiles : 0L)
                .build();
    }

    public UserRegulationFileResponse toFileResponse(NotebookFile file) {
        if (file == null) {
            return null;
        }

        // Normalize URL to full URL
        String storageUrl = file.getStorageUrl();
        if (storageUrl != null && !storageUrl.startsWith("http")) {
            storageUrl = urlNormalizer.normalizeToFull(storageUrl);
        }

        return UserRegulationFileResponse.builder()
                .id(file.getId())
                .originalFilename(file.getOriginalFilename())
                .mimeType(file.getMimeType())
                .fileSize(file.getFileSize())
                .storageUrl(storageUrl)
                .pagesCount(file.getPagesCount())
                .uploadedByName(file.getUploadedBy() != null ? file.getUploadedBy().getFullName() : null)
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .build();
    }
}
