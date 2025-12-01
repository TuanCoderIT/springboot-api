package com.example.springboot_api.dto.shared.chat;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReactRequest {
    @NotBlank(message = "Emoji is required")
    @Size(max = 32, message = "Emoji must not exceed 32 characters")
    private String emoji;
    
    @NotBlank(message = "Message ID is required")
    private UUID messageId;
}

