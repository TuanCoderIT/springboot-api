package com.example.springboot_api.dto.admin.notebook;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public class NotebookAdminResponse {
    private UUID id;
    private String title;
    private String description;
    private String type;
    private String visibility;
    private UUID createdBy;
    private Instant createdAt;
}
