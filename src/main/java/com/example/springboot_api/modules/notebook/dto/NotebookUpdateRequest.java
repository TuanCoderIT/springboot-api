package com.example.springboot_api.modules.notebook.dto;

import com.example.springboot_api.modules.notebook.entity.enums.NotebookType;
import com.example.springboot_api.modules.notebook.entity.enums.NotebookVisibility;

import java.util.Map;

/**
 * Dùng cho PATCH/PUT – tất cả field đều optional.
 */
public class NotebookUpdateRequest {

    private String title;
    private String description;
    private NotebookType type;
    private NotebookVisibility visibility;
    private String thumbnailUrl;
    private Map<String, Object> metadata;

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
}
