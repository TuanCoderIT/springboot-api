package com.example.springboot_api.dto.admin.notebook;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCommunityNotebookRequest {

    @NotBlank
    private String title;

    private String description;

    private String visibility; // public hoặc private
}
