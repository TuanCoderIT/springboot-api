package com.example.springboot_api.dto.shared.ai;

import java.util.List;

public record WebSearchResult(
                String query,
                long searchTimeMs,
                List<WebSearchItem> items) {
}
