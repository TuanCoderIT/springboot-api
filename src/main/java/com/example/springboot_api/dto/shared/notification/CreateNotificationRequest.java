package com.example.springboot_api.dto.shared.notification;

import java.util.List;
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
public class CreateNotificationRequest {
    private String type;
    private String title;
    private String content;
    private String url;
    private Map<String, Object> metadata;
    private String action;
    private List<String> roleTarget;

    // Target options (chỉ một trong các field này được set)
    private UUID userId; // Gửi cho user cụ thể
    private UUID notebookId; // Gửi cho tất cả members của notebook
    private Boolean sendToAdmins; // Gửi cho tất cả admin
}
