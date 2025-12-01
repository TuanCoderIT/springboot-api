package com.example.springboot_api.dto.shared.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypingRequest {
    private boolean isTyping;
}

