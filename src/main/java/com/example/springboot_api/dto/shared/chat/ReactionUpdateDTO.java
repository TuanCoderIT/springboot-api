package com.example.springboot_api.dto.shared.chat;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReactionUpdateDTO {
    private UUID messageId;
    private ReactionDTO reaction;
    private String action; // "added" or "removed"
}

