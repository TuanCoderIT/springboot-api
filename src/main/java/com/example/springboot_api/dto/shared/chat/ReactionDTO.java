package com.example.springboot_api.dto.shared.chat;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReactionDTO {
    private UUID id;
    private String emoji;
    private UserInfoDTO user;
    private OffsetDateTime createdAt;
}

