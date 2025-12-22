package com.example.springboot_api.dto.user.suggestion;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SuggestionResponse {
    private UUID aiSetId;
    private List<SuggestionItem> suggestions;
}
