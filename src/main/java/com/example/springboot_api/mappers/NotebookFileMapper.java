package com.example.springboot_api.mappers;

import org.springframework.stereotype.Component;

import com.example.springboot_api.dto.shared.notebook.NotebookFileResponse;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.utils.UrlNormalizer;

import lombok.RequiredArgsConstructor;

/**
 * Mapper chuyển đổi NotebookFile entities sang DTOs.
 */
@Component
@RequiredArgsConstructor
public class NotebookFileMapper {

    private final UrlNormalizer urlNormalizer;

    /**
     * Convert NotebookFile entity sang NotebookFileResponse DTO.
     */
    public NotebookFileResponse toNotebookFileResponse(NotebookFile file) {
        return toNotebookFileResponse(file, null);
    }

    /**
     * Convert NotebookFile entity sang NotebookFileResponse DTO với chunksCount.
     */
    public NotebookFileResponse toNotebookFileResponse(NotebookFile file, Long chunksCount) {
        if (file == null) {
            return null;
        }

        String normalizedStorageUrl = urlNormalizer.normalizeToFull(file.getStorageUrl());

        NotebookFileResponse.UploaderInfo uploaderInfo = null;
        if (file.getUploadedBy() != null) {
            String normalizedAvatarUrl = urlNormalizer.normalizeToFull(file.getUploadedBy().getAvatarUrl());
            uploaderInfo = new NotebookFileResponse.UploaderInfo(
                    file.getUploadedBy().getId(),
                    file.getUploadedBy().getFullName(),
                    file.getUploadedBy().getEmail(),
                    normalizedAvatarUrl);
        }

        NotebookFileResponse.NotebookInfo notebookInfo = null;
        if (file.getNotebook() != null) {
            String normalizedThumbnailUrl = urlNormalizer.normalizeToFull(file.getNotebook().getThumbnailUrl());
            notebookInfo = new NotebookFileResponse.NotebookInfo(
                    file.getNotebook().getId(),
                    file.getNotebook().getTitle(),
                    file.getNotebook().getDescription(),
                    file.getNotebook().getType(),
                    file.getNotebook().getVisibility(),
                    normalizedThumbnailUrl);
        }

        return new NotebookFileResponse(
                file.getId(),
                file.getOriginalFilename(),
                file.getMimeType(),
                file.getFileSize(),
                normalizedStorageUrl,
                file.getStatus(),
                file.getPagesCount(),
                file.getOcrDone(),
                file.getEmbeddingDone(),
                file.getChunkSize(),
                file.getChunkOverlap(),
                chunksCount,
                uploaderInfo,
                notebookInfo,
                file.getCreatedAt(),
                file.getUpdatedAt());
    }
}
