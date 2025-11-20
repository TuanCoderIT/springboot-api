package com.example.springboot_api.notebook.dto;

import lombok.Data;

@Data
public class NotebookRequest {
    private String title;
    private String description;
}