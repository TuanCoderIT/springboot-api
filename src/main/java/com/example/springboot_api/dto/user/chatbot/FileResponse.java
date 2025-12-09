package com.example.springboot_api.dto.user.chatbot;

import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileResponse {
    private UUID id;
    private String fileType;
    private String fileUrl;
    private String mimeType;
    private String fileName;
    private String ocrText;
    private String caption;
    private Map<String, Object> metadata;
}

