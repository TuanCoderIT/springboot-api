package com.example.springboot_api.dto.lecturer.chapter;

import lombok.Data;

@Data
public class UpdateChapterRequest {
    private String title;
    private String description;
}
