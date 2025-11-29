package com.example.springboot_api.modules.notebook.dto;

import com.example.springboot_api.modules.notebook.entity.enums.NotebookType;
import com.example.springboot_api.modules.notebook.entity.enums.NotebookVisibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class NotebookCreateRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private NotebookType type; // community / private_group / personal

    @NotNull
    private NotebookVisibility visibility; // public / private

    private String thumbnailUrl;

    // map sang jsonb metadata
    private Map<String, Object> metadata;

    // getter / setter

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
