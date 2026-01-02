package com.example.springboot_api.dto.lecturer.notebook;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturerFileDetailResponse {
    
    private UUID id;
    private String originalFilename;
    private String mimeType;
    private Long fileSize;
    private String status;
    private Boolean ocrDone;
    private Boolean embeddingDone;
    private OffsetDateTime createdAt;
    
    // Thông tin notebook
    private UUID notebookId;
    private String notebookTitle;
    
    // Thông tin người upload
    private LecturerNotebookFileResponse.UploaderInfo uploadedBy;
    
    // Nội dung file (để preview)
    private String contentSummary; // Tóm tắt nội dung file
    private Long totalChunks;
    private String firstChunkContent; // Nội dung chunk đầu tiên để preview
    
    // Metadata bổ sung
    private Integer chunkSize;
    private Integer chunkOverlap;
}