package com.example.springboot_api.dto.admin.notebook;

public record NotebookCreateRequest(
        String title,
        String description,
        String visibility // public | private
) {}

