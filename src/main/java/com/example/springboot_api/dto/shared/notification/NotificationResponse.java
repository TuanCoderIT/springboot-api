package com.example.springboot_api.dto.shared.notification;

import java.time.OffsetDateTime;
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
public class NotificationResponse {
    private UUID id;
    private UUID userId;
    private String userFullName;
    private String userEmail;
    private String type;
    private String title;
    private String content;
    private String url;
    private Map<String, Object> metadata;
    private Boolean isRead;
    private OffsetDateTime readAt;
    private String action;
    private List<String> roleTarget;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

