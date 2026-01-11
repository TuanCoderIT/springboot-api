package com.example.springboot_api.dto.user.chatbot;

import java.util.List;
import java.util.UUID;

import lombok.Data;

/**
 * Request cho API gửi tin nhắn chat regulation.
 * POST /user/regulation/chat
 */
@Data
public class RegulationChatRequest {
    private String message; // Nội dung câu hỏi (bắt buộc)
    private UUID conversationId; // ID cuộc trò chuyện (null nếu tạo mới)
    private List<UUID> fileIds; // Danh sách ID tài liệu quy chế đã chọn
    private List<UploadedImageResponse> images; // Danh sách ảnh đã upload
}
