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
public class LecturerNotebookFileResponse {
    
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
    private String notebookType;
    
    // Thông tin người upload (rút gọn)
    private UploaderInfo uploadedBy;
    
    // Thống kê nội dung (để lecturer biết file có bao nhiều nội dung)
    private Long chunksCount;
    private String contentPreview; // 200 ký tự đầu của nội dung
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploaderInfo {
        private UUID id;
        private String fullName;
        private String email;
    }
}