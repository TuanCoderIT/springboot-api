package com.example.springboot_api.dto.user.suggestion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho từng câu hỏi gợi mở.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionItem {
    private String question;
    private String hint;
    private String category;
}
