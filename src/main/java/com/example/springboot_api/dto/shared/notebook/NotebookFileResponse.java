package com.example.springboot_api.dto.shared.notebook;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotebookFileResponse(
                UUID id,
                String originalFilename,
                String mimeType,
                Long fileSize,
                String storageUrl,
                String status,
                Integer pagesCount,
                Boolean ocrDone,
                Boolean embeddingDone,
                Integer chunkSize,
                Integer chunkOverlap,
                Long chunksCount,
                UploaderInfo uploadedBy,
                NotebookInfo notebook,
                OffsetDateTime createdAt,
                OffsetDateTime updatedAt) {

        public record UploaderInfo(
                        UUID id,
                        String fullName,
                        String email,
                        String avatarUrl) {
        }

        public record NotebookInfo(
                        UUID id,
                        String title,
                        String description,
                        String type,
                        String visibility,
                        String thumbnailUrl) {
        }

        public static NotebookFileResponse from(com.example.springboot_api.models.NotebookFile file) {
                UploaderInfo uploader = file.getUploadedBy() != null
                                ? new UploaderInfo(
                                                file.getUploadedBy().getId(),
                                                file.getUploadedBy().getFullName(),
                                                file.getUploadedBy().getEmail(),
                                                file.getUploadedBy().getAvatarUrl())
                                : null;

                NotebookInfo notebook = file.getNotebook() != null
                                ? new NotebookInfo(
                                                file.getNotebook().getId(),
                                                file.getNotebook().getTitle(),
                                                file.getNotebook().getDescription(),
                                                file.getNotebook().getType(),
                                                file.getNotebook().getVisibility(),
                                                file.getNotebook().getThumbnailUrl())
                                : null;

                return new NotebookFileResponse(
                                file.getId(),
                                file.getOriginalFilename(),
                                file.getMimeType(),
                                file.getFileSize(),
                                file.getStorageUrl(),
                                file.getStatus(),
                                file.getPagesCount(),
                                file.getOcrDone(),
                                file.getEmbeddingDone(),
                                file.getChunkSize(),
                                file.getChunkOverlap(),
                                null,
                                uploader,
                                notebook,
                                file.getCreatedAt(),
                                file.getUpdatedAt());
        }

        public static NotebookFileResponse from(com.example.springboot_api.models.NotebookFile file, Long chunksCount) {
                UploaderInfo uploader = file.getUploadedBy() != null
                                ? new UploaderInfo(
                                                file.getUploadedBy().getId(),
                                                file.getUploadedBy().getFullName(),
                                                file.getUploadedBy().getEmail(),
                                                file.getUploadedBy().getAvatarUrl())
                                : null;

                NotebookInfo notebook = file.getNotebook() != null
                                ? new NotebookInfo(
                                                file.getNotebook().getId(),
                                                file.getNotebook().getTitle(),
                                                file.getNotebook().getDescription(),
                                                file.getNotebook().getType(),
                                                file.getNotebook().getVisibility(),
                                                file.getNotebook().getThumbnailUrl())
                                : null;

                return new NotebookFileResponse(
                                file.getId(),
                                file.getOriginalFilename(),
                                file.getMimeType(),
                                file.getFileSize(),
                                file.getStorageUrl(),
                                file.getStatus(),
                                file.getPagesCount(),
                                file.getOcrDone(),
                                file.getEmbeddingDone(),
                                file.getChunkSize(),
                                file.getChunkOverlap(),
                                chunksCount,
                                uploader,
                                notebook,
                                file.getCreatedAt(),
                                file.getUpdatedAt());
        }

        public static NotebookFileResponse from(com.example.springboot_api.models.NotebookFile file, Long chunksCount,
                        com.example.springboot_api.models.User uploader) {
                UploaderInfo uploaderInfo = uploader != null
                                ? new UploaderInfo(
                                                uploader.getId(),
                                                uploader.getFullName(),
                                                uploader.getEmail(),
                                                uploader.getAvatarUrl())
                                : null;

                NotebookInfo notebook = file.getNotebook() != null
                                ? new NotebookInfo(
                                                file.getNotebook().getId(),
                                                file.getNotebook().getTitle(),
                                                file.getNotebook().getDescription(),
                                                file.getNotebook().getType(),
                                                file.getNotebook().getVisibility(),
                                                file.getNotebook().getThumbnailUrl())
                                : null;

                return new NotebookFileResponse(
                                file.getId(),
                                file.getOriginalFilename(),
                                file.getMimeType(),
                                file.getFileSize(),
                                file.getStorageUrl(),
                                file.getStatus(),
                                file.getPagesCount(),
                                file.getOcrDone(),
                                file.getEmbeddingDone(),
                                file.getChunkSize(),
                                file.getChunkOverlap(),
                                chunksCount,
                                uploaderInfo,
                                notebook,
                                file.getCreatedAt(),
                                file.getUpdatedAt());
        }

        public static NotebookFileResponse from(com.example.springboot_api.models.NotebookFile file, Long chunksCount,
                        com.example.springboot_api.models.User uploader,
                        com.example.springboot_api.models.Notebook notebook) {
                UploaderInfo uploaderInfo = uploader != null
                                ? new UploaderInfo(
                                                uploader.getId(),
                                                uploader.getFullName(),
                                                uploader.getEmail(),
                                                uploader.getAvatarUrl())
                                : null;

                NotebookInfo notebookInfo = notebook != null
                                ? new NotebookInfo(
                                                notebook.getId(),
                                                notebook.getTitle(),
                                                notebook.getDescription(),
                                                notebook.getType(),
                                                notebook.getVisibility(),
                                                notebook.getThumbnailUrl())
                                : null;

                return new NotebookFileResponse(
                                file.getId(),
                                file.getOriginalFilename(),
                                file.getMimeType(),
                                file.getFileSize(),
                                file.getStorageUrl(),
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
