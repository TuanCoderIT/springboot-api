package com.example.springboot_api.notebook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class NotebookResponse {
    private UUID id;
    private String title;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
