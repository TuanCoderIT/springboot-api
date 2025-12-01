package com.example.springboot_api.dto.shared.chat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private UUID id;
    private UserInfoDTO user;
    private String content;
    private UUID replyToMessageId;
    private List<ReactionDTO> reactions;
    private OffsetDateTime createdAt;
}

