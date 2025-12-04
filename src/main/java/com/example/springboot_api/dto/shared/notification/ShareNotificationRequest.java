package com.example.springboot_api.dto.shared.notification;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareNotificationRequest {
    private UUID notificationId;

    // Target options (có thể chọn nhiều)
    private List<UUID> userIds; // Share với các user cụ thể
    private List<UUID> notebookIds; // Share với các nhóm
    private Boolean shareToAdmins; // Share với admin
}

