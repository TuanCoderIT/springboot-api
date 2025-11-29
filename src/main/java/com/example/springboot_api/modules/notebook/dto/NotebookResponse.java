package com.example.springboot_api.modules.notebook.dto;

import com.example.springboot_api.modules.notebook.entity.enums.NotebookType;
import com.example.springboot_api.modules.notebook.entity.enums.NotebookVisibility;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class NotebookResponse {

    private UUID id;
    private String title;
    private String description;
    private NotebookType type;
    private NotebookVisibility visibility;
    private String thumbnailUrl;
    private Map<String, Object> metadata;

    private UUID createdById;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Có thể bổ sung stats sau: membersCount, filesCount,...

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public NotebookType getType() {
        return type;
    }

    public void setType(NotebookType type) {
        this.type = type;
    }

    public NotebookVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(NotebookVisibility visibility) {
        this.visibility = visibility;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public UUID getCreatedById() {
        return createdById;
    }

    public void setCreatedById(UUID createdById) {
        this.createdById = createdById;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
