package com.example.springboot_api.mappers;

import org.springframework.stereotype.Component;

import com.example.springboot_api.dto.admin.regulation.RegulationFileResponse;
import com.example.springboot_api.dto.admin.regulation.RegulationNotebookResponse;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookFile;

/**
 * Mapper cho Regulation entities.
 */
@Component
public class RegulationMapper {

    /**
     * Convert Notebook entity sang RegulationNotebookResponse DTO với statistics.
     */
    public RegulationNotebookResponse toNotebookResponse(
            Notebook notebook,
            long totalFiles,
            long pendingFiles,
            long approvedFiles,
            long processingFiles,
            long failedFiles,
            long ocrDoneFiles,
            long embeddingDoneFiles) {
        if (notebook == null)
            return null;

        return RegulationNotebookResponse.builder()
                .id(notebook.getId())
                .title(notebook.getTitle())
                .description(notebook.getDescription())
                .type(notebook.getType())
                .visibility(notebook.getVisibility())
                .createdById(notebook.getCreatedBy() != null ? notebook.getCreatedBy().getId() : null)
                .createdByName(notebook.getCreatedBy() != null ? notebook.getCreatedBy().getFullName() : null)
                .createdAt(notebook.getCreatedAt() != null ? notebook.getCreatedAt().toInstant() : null)
                .updatedAt(notebook.getUpdatedAt() != null ? notebook.getUpdatedAt().toInstant() : null)
                .totalFiles(totalFiles)
                .pendingFiles(pendingFiles)
                .approvedFiles(approvedFiles)
                .processingFiles(processingFiles)
                .failedFiles(failedFiles)
                .ocrDoneFiles(ocrDoneFiles)
                .embeddingDoneFiles(embeddingDoneFiles)
                .build();
    }

    /**
     * Convert NotebookFile entity sang RegulationFileResponse DTO.
     */
    public RegulationFileResponse toFileResponse(NotebookFile file) {
        if (file == null)
            return null;

        return RegulationFileResponse.builder()
                .id(file.getId())
                .originalFilename(file.getOriginalFilename())
                .mimeType(file.getMimeType())
                .fileSize(file.getFileSize())
                .status(file.getStatus())
                .ocrDone(file.getOcrDone())
                .embeddingDone(file.getEmbeddingDone())
                .chunkSize(file.getChunkSize())
                .chunkOverlap(file.getChunkOverlap())
                .createdAt(file.getCreatedAt() != null ? file.getCreatedAt().toInstant() : null)
                .updatedAt(file.getUpdatedAt() != null ? file.getUpdatedAt().toInstant() : null)
                .build();
    }
}
