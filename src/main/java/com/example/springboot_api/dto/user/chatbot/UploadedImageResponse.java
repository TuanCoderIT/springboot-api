package com.example.springboot_api.dto.user.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho upload ảnh chat.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadedImageResponse {
    private String id;        // UUID của ảnh đã upload
    private String fileUrl;   // URL tương đối để hiển thị ảnh
    private String fileName;  // Tên file gốc
    private String mimeType;  // MIME type của file
    private String ocrText;   // (Optional) Text trích xuất từ ảnh
}
