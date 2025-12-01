package com.example.springboot_api.dto.shared.chat;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypingNotificationDTO {
    private UUID userId;
    private UserInfoDTO user;
    private boolean isTyping;
}

